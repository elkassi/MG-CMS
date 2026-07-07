package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.CapaciteInstallee;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.services.CapaciteInstalleeService;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;

/**
 * Computes the (machineType, zone) load matrix that powers the dispatcher
 * heatmap and the equilibre summary (Contract C7 — MASTER_SCHEDULING_VISION_v3.md
 * §4.1).
 *
 * <p>Algorithm — five lookups, no N+1:</p>
 * <ol>
 *   <li>Load every {@code CuttingRequest} for (date, shift) — existing
 *       {@code findAll(date, shift)} repo method.</li>
 *   <li>For each request, expand to series. Resolve each serie's zone via
 *       {@link SerieZoneResolver} (same routing the dispatcher uses, so
 *       the heatmap mirrors what publish would do).</li>
 *   <li>Resolve cutting times in one batch via
 *       {@link CuttingTimeCalculator#resolveMinutesBatch} (one CMS-DB hit).</li>
 *   <li>For each (zone, machineType) pair, compute capacity from
 *       {@link ActiveMachineResolver} active machines + the configured
 *       shift duration + {@link CapaciteInstalleeService} efficiency.</li>
 *   <li>Build cell + row + equilibre DTOs, classify status against
 *       {@link ZoneLoadProperties} thresholds, return.</li>
 * </ol>
 *
 * <p>Read-only — never mutates state. Safe to call frequently.</p>
 */
@Service
public class ZoneLoadService {

    private static final double DEFAULT_SHIFT_MINUTES_FALLBACK = 460.0;
    private static final double DEFAULT_EFFICIENCE_TARGET_FALLBACK = 90.0;
    private static final Logger log = LoggerFactory.getLogger(ZoneLoadService.class);

    /** Per-machine-type → groupe mapping (matches PdC and CapaciteInstallee.groupe). */
    private static String groupeOf(String machineType) {
        if (machineType == null) return "Coupe";
        String t = machineType.trim();
        if (t.equalsIgnoreCase("LASER-DXF") || t.equalsIgnoreCase("LASER-LSR")
                || t.equalsIgnoreCase("LASER")) {
            return "Laser";
        }
        return "Coupe";
    }

    @Autowired private CuttingRequestRepository cuttingRequestRepository;
    @Autowired private com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository serieDataRepository;
    @Autowired private SerieZoneResolver serieZoneResolver;
    @Autowired private CuttingTimeCalculator cuttingTimeCalculator;
    @Autowired private ActiveMachineResolver activeMachineResolver;
    @Autowired private CapaciteInstalleeService capaciteInstalleeService;
    @Autowired private ProductionTableRepository productionTableRepository;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private ShiftProperties shiftProperties;
    @Autowired private ZoneLoadProperties props;
    @Autowired private DispatcherProperties dispatcherProperties;

    // Result cache keyed by date|shift — zone load changes slowly
    private final Map<String, ZoneLoadDto> cache = new HashMap<>();
    private final Map<String, Long> cacheTimestamps = new HashMap<>();
    private static final long CACHE_TTL_MS = 30_000;

