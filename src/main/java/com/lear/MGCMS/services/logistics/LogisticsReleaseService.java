package com.lear.MGCMS.services.logistics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lear.MGCMS.domain.CuttingRequest.SequenceStatus;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.domain.StockStatusReport;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.logistics.LogisticsAllocation;
import com.lear.MGCMS.domain.logistics.LogisticsAllocation.Status;
import com.lear.MGCMS.domain.logistics.LogisticsPicklist;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.logistics.LogisticsPicklistRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.StockStatusReportService;
import com.lear.MGCMS.services.dispatcher.LiveChargeDto;
import com.lear.MGCMS.services.dispatcher.LiveChargeService;
import com.lear.cms.repositories.SuiviPlanningRepository;

/**
 * Logistics <b>release advisor</b> and picklist commit service.
 *
 * <p>Logistics releases a batch of sequences by flipping
 * {@code suiviplanning.Statu} from {@code 'Non demarre'} to {@code 'Released'}.
 * Before they do, this service answers, for every active, due, not-yet-released
 * sequence:</p>
 * <ul>
 *   <li>is there enough fabric for its series (in the target zone rack, or only
 *       elsewhere) — {@code OK / OUT_OF_ZONE / SHORTAGE / NONE};</li>
 *   <li>which zone to release it into so no zone overloads — per-machine-type
 *       cutting load is balanced across the STRICT zones; series whose machine
 *       type only exists in a SHARED zone (laser / DIE / Gerber) execute there
 *       and are <b>excluded</b> from the strict-zone balance; a sequence with a
 *       serie already started in a STRICT zone is <b>pinned</b> to that zone;</li>
 *   <li>which rolls to stage in which rack (placement suggestion);</li>
 *   <li>grouped by fabric so same-{@code refTissus} work is released together
 *       (fewer heavy-roll changes).</li>
 * </ul>
 *
 * <p>It performs no writes (Phase 1). It reuses {@link LiveChargeService} for the
 * resolved per-serie zone routing and {@code ScanRouleau} + {@code Zone.rollLocations}
 * for current stock — no business logic is re-implemented.</p>
 */
