package com.lear.MGCMS.services.scheduling;

import com.lear.cms.domain.TimingModel;
import com.lear.cms.repositories.TimingModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Single source of truth for estimated cutting time per serie.
 * <p>
 * Used by {@code PlanDeChargeService} (load % per machine) and
 * {@code OrdonnancementService} (queue ETA per serie). Before this bean
 * existed, both services carried their own copy of the logic and had
 * drifted: the ordonnancement copy was missing the Gerber multiplier.
 *
 * <h2>Priority waterfall</h2>
 * <ol>
 *   <li>{@code TimingModel.Validated_Cutting_time_Timing_Model} &mdash; if &gt; 0</li>
 *   <li>{@code TimingModel.Real_Cutting_time_Timing_Model}      &mdash; if &gt; 0</li>
 *   <li>{@code CuttingRequestSerie.tempsDeCoupe} (the estimate)</li>
 *   <li>{@code 0.0} when nothing is available</li>
 * </ol>
 *
 * <h2>Post-adjustments</h2>
 * <ul>
 *   <li><b>LASER-DXF &times; nbrCouche</b>: applied <i>only</i> when the value came
 *       from {@code tempsDeCoupe}. {@code TimingModel} readings for LASER-DXF
 *       placements already bake in the layer count, so multiplying them again
 *       would double-count.</li>
 *   <li><b>Gerber &times; 2</b>: applied <i>universally</i>, whatever the source.
 *       The plant's Gerber estimates are systematically half of actual.</li>
 * </ul>
 *
 * <p>The underlying TimingModel read is batched (SQL Server's 2100-parameter
 * IN-clause limit is respected). Callers that already have a placement map
 * can call {@link #resolve(String, Double, Integer, String, Map)} directly;
 * callers starting from a list of series can call
 * {@link #resolveMinutesBatch(List)} and get it in one shot.</p>
 *
 * <p>This bean is pure: it performs one read-only DB round-trip per batch and
 * holds no mutable state.</p>
 */
@Service
public class CuttingTimeCalculator {

    /** SQL Server caps IN-clause parameters at 2100; keep a safe margin. */
    private static final int BATCH_SIZE = 2000;

    private static final String MACHINE_TYPE_LASER_DXF = "LASER-DXF";
    private static final String MACHINE_TYPE_GERBER    = "Gerber";

    @Autowired
    private TimingModelRepository timingModelRepository;

    /** Which field of {@link TimingModel} the returned minutes came from. */
    public enum Source {
        /** {@code Validated_Cutting_time_Timing_Model} was present and positive. */
        VALIDATED,
        /** {@code Validated} was null/zero but {@code Real_Cutting_time} was present. */
        REAL,
        /** Both timing-model fields were absent; the serie's own {@code tempsDeCoupe} estimate was used. */
        TEMPS_DE_COUPE,
        /** Nothing was available &mdash; the resolver returned 0.0. */
        NONE
    }

    /** Minutes + source pair returned from {@link #resolve}. */
    public static final class Resolved {
        public final double minutes;
        public final Source source;

        public Resolved(double minutes, Source source) {
            this.minutes = minutes;
            this.source = source;
        }
    }

    /** Cached (validated, real) pair per placement; both may be null. */
    public static final class TimingRow {
        public final Double validated;
        public final Double real;

        public TimingRow(Double validated, Double real) {
            this.validated = validated;
            this.real = real;
        }
    }

    /**
     * Input payload for batch resolution. Callers may reuse their own
     * DTOs; they just fill this shape.
     *
     * @param <K> key type so the batch result can be keyed back by whatever
     *            id the caller is using (serie string, DB long, etc.)
     */
    public static final class SerieInput<K> {
        public final K key;
        public final String placement;
        public final Double tempsDeCoupe;
        public final Integer nbrCouche;
        public final String machineType;

        public SerieInput(K key, String placement, Double tempsDeCoupe,
                          Integer nbrCouche, String machineType) {
            this.key = key;
            this.placement = placement;
            this.tempsDeCoupe = tempsDeCoupe;
            this.nbrCouche = nbrCouche;
            this.machineType = machineType;
        }
    }

    // ----------------------------------------------------------------- core

    /**
     * Pure function: given a pre-loaded placement &rarr; TimingRow map, resolve
     * the effective minutes for one serie.
     *
     * <p>This is the method to call when you've already loaded TimingModel
     * rows for a bunch of placements and want to avoid another DB hit.</p>
     */
    public Resolved resolve(String placement,
                            Double tempsDeCoupe,
                            Integer nbrCouche,
                            String machineType,
                            Map<String, TimingRow> timingByPlacement) {
        return resolve(placement, tempsDeCoupe, nbrCouche, machineType, timingByPlacement, null);
    }

    /**
     * Efficiency-aware variant. When {@code efficiencePct} is non-null and
     * positive the resolved minutes are scaled by {@code 1 / (efficiencePct/100)}
     * — the machine's expected efficiency — and the legacy Gerber &times; 2 is
     * <i>not</i> applied (efficiency generalises it: Gerber at 50% yields the
     * same &times; 2). When {@code efficiencePct} is null the legacy Gerber
     * &times; 2 is kept, so callers passing nothing (e.g. {@code
     * OrdonnancementService}) are completely unaffected.
     */
    public Resolved resolve(String placement,
                            Double tempsDeCoupe,
                            Integer nbrCouche,
                            String machineType,
                            Map<String, TimingRow> timingByPlacement,
                            Double efficiencePct) {

        Double base = null;
        Source source = Source.NONE;

        // 1. Priority: Validated > Real
        if (placement != null && timingByPlacement != null) {
            TimingRow row = timingByPlacement.get(placement);
            if (row != null) {
                if (row.validated != null && row.validated > 0) {
                    base = row.validated;
                    source = Source.VALIDATED;
                } else if (row.real != null && row.real > 0) {
                    base = row.real;
                    source = Source.REAL;
                }
            }
        }

        // 2. Fallback to estimate; only here does LASER-DXF layer multiplier apply.
        if (base == null && tempsDeCoupe != null && tempsDeCoupe > 0) {
            base = tempsDeCoupe;
            source = Source.TEMPS_DE_COUPE;
            if (MACHINE_TYPE_LASER_DXF.equals(machineType)
                    && nbrCouche != null && nbrCouche > 1) {
                base = base * nbrCouche;
            }
        }

        if (base == null) {
            return new Resolved(0.0, Source.NONE);
        }

        // 3. Efficiency adjustment — applies to every source uniformly, like the
        //    legacy Gerber factor it replaces.
        if (efficiencePct != null && efficiencePct > 0) {
            base = base / (efficiencePct / 100.0);
        } else if (MACHINE_TYPE_GERBER.equals(machineType)) {
            base = base * 2;
        }

        return new Resolved(base, source);
    }

    /** Convenience: drop the source label when the caller only wants minutes. */
    public double resolveMinutes(String placement,
                                 Double tempsDeCoupe,
                                 Integer nbrCouche,
                                 String machineType,
                                 Map<String, TimingRow> timingByPlacement) {
        return resolve(placement, tempsDeCoupe, nbrCouche, machineType, timingByPlacement).minutes;
    }

    /** Efficiency-aware convenience overload; see {@link #resolve(String, Double, Integer, String, Map, Double)}. */
    public double resolveMinutes(String placement,
                                 Double tempsDeCoupe,
                                 Integer nbrCouche,
                                 String machineType,
                                 Map<String, TimingRow> timingByPlacement,
                                 Double efficiencePct) {
        return resolve(placement, tempsDeCoupe, nbrCouche, machineType, timingByPlacement, efficiencePct).minutes;
    }

    // ----------------------------------------------------------------- batch

    /**
     * One-shot: load TimingModel for every placement in the input, then
     * resolve each serie. Returns {@code Map<key, minutes>}.
     *
     * <p>Keys come from the caller's {@link SerieInput#key}; if two inputs
     * share a key the later one wins (same behaviour as
     * {@code Collectors.toMap} with a merge function).</p>
     */
    public <K> Map<K, Double> resolveMinutesBatch(List<SerieInput<K>> series) {
        if (series == null || series.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, TimingRow> timingMap = loadTimingMap(
                series.stream()
                        .map(s -> s.placement)
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList()));

        Map<K, Double> out = new HashMap<>(series.size() * 2);
        for (SerieInput<K> s : series) {
            double minutes = resolveMinutes(
                    s.placement, s.tempsDeCoupe, s.nbrCouche, s.machineType, timingMap);
            out.put(s.key, minutes);
        }
        return out;
    }

    /**
     * Batch-load the (validated, real) pair for every placement supplied.
     * Exposed so callers that already batch reads themselves can build the
     * map once and reuse it across many {@link #resolve} calls.
     *
     * <p>Duplicates in the input are collapsed. Null entries are ignored.
     * The returned map never contains {@code null} keys.</p>
     */
    public Map<String, TimingRow> loadTimingMap(Collection<String> placements) {
        if (placements == null || placements.isEmpty()) {
            return Collections.emptyMap();
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String p : placements) {
            if (p != null) unique.add(p);
        }
        if (unique.isEmpty()) return Collections.emptyMap();

        Map<String, TimingRow> result = new HashMap<>(unique.size() * 2);
        List<String> list = new ArrayList<>(unique);

        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            List<String> batch = list.subList(i, Math.min(i + BATCH_SIZE, list.size()));
            List<TimingModel> rows = timingModelRepository.findByPlacementTimingModelIn(batch);
            for (TimingModel tm : rows) {
                String key = tm.getPlacementTimingModel();
                if (key == null) continue;
                result.put(key, new TimingRow(
                        tm.getValidatedCuttingtimeTimingModel(),
                        tm.getRealCuttingtimeTimingModel()));
            }
        }
        return result;
    }
}