    /**
     * Build the full matrix DTO. Empty result is a valid response — the
     * UI renders a "no data" message rather than an error.
     */
    @Transactional(readOnly = true)
    public ZoneLoadDto computeMatrix(LocalDate date, int shift) {
        long startMs = System.currentTimeMillis();
        String key = date + "|" + shift;
        ZoneLoadDto cached = cache.get(key);
        Long ts = cacheTimestamps.get(key);
        if (cached != null && ts != null && System.currentTimeMillis() - ts < CACHE_TTL_MS) {
            log.debug("ZoneLoadService cache hit for {} — {} ms", key, System.currentTimeMillis() - startMs);
            return cached;
        }
        long stepStart = startMs;

        // =====================================================================
        // Phase 0 — Pre-load configuration data in ONE batch each
        // =====================================================================

        // Load all active zones first so we can build lookup maps and avoid
        // repeated findByObjId DB hits for frozen requests.
        List<Zone> allActiveZones = zoneRepository.findAllActive();
        allActiveZones.sort((a, b) -> {
            boolean aStrict = a.getCategory() != Zone.Category.SHARED;
            boolean bStrict = b.getCategory() != Zone.Category.SHARED;
            if (aStrict != bStrict) return aStrict ? -1 : 1;
            return a.getNom().compareToIgnoreCase(b.getNom());
        });

        Map<String, Zone> zoneByNom = new HashMap<>(allActiveZones.size() * 2);
        Set<String> activeZoneNoms = new HashSet<>(allActiveZones.size() * 2);
        for (Zone z : allActiveZones) {
            zoneByNom.put(z.getNom(), z);
            activeZoneNoms.add(z.getNom());
        }
        log.debug("ZoneLoadService phase 0a: loaded {} zones in {} ms",
                allActiveZones.size(), System.currentTimeMillis() - stepStart);

        // Batch load ALL machines in ONE query instead of N zone-scoped queries.
        // findAllMachinesLight returns: [id, nom, zoneNom, machineTypeName, tableLength]
        stepStart = System.currentTimeMillis();
        List<Object[]> allMachineRows = productionTableRepository.findAllMachinesLight();
        Map<String, Set<String>> typesByZone = new HashMap<>();
        Map<String, Map<String, Set<String>>> machinesByZoneByType = new HashMap<>();
        Set<String> machineTypesSeen = new LinkedHashSet<>();

        for (Object[] row : allMachineRows) {
            String zoneNom = (String) row[2];
            if (!activeZoneNoms.contains(zoneNom)) {
                continue;
            }
            String machineNom = (String) row[1];
            String typeName = (String) row[3];
            if (typeName == null) {
                continue;
            }
            machineTypesSeen.add(typeName);
            typesByZone.computeIfAbsent(zoneNom, k -> new LinkedHashSet<>()).add(typeName);
            machinesByZoneByType
                    .computeIfAbsent(zoneNom, k -> new HashMap<>())
                    .computeIfAbsent(typeName, k -> new LinkedHashSet<>())
                    .add(machineNom);
        }
        log.debug("ZoneLoadService phase 0b: batched {} machine rows into {} zone/type groups in {} ms",
                allMachineRows.size(), machinesByZoneByType.size(), System.currentTimeMillis() - stepStart);

        // =====================================================================
        // Phase 1 — Load request headers and series
        // =====================================================================
        stepStart = System.currentTimeMillis();
        // Light path: scalar request fields + light series projection.
        // Never load a full CuttingRequest @Entity graph (avoids eager
        // collection fetch of series / part-numbers / boxes — see audit #3).
        // Workbench loads active sequences due in this shift or older only;
        // upcoming due buckets are left out of the current load matrix.
        List<Object[]> requestRows = cuttingRequestRepository.findActiveDueOnOrBeforeLight(
                date, String.valueOf(shift));
        Map<String, RequestLight> requestBySeq = new LinkedHashMap<>(requestRows.size() * 2);
        List<String> activeSequences = new ArrayList<>(requestRows.size());
        for (Object[] r : requestRows) {
            String seq = (String) r[0];
            activeSequences.add(seq);
            requestBySeq.put(seq,
                    new RequestLight(seq, (String) r[1], (String) r[2],
                            r[3] != null && Boolean.TRUE.equals(r[3]), (String) r[4]));
        }

        List<Object[]> serieRows;
        if (activeSequences.isEmpty()) {
            serieRows = Collections.emptyList();
        } else {
            serieRows = serieDataRepository.findSeriesBySequencesLightProjection(activeSequences);
        }
        log.debug("ZoneLoadService phase 1: loaded {} requests, {} series in {} ms",
                requestRows.size(), serieRows.size(), System.currentTimeMillis() - stepStart);

        // =====================================================================
        // Phase 2 — Resolve zone + build cutting-time batch inputs
        // =====================================================================
        stepStart = System.currentTimeMillis();
        // Step 1: build (zone, type) → planned minutes + sequence-set + serie-set.
        // Exclude REJECTED requests (chef said no) and pure-SHARED requests
        // until we know the per-serie resolution; the resolver decides each
        // serie's zone independently of the request's dispatched_zone, so the
        // heatmap reflects the *correct* load even if the dispatcher hasn't
        // re-published yet.
        Map<CellKey, CellAccumulator> acc = new LinkedHashMap<>();
        Map<String, ZoneSummary> zoneSummary = new LinkedHashMap<>();

        // Build CuttingTimeCalculator inputs in one pass to batch the CMS DB hit.
        List<CuttingTimeCalculator.SerieInput<SerieKey>> timeInputs = new ArrayList<>();
        List<SerieRow> rows = new ArrayList<>();

        for (Object[] sr : serieRows) {
            String serieId   = (String) sr[0];
            String sequence  = (String) sr[1];
            String machineType = (String) sr[2];
            Double tempsDeCoupe = sr[3] != null ? ((Number) sr[3]).doubleValue() : null;
            Integer nbrCouche = sr[4] != null ? ((Number) sr[4]).intValue() : null;
            String placement = (String) sr[5];

            if (machineType == null || machineType.trim().isEmpty()) continue;

            RequestLight req = requestBySeq.get(sequence);
            if (req == null) continue; // orphan serie — should not happen
            if ("REJECTED".equalsIgnoreCase(req.zoneAcceptanceStatus)) continue;

            boolean frozen = req.pinnedByChef || "ACCEPTED".equalsIgnoreCase(req.zoneAcceptanceStatus);
            Optional<Zone> zoneOpt;
            if (frozen && req.dispatchedZone != null) {
                // Use pre-loaded zone map instead of hitting the DB.
                Zone z = zoneByNom.get(req.dispatchedZone);
                zoneOpt = Optional.ofNullable(z);
            } else {
                SerieDispatchInfo info = new SerieDispatchInfo(
                        serieId, sequence, machineType,
                        tempsDeCoupe, nbrCouche, placement, req.preferredZoneNom);
                zoneOpt = serieZoneResolver.resolveZone(info, date, shift);
            }
            if (!zoneOpt.isPresent()) continue;
            Zone zone = zoneOpt.get();

            machineTypesSeen.add(machineType);
            SerieKey k = new SerieKey(sequence, serieId);
            rows.add(new SerieRow(k, zone, machineType, sequence, frozen));
            timeInputs.add(new CuttingTimeCalculator.SerieInput<>(
                    k, placement, tempsDeCoupe, nbrCouche, machineType));
        }

        Map<SerieKey, Double> minutesBySerie = cuttingTimeCalculator.resolveMinutesBatch(timeInputs);
        log.debug("ZoneLoadService phase 2: resolved {} series zones + batched cutting times in {} ms",
                rows.size(), System.currentTimeMillis() - stepStart);

        // =====================================================================
        // Phase 3 — Roll up into per-cell accumulators
        // =====================================================================
        stepStart = System.currentTimeMillis();
        for (SerieRow r : rows) {
            double minutes = minutesBySerie.getOrDefault(r.key, 0.0);
            CellKey ck = new CellKey(r.zone.getNom(), r.machineType);
            CellAccumulator a = acc.computeIfAbsent(ck, k -> new CellAccumulator(r.zone));
            a.plannedMinutes += minutes;
            if (r.frozen) {
                a.baselineMinutes += minutes;
            } else {
                a.pendingMinutes += minutes;
            }
            a.sequenceSet.add(r.sequence);

            ZoneSummary zs = zoneSummary.computeIfAbsent(r.zone.getNom(),
                    n -> new ZoneSummary(r.zone));
            zs.plannedMinutes += minutes;
            zs.sequenceSet.add(r.sequence);
        }
        log.debug("ZoneLoadService phase 3: rolled up accumulators in {} ms", System.currentTimeMillis() - stepStart);

        // =====================================================================
        // Phase 4 — Produce cell DTOs
        // =====================================================================
        stepStart = System.currentTimeMillis();
        // Stable column order: STRICT-zones-first sort, then alpha within types observed.
        List<String> machineTypes = new ArrayList<>(machineTypesSeen);
        Collections.sort(machineTypes, String.CASE_INSENSITIVE_ORDER);

        List<ZoneLoadDto.ZoneLoadCellDto> cells = new ArrayList<>();
        Map<String, List<Double>> loadPctsByZone = new LinkedHashMap<>();

        double effectiveShiftMinutes = shiftProperties.getDurationMinutes() > 0
                ? shiftProperties.getDurationMinutes()
                : DEFAULT_SHIFT_MINUTES_FALLBACK;

        // Pre-load active machines for every zone ONCE (was Z×T DB hits before).
        long tAmStart = System.currentTimeMillis();
        Map<String, Set<String>> activeMachinesByZone = new LinkedHashMap<>();
        for (Zone z : allActiveZones) {
            activeMachinesByZone.put(z.getNom(), activeMachineResolver.activeMachines(date, shift, z.getNom()));
        }
        long tAmMs = System.currentTimeMillis() - tAmStart;
        if (tAmMs > 200) {
            log.warn("ZoneLoadService pre-loaded active machines for {} zones in {} ms", allActiveZones.size(), tAmMs);
        }

        // Only 2 groupes exist (Coupe / Laser). Cache efficiency to avoid
        // 50–100 DB hits when iterating every (zone, type) cell.
        Map<String, Double> efficienceCache = new HashMap<>(4);

        for (Zone z : allActiveZones) {
            Set<String> typesPresent = typesByZone.getOrDefault(z.getNom(), Collections.emptySet());
            for (String type : machineTypes) {
                CellKey ck = new CellKey(z.getNom(), type);
                CellAccumulator a = acc.get(ck);
                boolean machinePresent = typesPresent.contains(type);

                int activeMachines = 0;
                if (machinePresent) {
                    Set<String> upInZone = activeMachinesByZone.getOrDefault(z.getNom(), Collections.emptySet());
                    Set<String> machinesOfType = machinesByZoneByType
                            .getOrDefault(z.getNom(), Collections.emptyMap())
                            .getOrDefault(type, Collections.emptySet());
                    if (!upInZone.isEmpty()) {
                        Set<String> intersect = new HashSet<>(machinesOfType);
                        intersect.retainAll(upInZone);
                        activeMachines = intersect.size();
                    } else if (dispatcherProperties.isAllowUnconfirmedZones()) {
                        // No confirmation yet — fall back to all machines of this type in zone.
                        activeMachines = machinesOfType.size();
                    }
                }

                double efficiencePct = lookupEfficiencePctCached(date, shift, type, efficienceCache);
                double capacityMinutes = activeMachines * effectiveShiftMinutes * (efficiencePct / 100.0);
                double plannedMinutes = a == null ? 0.0 : a.plannedMinutes;
                double loadPct = capacityMinutes > 0
                        ? (plannedMinutes / capacityMinutes) * 100.0
                        : 0.0;
                int sequencesCount = a == null ? 0 : a.sequenceSet.size();

                double baselineMinutes = a == null ? 0.0 : a.baselineMinutes;
                double pendingMinutes = a == null ? 0.0 : a.pendingMinutes;
                cells.add(new ZoneLoadDto.ZoneLoadCellDto(
                        z.getNom(),
                        z.getCategory() == null ? "STRICT" : z.getCategory().name(),
                        type,
                        round2(plannedMinutes),
                        round2(baselineMinutes),
                        round2(pendingMinutes),
                        round2(capacityMinutes),
                        round2(loadPct),
                        activeMachines,
                        sequencesCount,
                        machinePresent));

                if (machinePresent && capacityMinutes > 0) {
                    loadPctsByZone.computeIfAbsent(z.getNom(), n -> new ArrayList<>())
                                  .add(loadPct);
                }
            }
        }
        log.debug("ZoneLoadService phase 4: built {} cells in {} ms", cells.size(), System.currentTimeMillis() - stepStart);

        // =====================================================================
        // Phase 5: per-zone aggregate rows.
        // =====================================================================
        stepStart = System.currentTimeMillis();
        List<ZoneLoadDto.ZoneLoadRowDto> zoneRows = new ArrayList<>();
        Map<String, Double> intraByZone = new LinkedHashMap<>();
        for (Zone z : allActiveZones) {
            ZoneSummary zs = zoneSummary.get(z.getNom());
            // capacity = sum of capacities across types in this zone.
            double zoneCapacity = 0.0;
            int zoneActiveMachines = 0;
            Set<String> typesPresent = typesByZone.getOrDefault(z.getNom(), Collections.emptySet());
            for (String type : typesPresent) {
                Set<String> upInZone = activeMachinesByZone.getOrDefault(z.getNom(), Collections.emptySet());
                Set<String> machinesOfType = machinesByZoneByType
                        .getOrDefault(z.getNom(), Collections.emptyMap())
                        .getOrDefault(type, Collections.emptySet());
                int active;
                if (!upInZone.isEmpty()) {
                    Set<String> intersect = new HashSet<>(machinesOfType);
                    intersect.retainAll(upInZone);
                    active = intersect.size();
                } else if (dispatcherProperties.isAllowUnconfirmedZones()) {
                    active = machinesOfType.size();
                } else {
                    active = 0;
                }
                zoneActiveMachines += active;
                double effPct = lookupEfficiencePctCached(date, shift, type, efficienceCache);
                zoneCapacity += active * effectiveShiftMinutes * (effPct / 100.0);
            }
            double zonePlanned = zs == null ? 0.0 : zs.plannedMinutes;
            double zoneLoadPct = zoneCapacity > 0 ? (zonePlanned / zoneCapacity) * 100.0 : 0.0;
            int zoneSequences = zs == null ? 0 : zs.sequenceSet.size();
            double intraSpread = computeIntraSpread(loadPctsByZone.get(z.getNom()));
            intraByZone.put(z.getNom(), round2(intraSpread));

            zoneRows.add(new ZoneLoadDto.ZoneLoadRowDto(
                    z.getNom(),
                    z.getCategory() == null ? "STRICT" : z.getCategory().name(),
                    round2(zonePlanned),
                    round2(zoneCapacity),
                    round2(zoneLoadPct),
                    zoneActiveMachines,
                    zoneSequences,
                    round2(intraSpread)));
        }
        log.debug("ZoneLoadService phase 5: built {} zone rows in {} ms", zoneRows.size(), System.currentTimeMillis() - stepStart);

        // Step 6: equilibre summary.
        ZoneLoadDto.EquilibreSummaryDto equilibre = computeEquilibre(zoneRows, intraByZone);

        ZoneLoadDto.ThresholdsDto thresholdsDto = new ZoneLoadDto.ThresholdsDto(
                props.getWarningThresholdPct(),
                props.getDangerThresholdPct(),
                props.getIntraZoneSpreadTargetPct(),
                props.getIntraZoneSpreadWarningPct(),
                props.getInterZoneSpreadTargetPct(),
                props.getInterZoneSpreadWarningPct());

        long elapsedMs = System.currentTimeMillis() - startMs;
        if (elapsedMs > 500) {
            log.warn("ZoneLoadService.computeMatrix() took {} ms — sequences={}, cells={}", elapsedMs, requestRows.size(), cells.size());
        } else {
            log.debug("ZoneLoadService.computeMatrix() took {} ms", elapsedMs);
        }

        ZoneLoadDto result = new ZoneLoadDto(date, shift, machineTypes, cells, zoneRows, equilibre, thresholdsDto);
        String cacheKey = date + "|" + shift;
        cache.put(cacheKey, result);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        return result;
    }