@Service
public class LogisticsReleaseService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsReleaseService.class);

    private static final int SQL_BATCH_SIZE = 1000;
    /** A zone at or above this load% is flagged so logistics avoids piling more on it. */
    private static final double OVERLOAD_PCT = 100.0;
    /** Headroom (minutes) a rerouting target must beat the natural zone by before we suggest it. */
    private static final double REROUTE_MARGIN_MINUTES = 60.0;
    private static final int MAX_ROLLS_PER_SERIE = 8;
    private static final String RELEASABLE_STATU = "Non demarre";
    /** Zone-recommendation scoring weights: charge balance is PRIMARY (an over-capacity zone is
     *  gated to WAIT regardless); material locality and material CONSOLIDATION (fewest distinct
     *  fabric refs per zone) break ties among zones of otherwise similar charge. Raise
     *  W_CONSOLIDATION to group materials more aggressively at the cost of a looser charge balance. */
    private static final double W_BALANCE = 2.0;
    private static final double W_LOCALITY = 0.5;
    private static final double W_CONSOLIDATION = 0.3;
    /** Soft penalty weight for box (spreading-table) occupancy in the zone score. */
    private static final double W_BOX = 1.0;
    /** Spreading boxes one machine can hold — mirrors WorkbenchSequenceFocusService. */
    private static final int BOXES_PER_MACHINE = 16;

    @Autowired private LiveChargeService liveChargeService;
    @Autowired private ScanRouleauRepository scanRouleauRepository;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private SuiviPlanningRepository suiviPlanningRepository;
    @Autowired private AllocationService allocationService;
    @Autowired private SerieRouleauTempService serieRouleauTempService;
    @Autowired private CuttingRequestRepository cuttingRequestRepository;
    @Autowired private CuttingRequestSerieDataRepository cuttingRequestSerieDataRepository;
    @Autowired private SequenceStatusService sequenceStatusService;
    @Autowired private LogisticsPicklistRepository logisticsPicklistRepository;
    @Autowired private CuttingRequestBoxInfoRepository boxInfoRepository;
    @Autowired private StockStatusReportService stockStatusReportService;
    @Autowired private ObjectMapper objectMapper;
    /** Primary (MGCMS) persistence context — used to flush deferred writes inside commit()'s guarded region. */
    @PersistenceContext private EntityManager em;

    public Map<String, Object> build(LocalDate date, int shift) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", now.toString());
        out.put("date", date != null ? date.toString() : null);
        out.put("shift", shift);

        // In-production live charge supplies per-zone load/headroom context only.
        // It does NOT carry the candidates: the picklist releases IMPORTED
        // sequences, which the workbench/liveCharge filter deliberately excludes.
        LiveChargeDto liveCharge = liveChargeService.compute();
        List<LiveChargeDto.ZoneChargeDto> liveZones =
                (liveCharge != null && liveCharge.getZones() != null)
                        ? liveCharge.getZones() : Collections.emptyList();

        StockIndex stock = buildStockIndex();
        Map<String, LiveChargeDto.ZoneChargeDto> zonesByName = new LinkedHashMap<>();
        for (LiveChargeDto.ZoneChargeDto z : liveZones) {
            if (z != null && z.getZoneNom() != null) zonesByName.put(z.getZoneNom(), z);
        }

        // 0. Per-zone box (spreading-table) occupancy over the in-production sequences:
        //    occupied = Σ boxCount over locked+pending sequences; capacity = activeMachines × 16.
        BoxIndex boxIndex = buildBoxIndex(liveZones);

        // 1+2. Candidates = IMPORTED (not-yet-released) sequences due in this slot.
        List<SeqCarrier> candidates = loadImportedCandidates(date, shift);

        // 2b. Zone recommendation (dispatch): oldest-first, balance machine-type
        //     charge across zones + material locality; ASSIGN or WAIT per sequence.
        //     A zone whose spreading boxes are saturated is excluded / penalized.
        // Capture the projected per-(zone, machineType) charge after greedy placement
        // so the balance table can show the "rectified" delta if the advice is respected.
        Map<String, Map<String, double[]>> projectedLoad = new LinkedHashMap<>();
        Map<String, ZoneDecision> decisions =
                recommendZones(candidates, liveZones, stock, boxIndex.loadByZone, projectedLoad);

        // 3. Build per-sequence advice + accumulate per-zone and per-material rollups.
        DemandIndex demand = new DemandIndex();
        List<SequenceAdvice> advices = new ArrayList<>();
        for (SeqCarrier carrier : candidates) {
            SequenceAdvice advice = buildAdvice(carrier, zonesByName, stock, demand,
                    decisions.get(carrier.sequence.getSequence()));
            advices.add(advice);
        }

        advices.sort(Comparator
                .comparingInt((SequenceAdvice a) -> statusOrder(a.materialStatus))
                .thenComparing(a -> a.dueDate != null ? a.dueDate.toString() : "9999")
                .thenComparing(a -> a.sequence != null ? a.sequence : ""));

        // 4. Zone rollup.
        List<Map<String, Object>> zoneRows = new ArrayList<>();
        int zonesOverloaded = 0;
        Map<String, Integer> candidatesByZone = new LinkedHashMap<>();
        for (SequenceAdvice a : advices) {
            if (a.suggestedZone != null) {
                candidatesByZone.merge(a.suggestedZone, 1, Integer::sum);
            }
        }
        for (LiveChargeDto.ZoneChargeDto z : liveZones) {
            if (z == null || z.getZoneNom() == null) continue;
            boolean overloaded = z.getOverallLoadPct() >= OVERLOAD_PCT;
            if (overloaded) zonesOverloaded++;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("zone", z.getZoneNom());
            row.put("category", z.getCategory());
            row.put("loadPct", round2(z.getOverallLoadPct()));
            row.put("headroomMinutes", round2(headroom(z)));
            row.put("overloaded", overloaded);
            row.put("candidateCount", candidatesByZone.getOrDefault(z.getZoneNom(), 0));
            row.put("occupiedBoxes", boxIndex.occupied(z.getZoneNom()));
            row.put("boxCapacity", boxIndex.capacity(z.getZoneNom()));
            row.put("boxOccupancyPct", boxIndex.occupancyPct(z.getZoneNom()));
            row.put("byMachineType", machineTypeRows(z, projectedLoad.get(z.getZoneNom())));
            zoneRows.add(row);
        }
        if (zoneRows.isEmpty()) {
            // No in-production load this slot — still surface the zones (0% load)
            // so logistics sees where the IMPORTED candidates would land.
            for (Zone z : zoneRepository.findAllActive()) {
                if (z.getNom() == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("zone", z.getNom());
                row.put("category", z.getCategory() != null ? z.getCategory().name() : null);
                row.put("loadPct", 0.0);
                row.put("headroomMinutes", 0.0);
                row.put("overloaded", false);
                row.put("candidateCount", candidatesByZone.getOrDefault(z.getNom(), 0));
                row.put("occupiedBoxes", 0);
                row.put("boxCapacity", 0);
                row.put("boxOccupancyPct", 0.0);
                row.put("byMachineType", new ArrayList<>());
                zoneRows.add(row);
            }
        }

        // 5. Material grouping across all candidate series.
        List<Map<String, Object>> materialRows = demand.toRows(stock);

        // 6. Transfer / fill / return rules over TRUE stock.
        List<Map<String, Object>> transferAfterUse = buildTransferAfterUse(demand, stock, advices);
        List<Map<String, Object>> fillPlan = buildFillPlan(demand, stock);
        List<Map<String, Object>> returnToMagasin = buildReturnToMagasin(stock);

        int ready = (int) advices.stream().filter(a -> "OK".equals(a.materialStatus)).count();
        int short_ = advices.size() - ready;

        out.put("zones", zoneRows);
        out.put("materials", materialRows);
        out.put("sequences", advices.stream().map(SequenceAdvice::toMap).collect(Collectors.toList()));
        out.put("transferAfterUse", transferAfterUse);
        out.put("fillPlan", fillPlan);
        out.put("returnToMagasin", returnToMagasin);
        out.put("totals", totals(candidates.size(), ready, short_, zonesOverloaded, materialRows.size()));
        return out;
    }

    /**
     * Stage 1 of the staged page load: the raw IMPORTED candidate sequences with
     * their demand — <b>no</b> stock read and <b>no</b> zone advice — so the UI can
     * show the sequence list immediately while the heavier {@link #build} advice
     * (stock + zone recommendation) is still computing.
     */
    public Map<String, Object> sequencesPreview(LocalDate date, int shift) {
        List<SeqCarrier> candidates = loadImportedCandidates(date, shift);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SeqCarrier carrier : candidates) {
            LiveChargeDto.SequenceDto seq = carrier.sequence;
            List<Map<String, Object>> series = new ArrayList<>();
            Set<String> machines = new LinkedHashSet<>();
            double neededTotal = 0.0;
            for (LiveChargeDto.SerieDto s : safeSeries(seq)) {
                if (isComplete(s.getStatusCoupe())) continue;
                double needed = Math.max(0.0, s.getTableLengthRequired());
                Map<String, Object> sr = new LinkedHashMap<>();
                sr.put("serie", s.getSerie());
                sr.put("machine", s.getMachine());
                sr.put("refTissus", normalizeMaterial(s.getRefTissus()));
                sr.put("neededMeters", round2(needed));
                series.add(sr);
                neededTotal += needed;
                if (s.getMachine() != null) machines.add(s.getMachine());
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sequence", seq.getSequence());
            row.put("dueDate", seq.getDueDate() != null ? seq.getDueDate().toString() : null);
            row.put("dueShift", carrier.dueShift);
            row.put("machines", new ArrayList<>(machines));
            row.put("serieCount", series.size());
            row.put("neededMeters", round2(neededTotal));
            row.put("series", series);
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sequences", rows);
        out.put("count", rows.size());
        return out;
    }

    /**
     * Load the IMPORTED (not-yet-released) sequences for this slot as advice
     * carriers. The picklist releases IMPORTED → RELEASED, but the workbench /
     * liveCharge feed only carries the in-production set {RELEASED, STARTED,
     * MATERIAL_MISSING}, so the candidates are loaded here directly. Series are
     * read as entities (clean getters, no projection-index coupling) and wrapped
     * in the same DTOs {@link #buildAdvice} consumes. A sequence whose serie has
     * physically started in a STRICT zone (zoneMatelassage / zoneCoupe written)
     * is marked locked and pinned to that zone; otherwise it sits unlocked in
     * its preferred / dispatched zone.
     */
    private List<SeqCarrier> loadImportedCandidates(LocalDate date, int shift) {
        List<Object[]> rows = cuttingRequestRepository.findImportedDueOnOrBeforeLight(
                date, String.valueOf(shift));
        if (rows.isEmpty()) return Collections.emptyList();

        Map<String, String> zoneBySeq = new LinkedHashMap<>();
        Map<String, LocalDate> dueBySeq = new LinkedHashMap<>();
        Map<String, String> dueShiftBySeq = new LinkedHashMap<>();
        List<String> sequences = new ArrayList<>();
        for (Object[] r : rows) {
            String seq = (String) r[0];
            if (seq == null) continue;
            String dispatchedZone = (String) r[1];
            String preferredZone = (String) r[2];
            LocalDate due = r[3] instanceof LocalDate ? (LocalDate) r[3] : null;
            String dueShift = r[4] != null ? String.valueOf(r[4]) : null;
            String releaseZone = (String) r[5];
            String effectiveZone = releaseZone != null ? releaseZone
                    : (dispatchedZone != null ? dispatchedZone : preferredZone);
            zoneBySeq.put(seq, effectiveZone);
            dueBySeq.put(seq, due);
            dueShiftBySeq.put(seq, dueShift);
            sequences.add(seq);
        }

        Map<String, List<CuttingRequestSerieData>> seriesBySeq = new LinkedHashMap<>();
        for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
            for (CuttingRequestSerieData s : cuttingRequestSerieDataRepository.findBySequencesArr(batch)) {
                if (s.getSequence() == null) continue;
                seriesBySeq.computeIfAbsent(s.getSequence(), k -> new ArrayList<>()).add(s);
            }
        }

        // Zone categories for pin detection: a serie physically started in a
        // STRICT zone pins the whole sequence there (work began before release).
        Map<String, Zone.Category> categoryByZone = new LinkedHashMap<>();
        for (Zone z : zoneRepository.findAllActive()) {
            if (z.getNom() != null) categoryByZone.put(z.getNom(), z.getCategory());
        }

        List<SeqCarrier> candidates = new ArrayList<>();
        for (String seq : sequences) {
            String effectiveZone = zoneBySeq.get(seq);
            List<CuttingRequestSerieData> series = seriesBySeq.getOrDefault(seq, Collections.emptyList());

            // Pin: zoneMatelassage / zoneCoupe is only written when work starts.
            String startedZone = null;
            for (CuttingRequestSerieData s : series) {
                String z = firstNonBlank(s.getZoneMatelassage(), s.getZoneCoupe());
                if (z != null && categoryByZone.get(z) == Zone.Category.STRICT) {
                    startedZone = z;
                    break;
                }
            }
            boolean pinned = startedZone != null;
            String ownerZone = pinned ? startedZone : effectiveZone;

            List<LiveChargeDto.SerieDto> serieDtos = new ArrayList<>();
            double seqMinutes = 0.0;
            for (CuttingRequestSerieData s : series) {
                double longueur = s.getLongueur() != null ? s.getLongueur() : 0.0;
                int couches = s.getNbrCouche() != null ? s.getNbrCouche() : 0;
                double tableLengthRequired = round2(longueur * couches);
                double estMinutes = s.getTempsDeCoupe() != null ? s.getTempsDeCoupe() : 0.0;
                if (!isComplete(s.getStatusCoupe())) seqMinutes += estMinutes;
                serieDtos.add(new LiveChargeDto.SerieDto(
                        s.getSerie(), s.getMachine(), s.getTempsDeCoupe(),
                        estMinutes, "TEMPS_DE_COUPE",
                        s.getStatusCoupe(), s.getStatusMatelassage(),
                        null, null,
                        null, null, null, null,
                        0.0, estMinutes,
                        null, null,
                        s.getPartNumberMaterial(), null,
                        tableLengthRequired));
            }
            LiveChargeDto.SequenceDto seqDto = new LiveChargeDto.SequenceDto(
                    seq, effectiveZone, effectiveZone,
                    ownerZone, "IMPORTED",
                    false, pinned,
                    null, null, null, null,
                    false, null,
                    round2(seqMinutes), serieDtos,
                    dueBySeq.get(seq), 0.0, null);
            SeqCarrier carrier = new SeqCarrier(seqDto, ownerZone);
            carrier.releaseStatu = RELEASABLE_STATU;
            carrier.dueShift = dueShiftBySeq.get(seq);
            candidates.add(carrier);
        }
        return candidates;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return null;
    }

    /**
     * Dispatch recommendation for the IMPORTED candidates. Sequences are placed
     * oldest-first (dueDate, dueShift); each is sent to the STRICT zone that
     * best balances per-machine-type cutting load across the strict zones,
     * while preferring the zone where the fabric already sits (locality) and
     * consolidating materials so each zone finishes with the fewest distinct
     * fabric refs (fewer roll changes). The running zone load is updated as each
     * sequence is placed (greedy / sequential), so two sequences never both
     * claim the same headroom; a sequence whose eligible zones would all
     * exceed capacity is told to WAIT.
     *
     * <p><b>Shared-zone rule:</b> a serie whose machine type is hosted by a
     * SHARED zone but not by the candidate STRICT zone executes in the shared
     * zone (mirrors {@link LiveChargeService}'s per-serie routing), so its
     * minutes are <b>not counted</b> in the strict-zone balance — only the
     * work the strict zone itself will execute weighs in. A sequence whose
     * every serie routes to shared zones (all-laser / DIE / Gerber work) is
     * assigned to the shared zone hosting the largest share of its minutes.</p>
     */
    private Map<String, ZoneDecision> recommendZones(List<SeqCarrier> candidates,
                                                     List<LiveChargeDto.ZoneChargeDto> liveZones,
                                                     StockIndex stock,
                                                     Map<String, Double> boxLoadByZone,
                                                     Map<String, Map<String, double[]>> projectedLoadOut) {
        Map<String, ZoneDecision> out = new LinkedHashMap<>();
        if (candidates.isEmpty()) return out;

        // Mutable per-zone, per-machineType load model: zone -> type -> {used, capacity}.
        Map<String, Map<String, double[]>> load = new LinkedHashMap<>();
        Map<String, String> categoryByZone = new LinkedHashMap<>();
        for (LiveChargeDto.ZoneChargeDto z : liveZones) {
            if (z == null || z.getZoneNom() == null) continue;
            Map<String, double[]> types = new LinkedHashMap<>();
            if (z.getByMachineType() != null) {
                for (LiveChargeDto.MachineTypeChargeDto mt : z.getByMachineType()) {
                    if (mt == null || mt.getMachineType() == null || mt.getActiveMachines() <= 0) continue;
                    types.put(mt.getMachineType(),
                            new double[]{mt.getTotalRemainingMinutes(), mt.getCapacityMinutes()});
                }
            }
            load.put(z.getZoneNom(), types);
            categoryByZone.put(z.getZoneNom(),
                    z.getCategory() != null ? z.getCategory() : "STRICT");
        }
        // Types absorbed by SHARED zones (active machines only). A serie of such
        // a type executes there whenever its owner zone lacks the machines, so
        // it must not weigh in the strict-zone balance.
        Set<String> sharedHostedTypes = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, double[]>> e : load.entrySet()) {
            if ("SHARED".equalsIgnoreCase(categoryByZone.get(e.getKey()))) {
                sharedHostedTypes.addAll(e.getValue().keySet());
            }
        }

        // Oldest-first: dueDate, then dueShift, then sequence id.
        List<SeqCarrier> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator
                .comparing((SeqCarrier c) -> c.sequence.getDueDate() != null
                        ? c.sequence.getDueDate().toString() : "9999-12-31")
                .thenComparing(c -> c.dueShift != null ? c.dueShift : "9")
                .thenComparing(c -> c.sequence.getSequence() != null ? c.sequence.getSequence() : ""));

        Map<String, Set<String>> assignedMaterialsByZone = new LinkedHashMap<>();
        for (SeqCarrier c : ordered) {
            String seq = c.sequence.getSequence();

            Map<String, Double> addedByType = new LinkedHashMap<>();
            Map<String, Double> neededByMaterial = new LinkedHashMap<>();
            for (LiveChargeDto.SerieDto s : safeSeries(c.sequence)) {
                if (isComplete(s.getStatusCoupe())) continue;
                if (s.getMachine() != null) {
                    addedByType.merge(s.getMachine(), Math.max(0.0, s.getRemainingMinutes()), Double::sum);
                }
                String mat = normalizeMaterial(s.getRefTissus());
                double need = Math.max(0.0, s.getTableLengthRequired());
                if (mat != null && need > 0) neededByMaterial.merge(mat, need, Double::sum);
            }

            // Physically started sequence: pinned to its zone, no balancing —
            // but its minutes still claim that zone's headroom for what follows.
            if (c.sequence.isLocked() && c.sequence.getEffectiveZone() != null) {
                String zn = c.sequence.getEffectiveZone();
                applyLoad(load.get(zn), addedByType);
                assignedMaterialsByZone.computeIfAbsent(zn, k -> new LinkedHashSet<>())
                        .addAll(neededByMaterial.keySet());
                out.put(seq, new ZoneDecision(zn, "ASSIGN",
                        "Séquence déjà entamée — zone physique " + zn));
                continue;
            }

            if (load.isEmpty()) {
                // No zone topology at all: fall back to the sequence's own zone.
                String fallback = c.sequence.getEffectiveZone() != null
                        ? c.sequence.getEffectiveZone() : c.ownerZone;
                out.put(seq, fallback != null
                        ? new ZoneDecision(fallback, "ASSIGN", "Zone préférée (charge live indisponible)")
                        : new ZoneDecision(null, "WAIT", "Aucune zone active"));
                continue;
            }

            double totalNeed = neededByMaterial.values().stream().mapToDouble(Double::doubleValue).sum();

            // Score every STRICT zone. Per zone, the sequence's types split into:
            //   hosted here   → projected onto this zone's load (counted);
            //   shared-hosted → executes in the SHARED zone (NOT counted);
            //   neither       → would silently land on another strict zone — disqualify.
            String best = null;
            double bestScore = -1e9;
            double bestProjLoad = Double.MAX_VALUE;
            boolean anyDroppedForBoxes = false;
            for (Map.Entry<String, Map<String, double[]>> ze : load.entrySet()) {
                String zn = ze.getKey();
                if ("SHARED".equalsIgnoreCase(categoryByZone.get(zn))) continue;
                Map<String, double[]> types = ze.getValue();
                double projMax = 0.0;
                int countable = 0;
                boolean disqualified = false;
                for (Map.Entry<String, Double> e : addedByType.entrySet()) {
                    double[] uc = types.get(e.getKey());
                    if (uc != null) {
                        double cap = uc[1] > 0 ? uc[1] : 0.0;
                        double proj = cap > 0 ? (uc[0] + e.getValue()) / cap : 2.0;
                        projMax = Math.max(projMax, proj);
                        countable++;
                    } else if (!sharedHostedTypes.contains(e.getKey())) {
                        disqualified = true;
                        break;
                    }
                }
                if (disqualified) continue;
                if (countable == 0 && !addedByType.isEmpty()) continue; // nothing executes here
                if (boxFull(boxLoadByZone, zn)) {
                    anyDroppedForBoxes = true;
                    continue;
                }

                double inZone = 0.0;
                for (Map.Entry<String, Double> e : neededByMaterial.entrySet()) {
                    inZone += Math.min(e.getValue(), stock.meters(e.getKey(), zn));
                }
                double locality = totalNeed > 0 ? inZone / totalNeed : 0.0;
                // Material consolidation: reward sending this sequence to a zone that ALREADY
                // works its fabric refs and penalise introducing NEW refs, so each zone finishes
                // its set with the FEWEST distinct materials (fewer roll changes). In [-1, 1]:
                // +1 = every ref already in this zone (adds no new material), -1 = every ref is
                // new here. This is what decides between two zones of otherwise similar charge:
                // a zone that can finish with 2 refs is preferred over one that would need 20.
                Set<String> zoneMats = assignedMaterialsByZone.getOrDefault(zn, Collections.emptySet());
                int seqRefCount = neededByMaterial.size();
                int sharedRefs = 0;
                for (String m : neededByMaterial.keySet()) if (zoneMats.contains(m)) sharedRefs++;
                double consolidation = seqRefCount > 0
                        ? (2.0 * sharedRefs - seqRefCount) / (double) seqRefCount : 0.0;
                double boxLoad = boxLoadByZone != null
                        ? boxLoadByZone.getOrDefault(zn, 0.0) : 0.0;
                double starvationBonus = projMax < 0.30 ? 0.3 : 0.0;
                double score = W_BALANCE * (1.0 - Math.min(projMax, 1.5))
                        + W_LOCALITY * locality
                        + W_CONSOLIDATION * consolidation
                        - W_BOX * boxLoad
                        + starvationBonus;
                if (score > bestScore) {
                    bestScore = score;
                    best = zn;
                    bestProjLoad = projMax;
                }
            }

            if (best != null) {
                if (bestProjLoad <= 1.0) {
                    applyLoad(load.get(best), addedByType);
                    assignedMaterialsByZone.computeIfAbsent(best, k -> new LinkedHashSet<>())
                            .addAll(neededByMaterial.keySet());
                    Set<String> bestMats = assignedMaterialsByZone.get(best);
                    out.put(seq, new ZoneDecision(best, "ASSIGN",
                            String.format(Locale.ROOT, "Zone %s — charge projetée %.0f%% — %d réf. tissu dans la zone",
                                    best, bestProjLoad * 100.0, bestMats != null ? bestMats.size() : 0)));
                } else {
                    out.put(seq, new ZoneDecision(best, "WAIT",
                            "Zones éligibles saturées — attendre qu'une zone se libère"));
                }
                continue;
            }

            if (anyDroppedForBoxes) {
                // Every strict zone that could host this work is box-saturated.
                out.put(seq, new ZoneDecision(null, "WAIT",
                        "Zones pleines (bacs saturés) — attendre"));
                continue;
            }

            // No strict zone executes anything of this sequence. All-shared work
            // (laser / DIE / Gerber only): own it in the shared zone carrying the
            // largest share of its minutes.
            if (!addedByType.isEmpty() && sharedHostedTypes.containsAll(addedByType.keySet())) {
                String bestShared = null;
                double bestSharedMinutes = -1.0;
                double bestSharedProj = Double.MAX_VALUE;
                boolean sharedDroppedForBoxes = false;
                for (Map.Entry<String, Map<String, double[]>> ze : load.entrySet()) {
                    String zn = ze.getKey();
                    if (!"SHARED".equalsIgnoreCase(categoryByZone.get(zn))) continue;
                    Map<String, double[]> types = ze.getValue();
                    double hostedMinutes = 0.0;
                    double projMax = 0.0;
                    for (Map.Entry<String, Double> e : addedByType.entrySet()) {
                        double[] uc = types.get(e.getKey());
                        if (uc == null) continue;
                        hostedMinutes += e.getValue();
                        double cap = uc[1] > 0 ? uc[1] : 0.0;
                        double proj = cap > 0 ? (uc[0] + e.getValue()) / cap : 2.0;
                        projMax = Math.max(projMax, proj);
                    }
                    if (hostedMinutes <= 0.0 && !types.keySet().containsAll(addedByType.keySet())) continue;
                    if (boxFull(boxLoadByZone, zn)) {
                        sharedDroppedForBoxes = true;
                        continue;
                    }
                    if (hostedMinutes > bestSharedMinutes
                            || (hostedMinutes == bestSharedMinutes && projMax < bestSharedProj)) {
                        bestShared = zn;
                        bestSharedMinutes = hostedMinutes;
                        bestSharedProj = projMax;
                    }
                }
                if (bestShared != null) {
                    if (bestSharedProj <= 1.0) {
                        applyLoad(load.get(bestShared), addedByType);
                        out.put(seq, new ZoneDecision(bestShared, "ASSIGN",
                                "Travail en zone partagée " + bestShared + " (hors équilibre zones strictes)"));
                    } else {
                        out.put(seq, new ZoneDecision(bestShared, "WAIT",
                                "Zone partagée saturée — attendre"));
                    }
                    continue;
                }
                if (sharedDroppedForBoxes) {
                    out.put(seq, new ZoneDecision(null, "WAIT",
                            "Zone partagée pleine (bacs saturés) — attendre"));
                    continue;
                }
            }

            // Some machine type has no active machine anywhere — name it.
            Set<String> unplaceable = new LinkedHashSet<>();
            for (String t : addedByType.keySet()) {
                boolean hosted = load.values().stream().anyMatch(m -> m.containsKey(t));
                if (!hosted) unplaceable.add(t);
            }
            if (!unplaceable.isEmpty()) {
                out.put(seq, new ZoneDecision(null, "WAIT",
                        "Aucune machine active pour: " + String.join(", ", unplaceable)));
            } else {
                String fallback = c.sequence.getEffectiveZone() != null
                        ? c.sequence.getEffectiveZone() : c.ownerZone;
                out.put(seq, fallback != null
                        ? new ZoneDecision(fallback, "ASSIGN", "Zone préférée (équilibrage indisponible)")
                        : new ZoneDecision(null, "WAIT", "Aucune zone n'héberge les machines requises"));
            }
        }
        // Expose the post-placement projected charge per (zone, machineType): used[0] is
        // now the charge after releasing every ASSIGN sequence; cap[1] is the capacity.
        if (projectedLoadOut != null) {
            for (Map.Entry<String, Map<String, double[]>> e : load.entrySet()) {
                Map<String, double[]> copy = new LinkedHashMap<>();
                for (Map.Entry<String, double[]> te : e.getValue().entrySet()) {
                    copy.put(te.getKey(), new double[]{ te.getValue()[0], te.getValue()[1] });
                }
                projectedLoadOut.put(e.getKey(), copy);
            }
        }
        return out;
    }

    /** Add this sequence's minutes to the zone's per-type model — hosted types only. */
    private static void applyLoad(Map<String, double[]> types, Map<String, Double> addedByType) {
        if (types == null) return;
        for (Map.Entry<String, Double> e : addedByType.entrySet()) {
            double[] uc = types.get(e.getKey());
            if (uc != null) uc[0] += e.getValue();
        }
    }

    /**
     * Per-machineType charge for a zone: current in-production load, the load
     * projected after releasing every recommended ASSIGN sequence this slot, and
     * the delta between them. {@code projected} is null when the zone carried no
     * live load (then projected == current). Drives the /logisticsRelease balance
     * table (one row per STRICT zone, a cell per Lectra / Lectra IP6 machine type).
     */
    private List<Map<String, Object>> machineTypeRows(LiveChargeDto.ZoneChargeDto z,
                                                      Map<String, double[]> projected) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (z == null || z.getByMachineType() == null) return rows;
        for (LiveChargeDto.MachineTypeChargeDto mt : z.getByMachineType()) {
            if (mt == null || mt.getMachineType() == null) continue;
            double capacity = mt.getCapacityMinutes();
            double current = mt.getTotalRemainingMinutes();
            double[] proj = projected != null ? projected.get(mt.getMachineType()) : null;
            double projectedMinutes = proj != null ? proj[0] : current;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("machineType", mt.getMachineType());
            row.put("activeMachines", mt.getActiveMachines());
            row.put("capacityMinutes", round2(capacity));
            row.put("currentMinutes", round2(current));
            row.put("projectedMinutes", round2(projectedMinutes));
            row.put("deltaMinutes", round2(projectedMinutes - current));
            row.put("currentLoadPct", round2(capacity > 0 ? current / capacity * 100.0 : 0.0));
            row.put("projectedLoadPct", round2(capacity > 0 ? projectedMinutes / capacity * 100.0 : 0.0));
            rows.add(row);
        }
        return rows;
    }

    /**
     * Commit the selected advisory rows into production:
     * <ol>
     *   <li>guard against stale UI by recomputing candidates;</li>
     *   <li>flip {@code suiviplanning.Statu} to {@code Released} in the CMS datasource;</li>
     *   <li>mirror the MG-CMS lifecycle to {@code RELEASED} with the fixed zone;</li>
     *   <li>write RELEASED ledger rows and persist a printable snapshot.</li>
     * </ol>
     *
     * <p>The CMS datasource and primary datasource are not XA-bound. We update
     * {@code suiviplanning} first because it is the cross-app source of truth,
     * then mirror locally. If a race means the external app already released a
     * sequence, the post-update status read accepts that as idempotent.</p>
     *
     * <p><b>Partial-failure handling (compensation).</b> {@code transition()}
     * keys off the <i>local</i> {@code sequenceStatus} (IMPORTED), not
     * suiviplanning, so it does not depend on the flip having happened first —
     * but the flip commits on the non-XA CMS datasource the instant
     * {@code releaseNonDemarreBySequences} returns, before any local mirror /
     * allocation / snapshot write. A full reorder (flip last) would have to drop
     * the race-safe guarded flip + its verification read, which the success
     * contract depends on; that is the more invasive change. Instead the local
     * steps after the flip are wrapped so that <i>any</i> failure reverts the
     * just-released suiviplanning rows back to {@code 'Non demarre'} (guarded:
     * only the exact rows this call flipped — their ids snapshotted before the
     * flip — and only rows still at {@code 'Released'}), preventing the divergence
     * the 20-min sync would otherwise mirror with no releaseZone. Because Hibernate
     * would otherwise defer the local mirror UPDATE and the picklist/allocation
     * inserts to the outer @Transactional commit (which runs after this try/catch),
     * the guarded region ends with an explicit {@code em.flush()} so a flush-time
     * constraint failure is caught and compensated here, not stranded after the
     * suiviplanning flip. Every partial-failure path logs the affected rows with
     * {@code log.error} so ops can reconcile.</p>
     */
    @Transactional
    public Map<String, Object> commit(LocalDate date, int shift, List<String> selectedSequences, String createdBy) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> selected = normalizeSequences(selectedSequences);
        if (selected.isEmpty()) {
            result.put("success", false);
            result.put("error", "Sélection vide");
            return result;
        }

        Map<String, Object> advicePayload = build(date, shift);
        Map<String, Map<String, Object>> adviceBySequence = adviceBySequence(advicePayload);
        List<String> missing = selected.stream()
                .filter(seq -> !adviceBySequence.containsKey(seq))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            result.put("success", false);
            result.put("error", "Séquence non releasable ou déjà sortie: " + String.join(", ", missing));
            result.put("missingSequences", missing);
            return result;
        }

        List<String> blocked = selected.stream()
                .filter(seq -> Boolean.TRUE.equals(adviceBySequence.get(seq).get("materialMissingSuggested")))
                .collect(Collectors.toList());
        if (!blocked.isEmpty()) {
            result.put("success", false);
            result.put("error", "Matière insuffisante: aucune série complète ne peut démarrer pour "
                    + String.join(", ", blocked));
            result.put("blockedSequences", blocked);
            return result;
        }

        // A WAIT recommendation (zone saturée / bacs pleins / aucune machine active) must not
        // be released: transition() would fall back to resolveZone() and push the work into the
        // exact overloaded/unstaffed zone the dispatch advice told logistics to avoid.
        List<String> waitBlocked = selected.stream()
                .filter(seq -> "WAIT".equals(value(adviceBySequence.get(seq).get("recommendation"))))
                .collect(Collectors.toList());
        if (!waitBlocked.isEmpty()) {
            result.put("success", false);
            result.put("error", "Recommandation d'attente (WAIT): zone saturée ou machine indisponible pour "
                    + String.join(", ", waitBlocked));
            result.put("blockedSequences", waitBlocked);
            return result;
        }

        // Capture the PRECISE rows this call will flip: the row ids currently at
        // 'Non demarre' for the selected sequences. Compensation reverts EXACTLY these
        // ids, so a sibling row of the same sequence that was already 'Released' by the
        // external CMS desktop app (or an earlier session) is never un-released by us if
        // a later local step fails.
        List<Long> flippedRowIds = suiviPlanningRepository.findNonDemarreRowIdsBySequences(selected);

        int suiviUpdated = suiviPlanningRepository.releaseNonDemarreBySequences(selected);
        Map<String, Set<String>> statusAfterRelease = loadReleaseStatu(selected);
        // A multi-row sequence is fully released only when NO row remains at 'Non demarre'
        // after the flip. A leftover 'Non demarre' row means the flip did not fully take
        // (race / missing rows), so we must NOT let the local mirror commit a half-released
        // sequence — treat it as notReleased and compensate.
        List<String> notReleased = selected.stream()
                .filter(seq -> statusAfterRelease.getOrDefault(seq, Collections.emptySet()).stream()
                        .anyMatch(s -> "Non demarre".equalsIgnoreCase(s == null ? "" : s.trim())))
                .collect(Collectors.toList());
        if (!notReleased.isEmpty()) {
            // Some rows flipped to Released but not all the selected sequences did
            // (a race, or rows missing from suiviplanning). No local mirror has run
            // yet, so there is nothing to roll back locally — but the rows that DID
            // flip would be orphaned at Released with no MG-CMS releaseZone. Revert
            // them (guarded to rows still at 'Released') so the batch is all-or-nothing.
            int reverted = compensateSuiviRelease(flippedRowIds,
                    "flip suiviplanning incomplet: " + String.join(", ", notReleased));
            result.put("success", false);
            result.put("error", "Impossible de passer suiviplanning à Released: " + String.join(", ", notReleased));
            result.put("suiviUpdatedRows", suiviUpdated);
            result.put("suiviRevertedRows", reverted);
            result.put("notReleasedSequences", notReleased);
            return result;
        }

        // suiviplanning is now Released (committed on the non-XA CMS datasource).
        // Everything below is local (primary datasource, joins this @Transactional)
        // plus the recap magasin read; on ANY failure we revert the rows this call
        // released so suiviplanning never sits at Released while the local mirror
        // stays IMPORTED (which the 20-min sync would otherwise propagate with no
        // releaseZone). See the method javadoc for the reorder-vs-compensate choice.
        try {
            List<Map<String, Object>> transitionResults = new ArrayList<>();
            List<String> localFailures = new ArrayList<>();
            for (String sequence : selected) {
                Map<String, Object> advice = adviceBySequence.get(sequence);
                String zone = value(advice.get("suggestedZone"));
                Map<String, Object> transition =
                        sequenceStatusService.transition(sequence, SequenceStatus.RELEASED, zone);
                transitionResults.add(transition);
                if (!Boolean.TRUE.equals(transition.get("success"))) {
                    localFailures.add(sequence + " (" + transition.get("error") + ")");
                }
            }
            if (!localFailures.isEmpty()) {
                // A normal return inside @Transactional COMMITS — so any earlier
                // transition() that already wrote sequenceStatus=RELEASED locally
                // would stick while suiviplanning is reverted (the inverse split).
                // Mark rollback-only so the primary side undoes those local writes,
                // matching the suiviplanning revert below.
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                int reverted = compensateSuiviRelease(flippedRowIds,
                        "miroir MG-CMS échoué: " + String.join("; ", localFailures));
                result.put("success", false);
                result.put("error", "suiviplanning est Released, mais le miroir MG-CMS a échoué: "
                        + String.join("; ", localFailures));
                result.put("suiviUpdatedRows", suiviUpdated);
                result.put("suiviRevertedRows", reverted);
                result.put("transitions", transitionResults);
                return result;
            }

            String picklistId = newPicklistId();
            for (String sequence : selected) {
                allocationService.cancel(sequence);
            }
            List<LogisticsAllocation> allocations = buildAllocations(picklistId, selected, adviceBySequence, createdBy);
            allocationService.reserve(allocations);

            // Magasin picklist: rolls to fetch when rack + on-table cannot cover the
            // committed + new demand for a material. This recap() is a DIFFERENT view
            // from build() above (RAW rack+on-table availability + magasin FIFO, not
            // build()'s TRUE deducted StockIndex), so its result cannot be taken from
            // advicePayload; it is computed exactly once here and reused for the
            // snapshot — there is no second recap()/build() pass in the commit path.
            List<Map<String, Object>> magasinPull = collectCommitMagasinPull(selected);

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("success", true);
            snapshot.put("picklistId", picklistId);
            snapshot.put("date", date != null ? date.toString() : null);
            snapshot.put("shift", shift);
            snapshot.put("createdAt", LocalDateTime.now().toString());
            snapshot.put("createdBy", createdBy);
            snapshot.put("sequences", selected.stream().map(adviceBySequence::get).collect(Collectors.toList()));
            snapshot.put("transferAfterUse", advicePayload.get("transferAfterUse"));
            snapshot.put("fillPlan", advicePayload.get("fillPlan"));
            snapshot.put("returnToMagasin", advicePayload.get("returnToMagasin"));
            snapshot.put("magasinPull", magasinPull);
            snapshot.put("suiviUpdatedRows", suiviUpdated);
            snapshot.put("allocationCount", allocations.size());
            snapshot.put("transitions", transitionResults);

            savePicklistSnapshot(picklistId, date, shift, selected.size(), createdBy, snapshot);
            // Force the primary datasource to flush every deferred write NOW — the
            // transition() UPDATE and the picklist/allocation inserts Hibernate would
            // otherwise defer to the outer @Transactional commit, which runs AFTER this
            // try/catch. Flushing here keeps a flush-time constraint failure inside the
            // guarded region so it is compensated synchronously instead of stranding the
            // committed suiviplanning flip.
            em.flush();
            return snapshot;
        } catch (RuntimeException ex) {
            // Local mirror/allocation/snapshot/flush threw after the suiviplanning flip
            // committed. Revert the released rows and rethrow so the primary
            // @Transactional still rolls back its own (CuttingRequestData /
            // allocation / picklist) writes — keeping both sides at pre-commit.
            compensateSuiviRelease(flippedRowIds, "exception après flip: " + ex);
            throw ex;
        }
    }

    /**
     * Compensating revert after the suiviplanning flip committed but the local
     * commit could not complete. Puts back to {@code 'Non demarre'} EXACTLY the rows
     * this call flipped ({@code rowIds} snapshotted before the flip, guarded to rows
     * still at {@code 'Released'}) and logs the affected rows loudly so ops can
     * reconcile. Never throws — a failed revert is itself logged, not allowed to mask
     * the original failure.
     *
     * @return the number of suiviplanning rows put back to {@code 'Non demarre'}
     */
    private int compensateSuiviRelease(List<Long> rowIds, String cause) {
        if (rowIds == null || rowIds.isEmpty()) return 0;
        try {
            int reverted = suiviPlanningRepository.revertReleasedToNonDemarreByIds(rowIds);
            log.error("Logistics commit failed after suiviplanning flip ({}). Reverted {} row(s) "
                            + "back to 'Non demarre' for row ids {}. Reconcile any not reverted.",
                    cause, reverted, rowIds);
            return reverted;
        } catch (RuntimeException revertEx) {
            log.error("CRITICAL: logistics commit failed AND the suiviplanning revert ALSO failed ({}). "
                            + "Row ids {} may be stuck at 'Released' with no MG-CMS releaseZone — "
                            + "reconcile manually.",
                    cause, rowIds, revertEx);
            return 0;
        }
    }

    private List<String> normalizeSequences(List<String> selectedSequences) {
        if (selectedSequences == null) return Collections.emptyList();
        return selectedSequences.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> adviceBySequence(Map<String, Object> advicePayload) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        Object rows = advicePayload.get("sequences");
        if (!(rows instanceof List)) return out;
        for (Object row : (List<?>) rows) {
            if (!(row instanceof Map)) continue;
            Map<String, Object> seq = (Map<String, Object>) row;
            String sequence = value(seq.get("sequence"));
            if (sequence != null) out.put(sequence, seq);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<LogisticsAllocation> buildAllocations(String picklistId,
                                                       List<String> selected,
                                                       Map<String, Map<String, Object>> adviceBySequence,
                                                       String createdBy) {
        List<LogisticsAllocation> allocations = new ArrayList<>();
        for (String sequence : selected) {
            Map<String, Object> advice = adviceBySequence.get(sequence);
            if (advice == null) continue;
            Object seriesObj = advice.get("series");
            if (!(seriesObj instanceof List)) continue;
            for (Object serieObj : (List<?>) seriesObj) {
                if (!(serieObj instanceof Map)) continue;
                Map<String, Object> serie = (Map<String, Object>) serieObj;
                String serieId = value(serie.get("serie"));
                String material = value(serie.get("refTissus"));
                String targetZone = value(serie.get("targetZone"));
                double remaining = number(serie.get("neededMeters"));
                Object placementsObj = serie.get("rollPlacements");
                if (!(placementsObj instanceof List)) continue;
                for (Object placementObj : (List<?>) placementsObj) {
                    if (!(placementObj instanceof Map) || remaining <= 0) continue;
                    Map<String, Object> placement = (Map<String, Object>) placementObj;
                    double meters = Math.min(remaining, number(placement.get("meters")));
                    if (meters <= 0) continue;
                    String sourceZone = value(placement.get("sourceZone"));
                    if (sourceZone == null) sourceZone = value(placement.get("zone"));
                    LogisticsAllocation allocation = new LogisticsAllocation(
                            sequence,
                            serieId,
                            material,
                            value(placement.get("serialId")),
                            value(placement.get("rack")),
                            sourceZone,
                            targetZone != null ? targetZone : value(advice.get("suggestedZone")),
                            round2(meters),
                            Status.RELEASED,
                            createdBy);
                    allocation.setPicklistId(picklistId);
                    allocations.add(allocation);
                    remaining -= meters;
                }
            }
        }
        return allocations;
    }

    private String newPicklistId() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "PL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + suffix;
    }

    private void savePicklistSnapshot(String picklistId,
                                      LocalDate date,
                                      int shift,
                                      int sequenceCount,
                                      String createdBy,
                                      Map<String, Object> snapshot) {
        LogisticsPicklist picklist = new LogisticsPicklist();
        picklist.setId(picklistId);
        picklist.setReleaseDate(date);
        picklist.setShift(shift);
        picklist.setSequenceCount(sequenceCount);
        picklist.setCreatedBy(createdBy);
        try {
            picklist.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            picklist.setSnapshotJson("{\"error\":\"snapshot serialization failed\"}");
        }
        logisticsPicklistRepository.save(picklist);
    }

    /**
     * The most recent persisted picklist snapshot for a (date, shift), deserialized
     * to the same shape {@link #commit} returned, so the UI can reprint an earlier
     * release even after a refresh wiped its in-memory commit result. Returns a
     * {@code success=false} map (not an exception) when no picklist exists for the
     * slot, so the front end can surface a plain message.
     */
    public Map<String, Object> lastPicklist(LocalDate date, int shift) {
        LogisticsPicklist picklist =
                logisticsPicklistRepository.findFirstByReleaseDateAndShiftOrderByCreatedAtDesc(date, shift);
        if (picklist == null || picklist.getSnapshotJson() == null) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", false);
            out.put("error", "Aucune picklist enregistrée pour ce créneau");
            return out;
        }
        try {
            return objectMapper.readValue(picklist.getSnapshotJson(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", false);
            out.put("error", "Snapshot de picklist illisible");
            return out;
        }
    }

    // ------------------------------------------------------------------ material recap

    /**
     * Material balance for a selection of sequences, answering "can this batch be
     * confirmed without a magasin shortage?". Per normalized material:
     * <ul>
     *   <li>{@code newMeters} — Σ (longueur × nbrCouche) over the selected
     *       sequences' not-Complete series (the demand this release adds);</li>
     *   <li>{@code committedMeters} — meters already promised to production but
     *       not yet spread (Waiting series of RELEASED/STARTED sequences);</li>
     *   <li>{@code availableMeters} — raw rack metrage (no reservation/on-table
     *       deduction) plus the on-table in-progress estimate;</li>
     *   <li>{@code remainingMeters} = available − committed − new.</li>
     * </ul>
     * A negative remainder is a deficit checked against the magasin
     * ({@link StockStatusReportService#getCurrentStock(List)}); the batch can be
     * confirmed only when no material is in SHORTAGE.
     */
    public Map<String, Object> recap(List<String> selectedSequences) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> selected = normalizeSequences(selectedSequences);
        if (selected.isEmpty()) {
            result.put("success", false);
            result.put("error", "Sélection vide");
            result.put("materials", Collections.emptyList());
            return result;
        }

        // Each metric carries its per-material total (the header number) and the
        // rows behind it (the cell drill-down). The committed TOTAL keeps its
        // proven aggregate query (it gates canConfirm); only its detail is added.
        Metric newM = newMetric(selected);
        Metric availableM = availableMetric();
        Map<String, Double> committedByMaterial = committedMetersByMaterial();
        Map<String, List<Map<String, Object>>> committedDetail = committedDetailByMaterial();

        // Materials in play = those this selection needs.
        Set<String> materials = new LinkedHashSet<>(newM.total.keySet());

        // First pass: compute the remainder per material and collect any deficit.
        Map<String, Double> deficitByMaterial = new LinkedHashMap<>();
        for (String material : materials) {
            double available = availableM.totalOf(material);
            double committed = committedByMaterial.getOrDefault(material, 0.0);
            double newMeters = newM.totalOf(material);
            double remaining = available - committed - newMeters;
            if (remaining < 0) deficitByMaterial.put(material, -remaining);
        }

        // Magasin lookup only when there is a real deficit (it parses R100.prn).
        Map<String, List<StockStatusReport>> magasinByMaterial =
                deficitByMaterial.isEmpty()
                        ? Collections.emptyMap()
                        : loadMagasinStock(deficitByMaterial.keySet());

        List<Map<String, Object>> materialRows = new ArrayList<>();
        int materialsOk = 0;
        int materialsCovered = 0;
        int materialsShort = 0;
        for (String material : materials) {
            double available = round2(availableM.totalOf(material));
            double committed = round2(committedByMaterial.getOrDefault(material, 0.0));
            double newMeters = round2(newM.totalOf(material));
            double remaining = round2(available - committed - newMeters);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("refTissus", material);
            row.put("newMeters", newMeters);
            row.put("committedMeters", committed);
            row.put("availableMeters", available);
            row.put("remainingMeters", remaining);
            row.put("breakdown", buildBreakdown(newM, committedDetail, availableM, material,
                    available, committed, newMeters, remaining));
            if (remaining >= 0) {
                row.put("status", "OK");
                row.put("magasinMeters", 0.0);
                row.put("magasinPull", Collections.emptyList());
                materialsOk++;
            } else {
                double deficit = -remaining;
                List<StockStatusReport> magasinRows = magasinByMaterial.getOrDefault(material, Collections.emptyList());
                double magasinMeters = round2(sumMagasinMeters(magasinRows));
                List<Map<String, Object>> pull = magasinPull(magasinRows, deficit);
                boolean covered = magasinMeters >= deficit;
                row.put("status", covered ? "COVERED" : "SHORTAGE");
                row.put("magasinMeters", magasinMeters);
                row.put("magasinPull", pull);
                if (covered) materialsCovered++; else materialsShort++;
            }
            materialRows.add(row);
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("materialsOk", materialsOk);
        totals.put("materialsCovered", materialsCovered);
        totals.put("materialsShort", materialsShort);

        result.put("success", true);
        result.put("selectedCount", selected.size());
        result.put("canConfirm", materialsShort == 0);
        result.put("materials", materialRows);
        result.put("totals", totals);
        return result;
    }

    /**
     * "Nouveau" demand the selection adds: Σ (longueur × nbrCouche) over the
     * selected sequences' not-Complete series, by normalized material — with the
     * contributing series exposed for the cell drill-down.
     */
    private Metric newMetric(List<String> selected) {
        Metric m = new Metric();
        for (int i = 0; i < selected.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = selected.subList(i, Math.min(i + SQL_BATCH_SIZE, selected.size()));
            for (CuttingRequestSerieData s : cuttingRequestSerieDataRepository.findBySequencesArr(batch)) {
                if (s == null || isComplete(s.getStatusCoupe())) continue;
                String material = normalizeMaterial(s.getPartNumberMaterial());
                if (material == null) continue;
                double longueur = s.getLongueur() != null ? s.getLongueur() : 0.0;
                int couches = s.getNbrCouche() != null ? s.getNbrCouche() : 0;
                double meters = longueur * couches;
                if (meters > 0) {
                    m.add(material, meters, demandRow(s.getSequence(), s.getSerie(), longueur, couches, meters));
                }
            }
        }
        return m;
    }

    /** Meters promised to production but not yet spread (Waiting series of RELEASED/STARTED sequences), by material. */
    private Map<String, Double> committedMetersByMaterial() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Object[] row : cuttingRequestSerieDataRepository.sumCommittedWaitingMetersByMaterial()) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            String material = normalizeMaterial(String.valueOf(row[0]));
            if (material == null) continue;
            out.merge(material, number(row[1]), Double::sum);
        }
        return out;
    }

    /** Row-level detail behind {@link #committedMetersByMaterial()} for the "Engagé" cell drill-down. */
    private Map<String, List<Map<String, Object>>> committedDetailByMaterial() {
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        for (Object[] row : cuttingRequestSerieDataRepository.findCommittedWaitingSeries()) {
            if (row == null || row.length < 5 || row[0] == null) continue;
            String material = normalizeMaterial(String.valueOf(row[0]));
            if (material == null) continue;
            double longueur = number(row[3]);
            int couches = (int) Math.round(number(row[4]));
            double meters = longueur * couches;
            if (meters <= 0) continue;
            out.computeIfAbsent(material, k -> new ArrayList<>())
                    .add(demandRow(value(row[1]), value(row[2]), longueur, couches, meters));
        }
        return out;
    }

    /**
     * RAW availability per normalized material: rack metrage (no reservation /
     * on-table deduction) plus the on-table in-progress estimate. This is the
     * "rack + on-going" pool the recap balances against, NOT the TRUE stock the
     * advisor uses for placement.
     */
    private Metric availableMetric() {
        Metric m = new Metric();
        for (Object[] row : scanRouleauRepository.findAllLight()) {
            if (row == null || row.length < 6) continue;
            String material = normalizeMaterial(value(row[1]));   // reftissu
            if (material == null) continue;
            double meters = number(row[5]);                       // metrage
            if (meters <= 0) meters = number(row[2]);             // quantite fallback
            if (meters <= 0) continue;
            m.add(material, meters, availableRow(value(row[0]), value(row[3]), meters, "RACK"));
        }
        List<SerieRouleauTemp> rolls = serieRouleauTempService.getAll();
        if (rolls != null) {
            for (SerieRouleauTemp roll : rolls) {
                if (roll == null) continue;
                String material = normalizeMaterial(roll.getReftissu());
                if (material == null) continue;
                double rest = roll.getEstimationRest() != null ? roll.getEstimationRest() : 0.0;
                if (rest > 0) m.add(material, rest, availableRow(roll.getIdRouleau(), "Table", rest, "ON_TABLE"));
            }
        }
        return m;
    }

    /** One contributing serie row for the "Nouveau" / "Engagé" cell drill-down. */
    private Map<String, Object> demandRow(String sequence, String serie, double longueur, int couches, double meters) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("sequence", sequence);
        r.put("serie", serie);
        r.put("longueur", round2(longueur));
        r.put("nbrCouche", couches);
        r.put("meters", round2(meters));
        return r;
    }

    /** One contributing roll row for the "Dispo" cell drill-down (RACK rack roll or ON_TABLE spreading roll). */
    private Map<String, Object> availableRow(String serialId, String location, double meters, String source) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("serialId", serialId);
        r.put("location", location);
        r.put("meters", round2(meters));
        r.put("source", source);
        return r;
    }

    /**
     * "Show how it's calculated" payload for one material's recap row: the rows
     * behind Nouveau / Engagé / Dispo, plus the Restant = Dispo − Engagé − Nouveau
     * operands. Keyed by the same names as the row's number cells so the UI can
     * look up {@code breakdown[column]} directly. Magasin is loaded separately
     * (lazily) via {@link #magasinDetail(String)}.
     */
    private Map<String, Object> buildBreakdown(Metric newM,
                                               Map<String, List<Map<String, Object>>> committedDetail,
                                               Metric availableM,
                                               String material,
                                               double available, double committed,
                                               double newMeters, double remaining) {
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("newMeters", newM.detailOf(material));
        breakdown.put("committedMeters", committedDetail.getOrDefault(material, Collections.emptyList()));
        breakdown.put("availableMeters", availableM.detailOf(material));
        Map<String, Object> remainingParts = new LinkedHashMap<>();
        remainingParts.put("availableMeters", available);
        remainingParts.put("committedMeters", committed);
        remainingParts.put("newMeters", newMeters);
        remainingParts.put("remainingMeters", remaining);
        breakdown.put("remainingMeters", remainingParts);
        return breakdown;
    }

    /**
     * Magasin stock for the deficit materials, grouped by normalized material.
     * {@link StockStatusReportService#getCurrentStock(List)} matches the R100
     * item number, which may carry the {@code P} prefix or not — so both the
     * normalized ref and {@code "P" + normalized} are passed. Rows are bucketed
     * back by {@code normalizeMaterial(getItemNumber())} — the R100 item number is
     * the material; {@code getRef()} is the roll serial.
     */
    private Map<String, List<StockStatusReport>> loadMagasinStock(Set<String> deficitMaterials) {
        Map<String, List<StockStatusReport>> out = new LinkedHashMap<>();
        if (deficitMaterials == null || deficitMaterials.isEmpty()) return out;
        List<String> refs = new ArrayList<>();
        for (String material : deficitMaterials) {
            if (material == null) continue;
            refs.add(material);
            refs.add("P" + material);
        }
        List<StockStatusReport> stock = stockStatusReportService.getCurrentStock(refs);
        if (stock == null) return out;
        for (StockStatusReport report : stock) {
            if (report == null) continue;
            String material = normalizeMaterial(report.getItemNumber());
            if (material == null || !deficitMaterials.contains(material)) continue;
            out.computeIfAbsent(material, k -> new ArrayList<>()).add(report);
        }
        return out;
    }

    private double sumMagasinMeters(List<StockStatusReport> magasinRows) {
        if (magasinRows == null) return 0.0;
        double sum = 0.0;
        for (StockStatusReport report : magasinRows) {
            if (report != null && report.getQtyOnHand() != null) sum += report.getQtyOnHand();
        }
        return sum;
    }

    /**
     * FIFO magasin pick for one material: oldest rows first (lastUpdated ascending,
     * nulls last), accumulating qtyOnHand until the deficit is covered. Output rows
     * are {@code {location, ref, qty}}.
     */
    private List<Map<String, Object>> magasinPull(List<StockStatusReport> magasinRows, double deficit) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (magasinRows == null || magasinRows.isEmpty() || deficit <= 0) return out;
        List<StockStatusReport> ordered = new ArrayList<>(magasinRows);
        ordered.sort(Comparator.comparing(StockStatusReport::getLastUpdated,
                Comparator.nullsLast(Comparator.naturalOrder())));
        double accumulated = 0.0;
        for (StockStatusReport report : ordered) {
            if (accumulated >= deficit) break;
            double qty = report.getQtyOnHand() != null ? report.getQtyOnHand() : 0.0;
            if (qty <= 0) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("location", report.getLocation());
            row.put("ref", report.getRef());
            row.put("qty", round2(qty));
            out.add(row);
            accumulated += qty;
        }
        return out;
    }

    /**
     * R100 magasin rolls behind a material's "Magasin" cell — the AVAIL2 rows
     * (id rouleau + métrage) read from {@code reportFolder.path/R100.prn} via
     * {@link StockStatusReportService#getCurrentStock(List)}. Loaded on demand
     * (the recap cell drill-down) so the file is read only when the cell is opened.
     */
    public Map<String, Object> magasinDetail(String material) {
        Map<String, Object> out = new LinkedHashMap<>();
        String norm = normalizeMaterial(material);
        out.put("refTissus", norm);
        List<Map<String, Object>> rolls = new ArrayList<>();
        double total = 0.0;
        if (norm != null) {
            List<StockStatusReport> magasinRows = loadMagasinStock(Collections.singleton(norm))
                    .getOrDefault(norm, Collections.emptyList());
            for (StockStatusReport report : magasinRows) {
                if (report == null) continue;
                double qty = report.getQtyOnHand() != null ? report.getQtyOnHand() : 0.0;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("serialId", report.getRef());
                r.put("location", report.getLocation());
                r.put("itemNumber", report.getItemNumber());
                r.put("status", report.getStatus());
                r.put("meters", round2(qty));
                rolls.add(r);
                total += qty;
            }
        }
        out.put("rolls", rolls);
        out.put("totalMeters", round2(total));
        return out;
    }

    /**
     * Stage 3 of the staged page load: {@link #magasinDetail(String)} for several
     * materials at once (the ones the overview flags as short), keyed by normalized
     * material so the UI can cache them for the cell drill-down without re-reading R100.
     */
    public Map<String, Object> magasinDetailBatch(List<String> materials) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (materials == null) return out;
        for (String material : materials) {
            if (material == null || material.trim().isEmpty()) continue;
            // Key by the caller's material string (the recap/overview refTissus) so the
            // UI can look the result up directly; magasinDetail normalizes for the R100 read.
            String key = material.trim();
            if (out.containsKey(key)) continue;
            out.put(key, magasinDetail(key));
        }
        return out;
    }

    /**
     * Flatten the recap's per-material magasin pull into a single commit picklist:
     * one {@code {refTissus, location, ref, qty}} row per roll to fetch from the
     * magasin. Only deficit materials (rack + on-table short of committed + new)
     * contribute rows.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> collectCommitMagasinPull(List<String> selected) {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> recap = recap(selected);
        Object materials = recap.get("materials");
        if (!(materials instanceof List)) return out;
        for (Object materialObj : (List<?>) materials) {
            if (!(materialObj instanceof Map)) continue;
            Map<String, Object> material = (Map<String, Object>) materialObj;
            Object pull = material.get("magasinPull");
            if (!(pull instanceof List)) continue;
            for (Object pullObj : (List<?>) pull) {
                if (!(pullObj instanceof Map)) continue;
                Map<String, Object> pullRow = new LinkedHashMap<>((Map<String, Object>) pullObj);
                pullRow.put("refTissus", material.get("refTissus"));
                out.add(pullRow);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ transfer / fill / return rules

    /**
     * RULE 1 — oversized roll → transfer-after-use. For each material, when a
     * single roll holds more meters than the material's total cross-zone need,
     * that one roll can satisfy the earliest-due zone and then move on. It is
     * assigned to the earliest-due needing zone and a {@code transferAfterUse}
     * step (to the next needing zone) is emitted on its placement; the same step
     * is stamped onto the matching roll inside that zone's serie
     * {@code rollPlacements}.
     */
    private List<Map<String, Object>> buildTransferAfterUse(DemandIndex demand, StockIndex stock,
                                                            List<SequenceAdvice> advices) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (MaterialDemand d : demand.byMaterial.values()) {
            double totalNeed = d.needed;
            if (totalNeed <= 0) continue;
            List<String> priority = d.zonesByPriority();
            if (priority.size() < 2) continue; // a transfer needs a second needing zone
            String primaryZone = priority.get(0);
            String nextZone = priority.get(1);
            for (Roll roll : stock.allRolls(d.material)) {
                if (roll.meters <= totalNeed) continue;
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("refTissus", d.material);
                step.put("serialId", roll.serialId);
                step.put("rack", roll.location);
                step.put("currentZone", roll.zone);
                step.put("useInZone", primaryZone);
                step.put("transferAfterUseTo", nextZone);
                step.put("meters", round2(roll.meters));
                step.put("totalNeed", round2(totalNeed));
                out.add(step);
                stampTransferAfterUse(advices, primaryZone, d.material, roll.serialId, nextZone);
            }
        }
        return out;
    }

    /** Stamp {@code transferAfterUse} onto the matching roll placement in the primary zone's serie rows. */
    private void stampTransferAfterUse(List<SequenceAdvice> advices, String zone, String material,
                                       String serialId, String nextZone) {
        if (serialId == null) return;
        for (SequenceAdvice advice : advices) {
            if (!zone.equals(advice.suggestedZone)) continue;
            for (Map<String, Object> serieRow : advice.series) {
                if (!material.equals(serieRow.get("refTissus"))) continue;
                Object placements = serieRow.get("rollPlacements");
                if (!(placements instanceof List)) continue;
                for (Object p : (List<?>) placements) {
                    if (p instanceof Map && serialId.equals(((Map<?, ?>) p).get("serialId"))) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> placement = (Map<String, Object>) p;
                        placement.put("transferAfterUse", nextZone);
                    }
                }
            }
        }
    }

    /**
     * RULE 2 — insufficient stock → fill ONE zone with the smallest-sufficient
     * roll set. When total TRUE stock is below the material's total need, the
     * highest-priority zone (earliest-due, then largest gap) is filled with the
     * FEWEST whole rolls whose Σ meters ≥ that zone's gap (rolls are atomic — no
     * splitting). Each chosen roll is reserved by decrementing a running stock
     * copy as we go, so a later zone sees the reduced stock — this is the
     * anti-double-counting fix. Output is a per-material zone fill plan.
     */
    private List<Map<String, Object>> buildFillPlan(DemandIndex demand, StockIndex stock) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (MaterialDemand d : demand.byMaterial.values()) {
            double total = stock.totalMeters(d.material);
            if (total >= d.needed) continue; // RULE 2 only fires on a real shortage
            // Mutable pool of remaining rolls (largest first), decremented per zone.
            List<Roll> pool = new ArrayList<>(stock.allRolls(d.material));
            pool.sort(Comparator.comparingDouble((Roll r) -> r.meters).reversed());
            List<Double> remaining = new ArrayList<>();
            List<Roll> poolRolls = new ArrayList<>();
            for (Roll r : pool) { poolRolls.add(r); remaining.add(r.meters); }

            for (String zone : d.zonesByPriority()) {
                double gap = d.neededByZone.getOrDefault(zone, 0.0);
                if (gap <= 0) continue;
                List<Map<String, Object>> chosen = new ArrayList<>();
                double covered = 0.0;
                // Smallest-sufficient: take largest rolls first → fewest rolls to clear the gap.
                for (int i = 0; i < poolRolls.size() && covered < gap; i++) {
                    double avail = remaining.get(i);
                    if (avail <= 0) continue;
                    Roll roll = poolRolls.get(i);
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("serialId", roll.serialId);
                    r.put("rack", roll.location);
                    r.put("sourceZone", roll.zone);
                    r.put("meters", round2(avail));
                    chosen.add(r);
                    covered += avail;
                    remaining.set(i, 0.0); // reserved — next zone won't see this roll
                }
                if (chosen.isEmpty()) continue;
                Map<String, Object> plan = new LinkedHashMap<>();
                plan.put("refTissus", d.material);
                plan.put("zone", zone);
                plan.put("zoneGap", round2(gap));
                plan.put("covered", round2(covered));
                plan.put("sufficient", covered >= gap);
                plan.put("rolls", chosen);
                out.add(plan);
            }
        }
        return out;
    }

    /**
     * RULE 3 — rack roll no serie needs → return to magasin. Builds the BROAD
     * in-production demand set (distinct material over {@code RELEASED, STARTED,
     * MATERIAL_MISSING} sequences whose series are not all Complete, no due-date
     * filter) and flags every rack roll whose normalized material is outside it.
     */
    private List<Map<String, Object>> buildReturnToMagasin(StockIndex stock) {
        Set<String> inProduction = new HashSet<>();
        for (String material : cuttingRequestRepository.findInProductionMaterials()) {
            String m = normalizeMaterial(material);
            if (m != null) inProduction.add(m);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (String material : stock.materials()) {
            if (inProduction.contains(material)) continue;
            for (Roll roll : stock.allRolls(material)) {
                if (roll.meters <= 0) continue;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("refTissus", material);
                r.put("serialId", roll.serialId);
                r.put("rack", roll.location);
                r.put("zone", roll.zone);
                r.put("meters", round2(roll.meters));
                out.add(r);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ advice

    private SequenceAdvice buildAdvice(SeqCarrier carrier,
                                       Map<String, LiveChargeDto.ZoneChargeDto> zonesByName,
                                       StockIndex stock,
                                       DemandIndex demand,
                                       ZoneDecision decision) {
        LiveChargeDto.SequenceDto seq = carrier.sequence;
        SequenceAdvice advice = new SequenceAdvice();
        advice.sequence = seq.getSequence();
        advice.releaseStatu = carrier.releaseStatu;
        advice.dueDate = seq.getDueDate();
        advice.dueShift = carrier.dueShift;
        advice.totalRemainingMinutes = round2(seq.getTotalRemainingMinutes());
        advice.locked = seq.isLocked();
        if (decision != null) {
            advice.recommendedZone = decision.zone;
            advice.recommendation = decision.decision;
            advice.recommendationReason = decision.reason;
        }

        String naturalZone = seq.getEffectiveZone() != null ? seq.getEffectiveZone() : carrier.ownerZone;
        Set<String> machineTypes = new LinkedHashSet<>();
        for (LiveChargeDto.SerieDto serie : safeSeries(seq)) {
            if (isComplete(serie.getStatusCoupe())) continue;
            if (serie.getMachine() != null) machineTypes.add(serie.getMachine());
        }

        resolveZone(advice, seq, naturalZone, machineTypes, zonesByName);

        // The balanced decision is authoritative for unpinned sequences:
        // commit() releases into suggestedZone, so the zone the table shows
        // must be the zone that gets committed. resolveZone contributes only
        // the physical-pin override and the no-decision fallback.
        if (!advice.pinned && decision != null && decision.zone != null) {
            advice.suggestedZone = decision.zone;
            advice.zoneSource = "BALANCED";
        }

        // Per-serie material + roll placement.
        String worst = "OK";
        boolean anyDemand = false;
        boolean anySerieSpreadable = false;
        for (LiveChargeDto.SerieDto serie : safeSeries(seq)) {
            if (isComplete(serie.getStatusCoupe())) continue;
            String material = normalizeMaterial(serie.getRefTissus());
            double needed = Math.max(0.0, serie.getTableLengthRequired());
            if (material == null || needed <= 0) continue;
            anyDemand = true;
            // Route the serie like the live charge does: a type the owner zone
            // cannot host executes in a SHARED zone — its rolls must be staged
            // at THAT zone's rack, not the owner's.
            String targetZone = routeSerieZone(advice.suggestedZone, serie.getMachine(), zonesByName);
            double inZone = stock.meters(material, targetZone);
            double total = stock.totalMeters(material);
            String status = materialStatus(needed, inZone, total);
            if (statusOrder(status) < statusOrder(worst)) worst = status;
            // TRUE-stock feasibility: a serie is spreadable if total available covers its need.
            if (total >= needed) anySerieSpreadable = true;

            demand.add(targetZone, material, needed, seq.getSequence(), serie.getSerie(), advice.dueDate);

            Map<String, Object> serieRow = new LinkedHashMap<>();
            serieRow.put("serie", serie.getSerie());
            serieRow.put("machine", serie.getMachine());
            serieRow.put("refTissus", material);
            serieRow.put("neededMeters", round2(needed));
            serieRow.put("targetZone", targetZone);
            serieRow.put("availableInZone", round2(inZone));
            serieRow.put("availableTotal", round2(total));
            serieRow.put("materialStatus", status);
            serieRow.put("rollPlacements", stock.pickRolls(material, targetZone, needed));
            advice.series.add(serieRow);
        }
        advice.materialStatus = anyDemand ? worst : "OK";
        // Advisory only (DO NOT mutate status): if the sequence has demand but no
        // serie can be fully spread from TRUE stock, suggest MATERIAL_MISSING.
        advice.materialMissingSuggested = anyDemand && !anySerieSpreadable;
        return advice;
    }

    /** Validate-and-advise the zone; a started-in-STRICT sequence is pinned to its zone. */
    private void resolveZone(SequenceAdvice advice,
                             LiveChargeDto.SequenceDto seq,
                             String naturalZone,
                             Set<String> machineTypes,
                             Map<String, LiveChargeDto.ZoneChargeDto> zonesByName) {
        // Pinned: a serie already started on a STRICT table locks the whole sequence here.
        if (seq.isLocked() && naturalZone != null) {
            advice.suggestedZone = naturalZone;
            advice.zoneSource = "PINNED_STRICT";
            advice.pinned = true;
            advice.overloadRisk = isOverloaded(naturalZone, zonesByName);
            return;
        }

        boolean naturalOverloaded = isOverloaded(naturalZone, zonesByName);
        if (naturalZone != null && !naturalOverloaded) {
            advice.suggestedZone = naturalZone;
            advice.zoneSource = "PREFERRED";
            return;
        }

        // Natural zone is overloaded (or unknown) — look for a capable zone with more headroom.
        String best = null;
        double bestHeadroom = naturalZone != null ? headroom(zonesByName.get(naturalZone)) : -1.0;
        for (LiveChargeDto.ZoneChargeDto z : zonesByName.values()) {
            if (z.getZoneNom() == null || z.getZoneNom().equals(naturalZone)) continue;
            if (!hostsAll(z, machineTypes)) continue;
            double h = headroom(z);
            if (h > bestHeadroom + REROUTE_MARGIN_MINUTES) {
                bestHeadroom = h;
                best = z.getZoneNom();
            }
        }
        if (best != null) {
            advice.suggestedZone = best;
            advice.zoneSource = naturalZone == null ? "ADVISED" : "REROUTED";
            advice.rerouted = naturalZone != null;
        } else {
            advice.suggestedZone = naturalZone;
            advice.zoneSource = naturalZone == null ? "NONE" : "PREFERRED";
            advice.overloadRisk = naturalOverloaded;
        }
    }

    private boolean hostsAll(LiveChargeDto.ZoneChargeDto zone, Set<String> machineTypes) {
        if (machineTypes.isEmpty()) return true;
        if (zone.getByMachineType() == null) return false;
        Set<String> hosted = zone.getByMachineType().stream()
                .filter(mt -> mt != null && mt.getActiveMachines() > 0 && mt.getMachineType() != null)
                .map(LiveChargeDto.MachineTypeChargeDto::getMachineType)
                .collect(Collectors.toSet());
        return hosted.containsAll(machineTypes);
    }

    /**
     * Where a serie of {@code machineType} actually executes when its sequence
     * is owned by {@code ownerZone} — mirror of {@code LiveChargeService}'s
     * per-serie routing: owner zone if it hosts the type (active machines),
     * else the first SHARED zone hosting it, else the first STRICT zone
     * hosting it, else the owner zone unchanged.
     */
    private String routeSerieZone(String ownerZone, String machineType,
                                  Map<String, LiveChargeDto.ZoneChargeDto> zonesByName) {
        if (machineType == null || machineType.trim().isEmpty()) return ownerZone;
        LiveChargeDto.ZoneChargeDto owner = ownerZone != null ? zonesByName.get(ownerZone) : null;
        if (owner != null && hostsType(owner, machineType)) return ownerZone;
        String strictFallback = null;
        for (LiveChargeDto.ZoneChargeDto z : zonesByName.values()) {
            if (z == null || z.getZoneNom() == null || z.getZoneNom().equals(ownerZone)) continue;
            if (!hostsType(z, machineType)) continue;
            if ("SHARED".equalsIgnoreCase(z.getCategory())) return z.getZoneNom();
            if (strictFallback == null) strictFallback = z.getZoneNom();
        }
        return strictFallback != null ? strictFallback : ownerZone;
    }

    private boolean hostsType(LiveChargeDto.ZoneChargeDto zone, String machineType) {
        if (zone.getByMachineType() == null) return false;
        for (LiveChargeDto.MachineTypeChargeDto mt : zone.getByMachineType()) {
            if (mt != null && mt.getActiveMachines() > 0
                    && machineType.equals(mt.getMachineType())) {
                return true;
            }
        }
        return false;
    }

    private boolean isOverloaded(String zone, Map<String, LiveChargeDto.ZoneChargeDto> zonesByName) {
        if (zone == null) return false;
        LiveChargeDto.ZoneChargeDto z = zonesByName.get(zone);
        return z != null && z.getOverallLoadPct() >= OVERLOAD_PCT;
    }

    private double headroom(LiveChargeDto.ZoneChargeDto zone) {
        if (zone == null) return 0.0;
        return Math.max(0.0, zone.getTotalCapacityMinutes() - zone.getTotalRemainingMinutes());
    }

    // ------------------------------------------------------------------ release status

    private Map<String, Set<String>> loadReleaseStatu(List<String> sequences) {
        Map<String, Set<String>> out = new LinkedHashMap<>();
        if (sequences == null || sequences.isEmpty()) return out;
        for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
            for (Object[] row : suiviPlanningRepository.findStatuBySequences(batch)) {
                if (row == null || row.length < 2 || row[0] == null) continue;
                String seq = String.valueOf(row[0]);
                String statu = row[1] != null ? String.valueOf(row[1]) : null;
                out.computeIfAbsent(seq, k -> new LinkedHashSet<>()).add(statu);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ stock

    /**
     * Builds the rack stock index and makes it <b>TRUE</b>: the raw
     * {@code ScanRouleau} meters are reduced by (a) the soft allocation ledger
     * ({@link AllocationService#reservedMetersByMaterialZone()} — meters already
     * advised/released to a zone) and (b) on-table rolls
     * ({@link SerieRouleauTemp#getEstimationRest()} — fabric physically off the
     * rack on a matelassage table). Two zones are therefore never told to use
     * the same roll, and rolls already in use on a table no longer count as
     * available stock.
     */
    private StockIndex buildStockIndex() {
        StockIndex index = new StockIndex(buildLocationToZone());
        for (Object[] row : scanRouleauRepository.findAllLight()) {
            if (row == null || row.length < 6) continue;
            String material = normalizeMaterial(value(row[1]));   // reftissu
            if (material == null) continue;
            String location = value(row[3]);                      // emplacement
            double meters = number(row[5]);                       // metrage
            if (meters <= 0) meters = number(row[2]);             // quantite fallback
            if (meters <= 0) continue;
            String zone = index.zoneForLocation(location);
            index.add(material, zone, location, value(row[0]), meters);
        }
        // Deduction (a): soft reservations already advised/released to a zone.
        index.applyReservations(allocationService.reservedMetersByMaterialZone());
        // Deduction (b): rolls physically pulled onto a matelassage table.
        index.applyOnTable(collectOnTable());
        return index;
    }

    /**
     * On-table fabric to subtract from rack availability: every in-progress
     * {@link SerieRouleauTemp} roll, as {@code (normalizedMaterial, idRouleau,
     * estimationRest)}. SerieRouleauTemp carries no zone — the zone is resolved
     * downstream from the roll's {@code idRouleau} against the rack index, and
     * when it cannot be resolved the meters are subtracted from the material
     * total only (a global deduction).
     */
    private List<Object[]> collectOnTable() {
        List<Object[]> out = new ArrayList<>();
        List<SerieRouleauTemp> rolls = serieRouleauTempService.getAll();
        if (rolls == null) return out;
        for (SerieRouleauTemp roll : rolls) {
            if (roll == null) continue;
            String material = normalizeMaterial(roll.getReftissu());
            if (material == null) continue;
            double rest = roll.getEstimationRest() != null ? roll.getEstimationRest() : 0.0;
            if (rest <= 0) continue;
            out.add(new Object[]{material, roll.getIdRouleau(), rest});
        }
        return out;
    }

    private Map<String, String> buildLocationToZone() {
        Map<String, String> out = new LinkedHashMap<>();
        List<Zone> zones = zoneRepository.findAllActive();
        if (zones == null) return out;
        for (Zone zone : zones) {
            if (zone == null || zone.getNom() == null) continue;
            for (String loc : parseLocations(zone.getRollLocations())) {
                out.put(norm(loc), zone.getNom());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ box capacity gate

    /**
     * Per-zone box (spreading-table) occupancy over the in-production sequences.
     * Mirrors {@code WorkbenchSequenceFocusService}: capacity is
     * {@code Σ activeMachines × 16} for the zone, occupancy is the box count
     * summed over the zone's {@code liveCharge} locked + pending sequences
     * (via {@link CuttingRequestBoxInfoRepository#countBoxesBySequences(List)}).
     */
    private BoxIndex buildBoxIndex(List<LiveChargeDto.ZoneChargeDto> liveZones) {
        BoxIndex index = new BoxIndex();
        Map<String, List<String>> sequencesByZone = new LinkedHashMap<>();
        List<String> allSequences = new ArrayList<>();
        for (LiveChargeDto.ZoneChargeDto z : liveZones) {
            if (z == null || z.getZoneNom() == null) continue;
            List<String> zoneSeqs = new ArrayList<>();
            collectSequenceIds(zoneSeqs, z.getLockedSequences());
            collectSequenceIds(zoneSeqs, z.getPendingSequences());
            sequencesByZone.put(z.getZoneNom(), zoneSeqs);
            allSequences.addAll(zoneSeqs);
            index.capacity.put(z.getZoneNom(), activeMachineCount(z) * BOXES_PER_MACHINE);
        }
        Map<String, Integer> boxCountBySeq = loadBoxCounts(allSequences);
        for (Map.Entry<String, List<String>> e : sequencesByZone.entrySet()) {
            int occupied = 0;
            for (String seq : e.getValue()) {
                occupied += boxCountBySeq.getOrDefault(seq, 0);
            }
            int capacity = index.capacity.getOrDefault(e.getKey(), 0);
            index.occupied.put(e.getKey(), occupied);
            index.loadByZone.put(e.getKey(), capacity > 0 ? (double) occupied / capacity : 0.0);
        }
        return index;
    }

    private void collectSequenceIds(List<String> out, List<LiveChargeDto.SequenceDto> sequences) {
        if (sequences == null) return;
        for (LiveChargeDto.SequenceDto seq : sequences) {
            if (seq != null && seq.getSequence() != null) out.add(seq.getSequence());
        }
    }

    private int activeMachineCount(LiveChargeDto.ZoneChargeDto zone) {
        if (zone == null || zone.getByMachineType() == null) return 0;
        int count = 0;
        for (LiveChargeDto.MachineTypeChargeDto mt : zone.getByMachineType()) {
            if (mt != null && mt.getActiveMachines() > 0) count += mt.getActiveMachines();
        }
        return count;
    }

    private Map<String, Integer> loadBoxCounts(List<String> sequences) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (sequences == null || sequences.isEmpty()) return out;
        for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
            for (Object[] row : boxInfoRepository.countBoxesBySequences(batch)) {
                if (row == null || row.length < 2 || row[0] == null) continue;
                String seq = String.valueOf(row[0]);
                int count = row[1] != null ? ((Number) row[1]).intValue() : 0;
                out.put(seq, count);
            }
        }
        return out;
    }

    private boolean boxFull(Map<String, Double> boxLoadByZone, String zone) {
        if (boxLoadByZone == null || zone == null) return false;
        return boxLoadByZone.getOrDefault(zone, 0.0) >= 1.0;
    }

    // ------------------------------------------------------------------ helpers

    private List<LiveChargeDto.SerieDto> safeSeries(LiveChargeDto.SequenceDto seq) {
        return seq.getSeries() == null ? Collections.emptyList() : seq.getSeries();
    }

    private boolean isComplete(String status) {
        return status != null && "Complete".equalsIgnoreCase(status.trim());
    }

    private String materialStatus(double needed, double inZone, double total) {
        if (needed <= 0) return "OK";
        if (inZone >= needed) return "OK";
        if (total >= needed) return "OUT_OF_ZONE";
        if (total <= 0) return "NONE";
        return "SHORTAGE";
    }

    private int statusOrder(String status) {
        if ("NONE".equals(status)) return 0;
        if ("SHORTAGE".equals(status)) return 1;
        if ("OUT_OF_ZONE".equals(status)) return 2;
        return 3; // OK
    }

    private List<String> parseLocations(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeMaterial(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (v.startsWith("P") && v.length() > 1) v = v.substring(1);
        return v.toUpperCase(Locale.ROOT);
    }

    private String norm(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private String value(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private double number(Object raw) {
        if (raw == null) return 0.0;
        if (raw instanceof Number) return ((Number) raw).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Object> totals(int candidates, int materialReady, int materialShort,
                                       int zonesOverloaded, int materialCount) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("candidateCount", candidates);
        t.put("materialReady", materialReady);
        t.put("materialShort", materialShort);
        t.put("zonesOverloaded", zonesOverloaded);
        t.put("materialCount", materialCount);
        return t;
    }

    // ------------------------------------------------------------------ inner types

    /**
     * A recap quantity: its per-material total (the header number) and the rows
     * behind it (the cell drill-down). Built in a single pass so the total and
     * the detail can never diverge.
     */
    private static final class Metric {
        final Map<String, Double> total = new LinkedHashMap<>();
        final Map<String, List<Map<String, Object>>> detail = new LinkedHashMap<>();

        void add(String material, double metersRaw, Map<String, Object> row) {
            total.merge(material, metersRaw, Double::sum);
            detail.computeIfAbsent(material, k -> new ArrayList<>()).add(row);
        }

        double totalOf(String material) { return total.getOrDefault(material, 0.0); }

        List<Map<String, Object>> detailOf(String material) {
            return detail.getOrDefault(material, Collections.emptyList());
        }
    }

    private static final class SeqCarrier {
        final LiveChargeDto.SequenceDto sequence;
        final String ownerZone;
        String releaseStatu;
        String dueShift;

        SeqCarrier(LiveChargeDto.SequenceDto sequence, String ownerZone) {
            this.sequence = sequence;
            this.ownerZone = ownerZone;
        }
    }

    /** Outcome of the zone-recommendation pass for one sequence. */
    private static final class ZoneDecision {
        final String zone;
        final String decision;   // ASSIGN | WAIT
        final String reason;
        ZoneDecision(String zone, String decision, String reason) {
            this.zone = zone;
            this.decision = decision;
            this.reason = reason;
        }
    }

    /** Per-zone spreading-box occupancy (occupied / capacity / load = occupied÷capacity). */
    private static final class BoxIndex {
        final Map<String, Integer> occupied = new LinkedHashMap<>();
        final Map<String, Integer> capacity = new LinkedHashMap<>();
        final Map<String, Double> loadByZone = new LinkedHashMap<>();

        int occupied(String zone)  { return occupied.getOrDefault(zone, 0); }
        int capacity(String zone)  { return capacity.getOrDefault(zone, 0); }
        double occupancyPct(String zone) {
            int cap = capacity(zone);
            return cap > 0 ? Math.round(occupied(zone) * 100.0 / cap * 100.0) / 100.0 : 0.0;
        }
    }

    private static final class SequenceAdvice {
        String sequence;
        String releaseStatu;
        LocalDate dueDate;
        double totalRemainingMinutes;
        boolean locked;
        boolean pinned;
        boolean rerouted;
        boolean overloadRisk;
        boolean materialMissingSuggested;
        String suggestedZone;
        String zoneSource;
        String recommendedZone;
        String recommendation;        // ASSIGN | WAIT
        String recommendationReason;
        String dueShift;
        String materialStatus = "OK";
        final List<Map<String, Object>> series = new ArrayList<>();

        Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("sequence", sequence);
            out.put("releaseStatu", releaseStatu);
            out.put("dueDate", dueDate != null ? dueDate.toString() : null);
            out.put("totalRemainingMinutes", totalRemainingMinutes);
            out.put("locked", locked);
            out.put("pinned", pinned);
            out.put("rerouted", rerouted);
            out.put("overloadRisk", overloadRisk);
            out.put("materialMissingSuggested", materialMissingSuggested);
            out.put("suggestedZone", suggestedZone);
            out.put("zoneSource", zoneSource);
            out.put("recommendedZone", recommendedZone);
            out.put("recommendation", recommendation);
            out.put("recommendationReason", recommendationReason);
            out.put("dueShift", dueShift);
            out.put("materialStatus", materialStatus);
            out.put("series", series);
            return out;
        }
    }

    private static final class DemandIndex {
        final Map<String, MaterialDemand> byMaterial = new LinkedHashMap<>();

        void add(String zone, String material, double needed, String sequence, String serie, LocalDate dueDate) {
            MaterialDemand d = byMaterial.computeIfAbsent(material, MaterialDemand::new);
            d.needed += needed;
            if (zone != null) {
                d.zones.add(zone);
                d.neededByZone.merge(zone, needed, Double::sum);
                if (dueDate != null) {
                    d.earliestDueByZone.merge(zone, dueDate,
                            (a, b) -> a.isBefore(b) ? a : b);
                }
            }
            if (sequence != null) d.sequences.add(sequence);
            if (serie != null) d.series.add(serie);
        }

        List<Map<String, Object>> toRows(StockIndex stock) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (MaterialDemand d : byMaterial.values()) {
                double total = stock.totalMeters(d.material);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("refTissus", d.material);
                row.put("neededMeters", Math.round(d.needed * 100.0) / 100.0);
                row.put("availableTotal", Math.round(total * 100.0) / 100.0);
                row.put("deficit", Math.round(Math.max(0, d.needed - total) * 100.0) / 100.0);
                row.put("status", total <= 0 ? "NONE" : (total >= d.needed ? "OK" : "SHORTAGE"));
                row.put("sequenceCount", d.sequences.size());
                row.put("serieCount", d.series.size());
                row.put("zones", new ArrayList<>(d.zones));
                row.put("sequences", new ArrayList<>(d.sequences));
                row.put("rolls", stock.rollRowsForMaterial(d.material));
                rows.add(row);
            }
            rows.sort(Comparator
                    .comparingInt((Map<String, Object> r) -> "OK".equals(r.get("status")) ? 1 : 0)
                    .thenComparing(r -> -((Number) r.get("deficit")).doubleValue()));
            return rows;
        }
    }

    private static final class MaterialDemand {
        final String material;
        double needed;
        final Set<String> zones = new LinkedHashSet<>();
        final Set<String> sequences = new LinkedHashSet<>();
        final Set<String> series = new LinkedHashSet<>();
        /** Need per target zone — drives the RULE 1 / RULE 2 zone priority + gaps. */
        final Map<String, Double> neededByZone = new LinkedHashMap<>();
        /** Earliest due date seen per target zone — the RULE 1 / RULE 2 priority key. */
        final Map<String, LocalDate> earliestDueByZone = new LinkedHashMap<>();

        MaterialDemand(String material) {
            this.material = material;
        }

        /** Zones needing this material, earliest-due first (largest gap breaks ties). */
        List<String> zonesByPriority() {
            List<String> zones = new ArrayList<>(neededByZone.keySet());
            zones.sort(Comparator
                    .comparing((String z) -> earliestDueByZone.get(z),
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(z -> -neededByZone.getOrDefault(z, 0.0)));
            return zones;
        }
    }

    private static final class Roll {
        final String serialId;
        final String location;
        final String zone;
        /** Mutable: shrunk as deductions (reservations / on-table) consume the roll. */
        double meters;

        Roll(String serialId, String location, String zone, double meters) {
            this.serialId = serialId;
            this.location = location;
            this.zone = zone;
            this.meters = meters;
        }
    }

    private final class StockIndex {
        private final Map<String, String> locationToZone;
        private final Map<String, Double> totalByMaterial = new LinkedHashMap<>();
        private final Map<String, Map<String, Double>> metersByZoneByMaterial = new LinkedHashMap<>();
        private final Map<String, Map<String, List<Roll>>> rollsByZoneByMaterial = new LinkedHashMap<>();
        /** serialId → zone, so an on-table roll (SerieRouleauTemp) can be placed. */
        private final Map<String, String> zoneBySerialId = new LinkedHashMap<>();

        StockIndex(Map<String, String> locationToZone) {
            this.locationToZone = locationToZone != null ? locationToZone : Collections.emptyMap();
        }

        void add(String material, String zone, String location, String serialId, double meters) {
            String z = zone != null ? zone : "UNKNOWN";
            totalByMaterial.merge(material, meters, Double::sum);
            metersByZoneByMaterial.computeIfAbsent(z, k -> new LinkedHashMap<>())
                    .merge(material, meters, Double::sum);
            rollsByZoneByMaterial.computeIfAbsent(z, k -> new LinkedHashMap<>())
                    .computeIfAbsent(material, k -> new ArrayList<>())
                    .add(new Roll(serialId, location, z, meters));
            if (serialId != null) zoneBySerialId.put(serialId, z);
        }

        /**
         * Subtract soft reservations (keyed {@code "MATERIAL|ZONE"}) from rack
         * availability — meters already advised/released to a zone are no longer
         * free. Atomic rolls in that (material, zone) bucket are shrunk largest
         * first so the roll lists stay TRUE.
         */
        void applyReservations(Map<String, Double> reservedByMaterialZone) {
            if (reservedByMaterialZone == null) return;
            for (Map.Entry<String, Double> e : reservedByMaterialZone.entrySet()) {
                String key = e.getKey();
                double reserved = e.getValue() != null ? e.getValue() : 0.0;
                if (key == null || reserved <= 0) continue;
                int sep = key.lastIndexOf('|');
                if (sep <= 0 || sep >= key.length() - 1) continue;
                String material = key.substring(0, sep);
                String zone = key.substring(sep + 1);
                deduct(material, zone, reserved);
            }
        }

        /**
         * Subtract on-table rolls ({@code [material, idRouleau, estimationRest]}).
         * The roll's zone is resolved from its {@code idRouleau} against the rack
         * index; when it cannot be resolved (the roll is no longer scanned on any
         * rack) the meters are subtracted from the material total only.
         */
        void applyOnTable(List<Object[]> onTable) {
            if (onTable == null) return;
            for (Object[] row : onTable) {
                if (row == null || row.length < 3) continue;
                String material = (String) row[0];
                String serialId = (String) row[1];
                double rest = row[2] instanceof Number ? ((Number) row[2]).doubleValue() : 0.0;
                if (material == null || rest <= 0) continue;
                String zone = serialId != null ? zoneBySerialId.get(serialId) : null;
                if (zone != null) {
                    deduct(material, zone, rest);
                } else {
                    // No rack zone resolvable — global deduction from the total only.
                    double cur = totalByMaterial.getOrDefault(material, 0.0);
                    totalByMaterial.put(material, Math.max(0.0, cur - rest));
                }
            }
        }

        /** Reduce (material, zone) availability and total by {@code amount}, shrinking atomic rolls largest-first. */
        private void deduct(String material, String zone, double amount) {
            double remaining = amount;
            double zoneAvail = meters(material, zone);
            double appliedToZone = Math.min(zoneAvail, remaining);
            if (appliedToZone > 0) {
                Map<String, Double> byMat = metersByZoneByMaterial.get(zone);
                if (byMat != null) byMat.put(material, Math.max(0.0, zoneAvail - appliedToZone));
            }
            // Shrink rolls so atomic-roll rules (RULE 1/2/3) see TRUE remaining rolls.
            List<Roll> rolls = new ArrayList<>(rolls(material, zone));
            rolls.sort(Comparator.comparingDouble((Roll r) -> r.meters).reversed());
            double toTrim = appliedToZone;
            for (Roll roll : rolls) {
                if (toTrim <= 0) break;
                double cut = Math.min(roll.meters, toTrim);
                roll.meters -= cut;
                toTrim -= cut;
            }
            dropEmptyRolls(material, zone);
            double cur = totalByMaterial.getOrDefault(material, 0.0);
            totalByMaterial.put(material, Math.max(0.0, cur - appliedToZone));
        }

        private void dropEmptyRolls(String material, String zone) {
            Map<String, List<Roll>> byMat = rollsByZoneByMaterial.get(zone);
            if (byMat == null) return;
            List<Roll> rolls = byMat.get(material);
            if (rolls != null) rolls.removeIf(r -> r.meters <= 0.0001);
        }

        String zoneForLocation(String location) {
            String key = norm(location);
            if (key.isEmpty()) return "UNKNOWN";
            String exact = locationToZone.get(key);
            if (exact != null) return exact;
            for (Map.Entry<String, String> e : locationToZone.entrySet()) {
                if (!e.getKey().isEmpty() && key.contains(e.getKey())) return e.getValue();
            }
            return "UNKNOWN";
        }

        double totalMeters(String material) {
            return totalByMaterial.getOrDefault(material, 0.0);
        }

        double meters(String material, String zone) {
            if (zone == null) return 0.0;
            Map<String, Double> m = metersByZoneByMaterial.get(zone);
            return m == null ? 0.0 : m.getOrDefault(material, 0.0);
        }

        private List<Roll> rolls(String material, String zone) {
            if (zone == null) return Collections.emptyList();
            Map<String, List<Roll>> m = rollsByZoneByMaterial.get(zone);
            if (m == null) return Collections.emptyList();
            return m.getOrDefault(material, Collections.emptyList());
        }

        /** Every (zone, roll) currently holding {@code material}, across all zones. */
        List<Roll> allRolls(String material) {
            List<Roll> out = new ArrayList<>();
            for (Map<String, List<Roll>> byMat : rollsByZoneByMaterial.values()) {
                out.addAll(byMat.getOrDefault(material, Collections.emptyList()));
            }
            return out;
        }

        /** Roll rows for {@code material} across all zones, for the materials-overview "Dispo" drill-down. */
        List<Map<String, Object>> rollRowsForMaterial(String material) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map.Entry<String, Map<String, List<Roll>>> e : rollsByZoneByMaterial.entrySet()) {
                String zone = e.getKey();
                for (Roll roll : e.getValue().getOrDefault(material, Collections.emptyList())) {
                    if (roll.meters <= 0) continue;
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("serialId", roll.serialId);
                    r.put("location", roll.location);
                    r.put("zone", zone);
                    r.put("meters", Math.round(roll.meters * 100.0) / 100.0);
                    out.add(r);
                }
            }
            return out;
        }

        /** Distinct materials that still have rack rolls. */
        Set<String> materials() {
            Set<String> out = new LinkedHashSet<>();
            for (Map<String, List<Roll>> byMat : rollsByZoneByMaterial.values()) {
                out.addAll(byMat.keySet());
            }
            return out;
        }

        /** Rolls to stage for {@code needed} meters — fill from the target zone first, then transfers. */
        List<Map<String, Object>> pickRolls(String material, String targetZone, double needed) {
            List<Map<String, Object>> out = new ArrayList<>();
            double covered = 0.0;

            List<Roll> inZone = new ArrayList<>(rolls(material, targetZone));
            inZone.sort(Comparator.comparingDouble((Roll r) -> r.meters).reversed());
            for (Roll roll : inZone) {
                if (covered >= needed || out.size() >= MAX_ROLLS_PER_SERIE) break;
                out.add(rollRow(roll, targetZone, true, null));
                covered += roll.meters;
            }

            if (covered < needed) {
                List<Object[]> elsewhere = new ArrayList<>();
                for (Map.Entry<String, Map<String, List<Roll>>> e : rollsByZoneByMaterial.entrySet()) {
                    String zone = e.getKey();
                    if (zone == null || zone.equals(targetZone)) continue;
                    for (Roll roll : e.getValue().getOrDefault(material, Collections.emptyList())) {
                        elsewhere.add(new Object[]{roll, zone});
                    }
                }
                elsewhere.sort(Comparator.comparingDouble((Object[] o) -> ((Roll) o[0]).meters).reversed());
                for (Object[] o : elsewhere) {
                    if (covered >= needed || out.size() >= MAX_ROLLS_PER_SERIE) break;
                    Roll roll = (Roll) o[0];
                    out.add(rollRow(roll, (String) o[1], false, (String) o[1]));
                    covered += roll.meters;
                }
            }
            return out;
        }

        private Map<String, Object> rollRow(Roll roll, String zone, boolean inTargetZone, String sourceZone) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("serialId", roll.serialId);
            r.put("rack", roll.location);
            r.put("zone", zone);
            r.put("meters", Math.round(roll.meters * 100.0) / 100.0);
            r.put("inTargetZone", inTargetZone);
            r.put("sourceZone", sourceZone);
            return r;
        }
    }
}