    /**
     * Lightweight version of {@link #computeMatrix} that returns only the
     * equilibre summary. Used by header strips that don't need cells.
     */
    @Transactional(readOnly = true)
    public ZoneLoadDto.EquilibreSummaryDto computeEquilibre(LocalDate date, int shift) {
        return computeMatrix(date, shift).getEquilibre();
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Cached variant of the efficiency lookup. There are only two groupes
     * (Coupe / Laser) so this saves 50–100 DB calls per matrix computation.
     */
    private double lookupEfficiencePctCached(LocalDate date, int shift, String machineType,
                                             Map<String, Double> cache) {
        String groupe = groupeOf(machineType);
        Double cached = cache.get(groupe);
        if (cached != null) {
            return cached;
        }
        CapaciteInstallee ci = capaciteInstalleeService.getEffective(date, shift, groupe);
        double value = (ci == null || ci.getEfficienceTarget() == null)
                ? DEFAULT_EFFICIENCE_TARGET_FALLBACK
                : ci.getEfficienceTarget();
        cache.put(groupe, value);
        return value;
    }

    private static double computeIntraSpread(List<Double> loadPcts) {
        if (loadPcts == null || loadPcts.size() < 2) return 0.0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (Double v : loadPcts) {
            if (v == null) continue;
            if (v > max) max = v;
            if (v < min) min = v;
        }
        if (max == Double.NEGATIVE_INFINITY || min == Double.POSITIVE_INFINITY) return 0.0;
        return Math.max(0.0, max - min);
    }

    private ZoneLoadDto.EquilibreSummaryDto computeEquilibre(
            List<ZoneLoadDto.ZoneLoadRowDto> zoneRows,
            Map<String, Double> intraByZone) {

        // Restrict to STRICT zones for both intra-summary and inter-zone spread.
        List<ZoneLoadDto.ZoneLoadRowDto> strictRows = new ArrayList<>();
        for (ZoneLoadDto.ZoneLoadRowDto r : zoneRows) {
            if ("STRICT".equalsIgnoreCase(r.getZoneCategory()) && r.getActiveMachines() > 0) {
                strictRows.add(r);
            }
        }

        double worstIntra = 0.0;
        String worstIntraZone = null;
        double sumIntra = 0.0;
        int countIntra = 0;
        for (ZoneLoadDto.ZoneLoadRowDto r : strictRows) {
            Double v = intraByZone.get(r.getZoneNom());
            if (v == null) continue;
            sumIntra += v;
            countIntra++;
            if (v > worstIntra) {
                worstIntra = v;
                worstIntraZone = r.getZoneNom();
            }
        }
        double avgIntra = countIntra == 0 ? 0.0 : sumIntra / countIntra;

        // Inter-zone spread
        double maxLoad = Double.NEGATIVE_INFINITY;
        double minLoad = Double.POSITIVE_INFINITY;
        String hottest = null;
        String coolest = null;
        for (ZoneLoadDto.ZoneLoadRowDto r : strictRows) {
            if (r.getLoadPct() > maxLoad) { maxLoad = r.getLoadPct(); hottest = r.getZoneNom(); }
            if (r.getLoadPct() < minLoad) { minLoad = r.getLoadPct(); coolest = r.getZoneNom(); }
        }
        double interSpread = (maxLoad == Double.NEGATIVE_INFINITY
                || minLoad == Double.POSITIVE_INFINITY
                || strictRows.size() < 2)
                ? 0.0 : Math.max(0.0, maxLoad - minLoad);

        String intraStatus = classify(worstIntra,
                props.getIntraZoneSpreadTargetPct(),
                props.getIntraZoneSpreadWarningPct());
        String interStatus = classify(interSpread,
                props.getInterZoneSpreadTargetPct(),
                props.getInterZoneSpreadWarningPct());

        return new ZoneLoadDto.EquilibreSummaryDto(
                round2(worstIntra), worstIntraZone,
                round2(avgIntra), round2(interSpread),
                hottest, coolest,
                intraStatus, interStatus,
                intraByZone);
    }

    /** GREEN ≤ target, AMBER ≤ warning, RED otherwise. */
    private static String classify(double v, double target, double warning) {
        if (v <= target) return "GREEN";
        if (v <= warning) return "AMBER";
        return "RED";
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static <T> List<T> safe(List<T> in) {
        return in == null ? new ArrayList<>() : in;
    }

    // ----------------------------------------------------------------- internal

    private static final class CellKey {
        final String zoneNom;
        final String machineType;
        CellKey(String zoneNom, String machineType) {
            this.zoneNom = zoneNom;
            this.machineType = machineType;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CellKey)) return false;
            CellKey c = (CellKey) o;
            return zoneNom.equals(c.zoneNom) && machineType.equals(c.machineType);
        }
        @Override public int hashCode() {
            return zoneNom.hashCode() * 31 + machineType.hashCode();
        }
    }

    private static final class CellAccumulator {
        final Zone zone;
        double plannedMinutes;
        double baselineMinutes;
        double pendingMinutes;
        final Set<String> sequenceSet = new HashSet<>();
        CellAccumulator(Zone zone) { this.zone = zone; }
    }

    private static final class ZoneSummary {
        final Zone zone;
        double plannedMinutes;
        final Set<String> sequenceSet = new HashSet<>();
        ZoneSummary(Zone zone) { this.zone = zone; }
    }

    private static final class SerieKey {
        final String sequence;
        final String serie;
        SerieKey(String sequence, String serie) { this.sequence = sequence; this.serie = serie; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SerieKey)) return false;
            SerieKey k = (SerieKey) o;
            return sequence.equals(k.sequence) && serie.equals(k.serie);
        }
        @Override public int hashCode() { return sequence.hashCode() * 31 + serie.hashCode(); }
    }

    private static final class SerieRow {
        final SerieKey key;
        final Zone zone;
        final String machineType;
        final String sequence;
        final boolean frozen;
        SerieRow(SerieKey key, Zone zone, String machineType, String sequence, boolean frozen) {
            this.key = key; this.zone = zone; this.machineType = machineType; this.sequence = sequence; this.frozen = frozen;
        }
    }

    /** Lightweight request scalar used by the projection path. */
    private static final class RequestLight {
        final String sequence;
        final String dispatchedZone;
        final String zoneAcceptanceStatus;
        final boolean pinnedByChef;
        final String preferredZoneNom;
        RequestLight(String sequence, String dispatchedZone, String zoneAcceptanceStatus,
                     boolean pinnedByChef, String preferredZoneNom) {
            this.sequence = sequence;
            this.dispatchedZone = dispatchedZone;
            this.zoneAcceptanceStatus = zoneAcceptanceStatus;
            this.pinnedByChef = pinnedByChef;
            this.preferredZoneNom = preferredZoneNom;
        }
    }
}
