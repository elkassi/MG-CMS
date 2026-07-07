package com.lear.MGCMS.services.dispatcher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.MachineQueue;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.MachineQueueRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.SerieRouleauTempRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;

/**
 * Read-only advisor: for every matelassage table about to go idle, recommend
 * the next Waiting/Waiting serie to mount so the downstream CNC cutter never
 * starves and CMS-Prod operators see the same top-3 contract as MG-CMS.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Load the in-production candidate series exactly the way
 *       {@link LiveChargeService} does — {@code findActiveDueOnOrBeforeLight}
 *       then a batched {@code findLiveChargeSeriesBySequences} projection
 *       (carries statusCoupe, statusMatelassage, tableCoupe, refTissus,
 *       longueur, nbrCouche). Complete series are dropped.</li>
 *   <li>Resolve each sequence's effective (lock-aware) zone with
 *       {@link LockResolver}; series only feed tables in their own effective
 *       zone, and IMPLICIT_TABLE_STRICT-locked sequences are treated as
 *       must-finish-first (a strong score boost when they can be fed here).</li>
 *   <li>For every UP table (via {@link ActiveMachineResolver}) compute
 *       <b>time-to-idle</b> blending the spreading roll runway
 *       ({@code SerieRouleauTemp.date + estimationRest}) with the cut-queue
 *       runway ({@code MachineQueue.estimatedEndTime}); the earlier wins.</li>
 *   <li>Rank candidate series per table by sequence lifecycle, same-reftissu
 *       affinity, material-on-rack, due date, completes-a-locked-sequence,
 *       and table-length fit. Only series with {@code statusCoupe=Waiting},
 *       {@code statusMatelassage=Waiting} and parent sequence status
 *       {@code RELEASED/STARTED} are eligible.</li>
 * </ol>
 *
 * <p>Mirrors LiveChargeService's performance profile: bounded projections,
 * one TimingModel batch, all aggregation in Java, 30s TTL cache.</p>
 */
@Service
public class TableFeedRankingService {

    private static final Logger log = LoggerFactory.getLogger(TableFeedRankingService.class);
    private static final int SQL_BATCH_SIZE = 2000;
    private static final long CACHE_TTL_MS = 30_000;
    private static final int DEFAULT_TOP_N = 3;

    // Ranking weights — higher score = better candidate to mount next.
    //
    // BANDED (lexicographic) priority: each tier sits in its own decimal band with
    // a gap wide enough that no sum of strictly-lower tiers can overtake a higher
    // tier. This makes the additive `score` behave like a strict priority ladder
    // (a soft preference can NEVER override a hard one) while staying a single
    // comparable number for display. Verified non-leak with these values:
    //   max(TierC+D+E+F+G) = 10000+540+100+10+1 = 10651 < 100000 (TierB)
    //   max(TierD+E+F+G)   =        540+100+10+1 =   651 <   5000 (TierC, W_DUE_TODAY)
    //   max(TierE+F+G)     =            100+10+1 =   111 <    500 (TierD, W_AGE_FLOOR)
    // Tier B  — WIP: finish work that is already open / locked-to-finish.
    private static final double W_SEQUENCE_STARTED      = 100000.0; // finish already opened sequence first
    private static final double W_COMPLETES_LOCKED      = 100000.0; // must-finish-first locked sequence
    // Tier C  — date pressure (exclusive: at most one of the two applies).
    private static final double W_DUE_OVERDUE           = 10000.0;  // dueDate strictly before today
    private static final double W_DUE_TODAY             = 5000.0;   // dueDate is today
    // Tier D  — anti-starvation / FIFO wait-age: a fairness floor lifting a long-waiting
    // sequence above affinity (but never above date pressure) so it cannot starve forever.
    private static final double W_AGE_FLOOR             = 500.0;    // applied once age >= threshold
    private static final double W_AGE_RAMP_PER_HOUR     = 0.1;      // + per hour waited beyond threshold
    private static final double W_AGE_RAMP_CAP          = 40.0;     // cap on the ramp (Tier D max = 540 < TierC)
    private static final long   AGE_THRESHOLD_HOURS     = 24L;      // only "aged" past this many hours
    // Tier E  — affinity: avoid a heavy roll change.
    private static final double W_SAME_REFTISSU         = 100.0;    // same reftissu already mounted
    // Tier F  — roll already on a rack in this zone.
    private static final double W_MATERIAL_IN_ZONE      = 10.0;     // material on rack in target zone
    private static final double P_MATERIAL_NOT_IN_ZONE  = -5.0;     // soft demotion: roll NOT on a rack here
    private static final double P_TARGET_ZONE_OVERLOADED = -50.0;   // target zone >= 100% loaded
    private static final double W_STARVING_ZONE         = 30.0;     // target zone < 30% loaded
    // Tier G  — keep the cutter busy: tiny bonus proportional to validated minutes,
    // capped so it can never reach Tier F (max contribution = 100 * 0.01 = 1.0).
    private static final double W_BUSY_PER_MINUTE       = 0.01;     // x min(validated, 100)
    private static final double W_BUSY_MINUTES_CAP      = 100.0;    // cap on the minutes that count
    private static final double P_DOES_NOT_FIT          = -1000.0;  // cannot physically mount → bury it (internal ordering only)

    @Autowired private CuttingRequestRepository cuttingRequestRepository;
    @Autowired private CuttingRequestSerieDataRepository serieDataRepository;
    @Autowired private ProductionTableRepository productionTableRepository;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private SerieRouleauTempRepository serieRouleauTempRepository;
    @Autowired private MachineQueueRepository machineQueueRepository;
    @Autowired private CuttingTimeCalculator cuttingTimeCalculator;
    @Autowired private ActiveMachineResolver activeMachineResolver;
    @Autowired private MaterialAvailabilityChecker materialAvailabilityChecker;
    @Autowired private ZoneLoadService zoneLoadService;

    /** Kill-switch for the Tier-D wait-age signal (mirrors mgcms.sequence.rectify.enabled).
     *  Package-private + default true so unit tests (no Spring) still exercise Tier D. */
    @Value("${mgcms.nextserie.waitage.enabled:true}")
    boolean waitAgeEnabled = true;

    // Result cache keyed by (date|shift|topN).
    private final Map<String, TableFeedDto> cache = new HashMap<>();
    private final Map<String, Long> cacheTimestamps = new HashMap<>();

    @Transactional(readOnly = true)
    public synchronized TableFeedDto compute(LocalDate date, int shift, int topN) {
        long startMs = System.currentTimeMillis();
        int effTopN = topN > 0 ? topN : DEFAULT_TOP_N;
        String cacheKey = date + "|" + shift + "|" + effTopN;
        Long cachedAt = cacheTimestamps.get(cacheKey);
        if (cachedAt != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            TableFeedDto hit = cache.get(cacheKey);
            if (hit != null) return hit;
        }

        LocalDateTime now = LocalDateTime.now();
        // Fresh material reads for this window — same hygiene the optimizer uses.
        materialAvailabilityChecker.clearSnapshotCache();

        // 1. Active in-production sequences due now-or-earlier (same loader as LiveCharge).
        List<Object[]> requestRows = cuttingRequestRepository.findActiveDueOnOrBeforeLight(
                date, String.valueOf(shift));
        Map<String, RequestLight> requestBySeq = new LinkedHashMap<>();
        for (Object[] r : requestRows) {
            String sequence = (String) r[0];
            if (sequence == null) continue;
            LocalDate dueDate = r.length > 5 && r[5] instanceof LocalDate ? (LocalDate) r[5] : null;
            requestBySeq.put(sequence, new RequestLight(
                    sequence,
                    (String) r[1],                              // dispatchedZone
                    (String) r[2],                              // zoneAcceptanceStatus
                    dueDate,
                    r.length > 7 ? (String) r[7] : null,        // releaseZone
                    (String) r[4],                               // preferred zone.nom
                    r.length > 8 ? (String) r[8] : null));       // sequenceStatus
        }

        // 1b. Release-age proxy (dispatchedAt, else createdAt) for the Tier-D wait-age signal.
        // Bounded to the candidate sequences and isolated from the 8-consumer candidate query.
        Map<String, LocalDateTime> releaseProxyBySeq = new HashMap<>();
        if (waitAgeEnabled && !requestBySeq.isEmpty()) {
            List<String> seqList = new ArrayList<>(requestBySeq.keySet());
            for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
                List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
                for (Object[] row : cuttingRequestRepository.findReleaseProxyBySequences(batch)) {
                    if (row[0] == null) continue;
                    LocalDateTime proxy = row[1] instanceof LocalDateTime ? (LocalDateTime) row[1]
                            : (row[2] instanceof LocalDateTime ? (LocalDateTime) row[2] : null);
                    if (proxy != null) releaseProxyBySeq.put((String) row[0], proxy);
                }
            }
        }

        // 2. Active zones + machine-type roster per zone (batch).
        List<Zone> activeZones = zoneRepository.findAllActive();
        Map<String, Zone> zoneByNom = new HashMap<>();
        List<String> zoneNoms = new ArrayList<>(activeZones.size());
        for (Zone z : activeZones) {
            zoneByNom.put(z.getNom(), z);
            zoneNoms.add(z.getNom());
        }
        Map<String, Map<String, Set<String>>> machinesByZoneByType = new HashMap<>();
        if (!zoneNoms.isEmpty()) {
            for (Object[] row : productionTableRepository.findMachinesWithTypeInZones(zoneNoms)) {
                String zoneNom = (String) row[0];
                String machineNom = (String) row[1];
                String typeName = (String) row[2];
                if (typeName == null) continue;
                machinesByZoneByType
                        .computeIfAbsent(zoneNom, k -> new HashMap<>())
                        .computeIfAbsent(typeName, t -> new LinkedHashSet<>())
                        .add(machineNom);
            }
        }
        for (String zn : zoneNoms) {
            machinesByZoneByType.computeIfAbsent(zn, k -> new HashMap<>());
        }
        // SHARED-first routing order, matching LiveChargeService.resolveTargetZone.
        List<String> zoneRoutingOrder = new ArrayList<>();
        for (Zone z : activeZones) if (z.getCategory() == Zone.Category.SHARED) zoneRoutingOrder.add(z.getNom());
        for (Zone z : activeZones) if (z.getCategory() != Zone.Category.SHARED) zoneRoutingOrder.add(z.getNom());

        // 3. Series projection (batched) — only for candidate sequences.
        List<String> sequences = new ArrayList<>(requestBySeq.keySet());
        List<Object[]> serieRows = new ArrayList<>();
        for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
            serieRows.addAll(serieDataRepository.findLiveChargeSeriesBySequences(batch));
        }
        Map<String, List<RawSerie>> seriesBySeq = new LinkedHashMap<>();
        Set<String> tableNoms = new LinkedHashSet<>();
        for (Object[] sr : serieRows) {
            RawSerie row = RawSerie.from(sr);
            seriesBySeq.computeIfAbsent(row.sequence, k -> new ArrayList<>()).add(row);
            if (row.tableCoupe != null && !row.tableCoupe.trim().isEmpty()) tableNoms.add(row.tableCoupe);
        }

        // 4. Table → zone/category map for lock resolution (batch).
        Map<String, LockResolver.TableZoneInfo> tableToZone = loadTableZoneMap(tableNoms);

        // 5. TimingModel batch (one round-trip), then resolve per serie.
        List<String> placements = new ArrayList<>();
        for (List<RawSerie> list : seriesBySeq.values()) {
            for (RawSerie r : list) if (r.placement != null) placements.add(r.placement);
        }
        Map<String, CuttingTimeCalculator.TimingRow> timingMap = cuttingTimeCalculator.loadTimingMap(placements);

        // 6. Build the candidate pool: non-Complete series with their effective zone.
        List<Candidate> candidates = new ArrayList<>();
        for (Map.Entry<String, List<RawSerie>> entry : seriesBySeq.entrySet()) {
            String sequence = entry.getKey();
            RequestLight req = requestBySeq.get(sequence);
            if (req == null) continue;
            if (!isFeedableSequenceStatus(req.sequenceStatus)) continue;
            if ("REJECTED".equalsIgnoreCase(req.zoneAcceptanceStatus)) continue;

            List<RawSerie> rawSeries = entry.getValue();
            List<LockResolver.SerieLockInput> lockInputs = new ArrayList<>(rawSeries.size());
            for (RawSerie r : rawSeries) {
                lockInputs.add(new LockResolver.SerieLockInput(
                        r.serie, r.statusCoupe, r.tableCoupe, r.dateDebutCoupe));
            }
            Optional<LockResolver.LockResult> lockOpt = LockResolver.resolve(
                    req.dispatchedZone, req.zoneAcceptanceStatus, lockInputs, tableToZone);

            boolean locked = lockOpt.isPresent();
            boolean implicitStrict = locked
                    && lockOpt.get().getReason() == LockResolver.LockReason.IMPLICIT_TABLE_STRICT;
            String effectiveZone;
            if (locked) {
                effectiveZone = lockOpt.get().getLockZoneNom();
            } else if (req.releaseZone != null) {
                effectiveZone = req.releaseZone;
            } else if (req.dispatchedZone != null) {
                effectiveZone = req.dispatchedZone;
            } else {
                effectiveZone = req.preferredZoneNom;
            }

            for (RawSerie r : rawSeries) {
                if (!isWaiting(r.statusCoupe) || !isWaiting(r.statusMatelassage)) {
                    continue;
                }
                // Route this serie's load to the zone that physically hosts its
                // machine type, exactly like LiveChargeService does.
                String targetZone = LiveChargeService.resolveTargetZone(
                        effectiveZone, r.machine, machinesByZoneByType, zoneRoutingOrder);
                if (targetZone == null) continue;
                double validated = cuttingTimeCalculator.resolve(
                        r.placement, r.tempsDeCoupe, r.nbrCouche, r.machine, timingMap).minutes;
                double requiredLength = requiredFabricMeters(r.longueur, r.nbrCouche);
                candidates.add(new Candidate(
                        r.serie, r.sequence, r.refTissus, r.machine, targetZone,
                        req.sequenceStatus, r.statusCoupe, r.statusMatelassage,
                        implicitStrict, r.longueur, r.nbrCouche, req.dueDate,
                        validated, requiredLength, releaseProxyBySeq.get(r.sequence)));
            }
        }

        // Group candidates by their target zone for quick per-table lookup.
        Map<String, List<Candidate>> candidatesByZone = new HashMap<>();
        for (Candidate c : candidates) {
            candidatesByZone.computeIfAbsent(c.targetZone, k -> new ArrayList<>()).add(c);
        }

        // 7. Occupancy probes: mounted roll per table + cut-queue end per table.
        Map<String, SerieRouleauTemp> mountedByTable = new HashMap<>();
        for (SerieRouleauTemp t : serieRouleauTempRepository.findAll()) {
            if (t.getTableMatelassage() != null) mountedByTable.put(t.getTableMatelassage(), t);
        }
        Map<String, LocalDateTime> cutQueueEndByMachine = new HashMap<>();
        for (MachineQueue mq : machineQueueRepository.findAllOrdered()) {
            if (mq.getMachineNom() == null || mq.getEstimatedEndTime() == null) continue;
            cutQueueEndByMachine.merge(mq.getMachineNom(), mq.getEstimatedEndTime(),
                    (a, b) -> b.isAfter(a) ? b : a);
        }

        // 8. Material availability per (zone, refTissu) for the candidate refs.
        Map<String, Set<String>> refsByZone = new HashMap<>();
        for (Candidate c : candidates) {
            if (c.refTissus != null && !c.refTissus.trim().isEmpty()) {
                refsByZone.computeIfAbsent(c.targetZone, k -> new HashSet<>()).add(c.refTissus.trim());
            }
        }
        Map<String, Map<String, MaterialAvailabilityChecker.MaterialStatus>> materialByZone = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : refsByZone.entrySet()) {
            materialByZone.put(e.getKey(), materialAvailabilityChecker.check(e.getValue(), e.getKey()));
        }

        // 9. Walk active zones → UP tables → time-to-idle + ranked candidates.
        List<ProductionTable> allTables = productionTableRepository.findAll();
        Map<String, List<ProductionTable>> tablesByZone = new HashMap<>();
        for (ProductionTable pt : allTables) {
            if (pt.getZone() == null || pt.getZone().getNom() == null) continue;
            tablesByZone.computeIfAbsent(pt.getZone().getNom(), k -> new ArrayList<>()).add(pt);
        }

        ZoneLoadDto loadDto = zoneLoadService.computeMatrix(date, shift);
        Map<String, Double> zoneLoadByNom = new HashMap<>();
        if (loadDto != null && loadDto.getRows() != null) {
            for (ZoneLoadDto.ZoneLoadRowDto z : loadDto.getRows()) {
                if (z != null && z.getZoneNom() != null) {
                    zoneLoadByNom.put(z.getZoneNom(), z.getLoadPct());
                }
            }
        }

        List<Zone> orderedZones = orderZones(activeZones);
        List<TableFeedDto.ZoneFeedDto> zoneDtos = new ArrayList<>();
        for (Zone zone : orderedZones) {
            String zoneNom = zone.getNom();
            Set<String> upInZone = activeMachineResolver.activeMachines(date, shift, zoneNom);
            List<ProductionTable> tables = tablesByZone.getOrDefault(zoneNom, Collections.emptyList());
            List<TableFeedDto.TableRowDto> tableRows = new ArrayList<>();
            for (ProductionTable pt : tables) {
                String tableNom = pt.getNom();
                if (tableNom == null) continue;
                // A table is feedable when shift-confirmation marked it up OR it is
                // physically in use right now (a roll mounted on its spreading table,
                // or work queued on its cutter). The second case keeps an operator who
                // is actively working from seeing zero "next serie" recommendations when
                // the shift was never formally confirmed — the common floor reality.
                boolean inUse = mountedByTable.containsKey(tableNom)
                        || cutQueueEndByMachine.containsKey(tableNom);
                if (!upInZone.contains(tableNom) && !inUse) continue;

                SerieRouleauTemp mounted = mountedByTable.get(tableNom);
                LocalDateTime spreadingIdleAt = null;
                String mountedRef = null;
                if (mounted != null) {
                    mountedRef = mounted.getReftissu();
                    if (mounted.getDate() != null && mounted.getEstimationRest() != null) {
                        spreadingIdleAt = mounted.getDate()
                                .plusMinutes(Math.round(Math.max(0.0, mounted.getEstimationRest())));
                    }
                }
                LocalDateTime cutQueueIdleAt = cutQueueEndByMachine.get(tableNom);

                LocalDateTime idleAt = earliest(spreadingIdleAt, cutQueueIdleAt);
                boolean idleNow = idleAt == null;
                double timeToIdle = idleAt == null ? 0.0
                        : Math.max(0.0, Duration.between(now, idleAt).toMillis() / 60000.0);

                List<TableFeedDto.CandidateDto> ranked = rankForTable(
                        pt, mountedRef, candidatesByZone.getOrDefault(zoneNom, Collections.emptyList()),
                        materialByZone.getOrDefault(zoneNom, Collections.emptyMap()), zoneLoadByNom, effTopN);

                tableRows.add(new TableFeedDto.TableRowDto(
                        tableNom,
                        pt.getMachineType() != null ? pt.getMachineType().getName() : null,
                        pt.getTableLength(), mountedRef, spreadingIdleAt, cutQueueIdleAt,
                        round2(timeToIdle), idleNow, ranked));
            }
            // Soonest-to-idle first so the chef sees the most urgent tables up top.
            tableRows.sort((a, b) -> Double.compare(a.getTimeToIdleMinutes(), b.getTimeToIdleMinutes()));
            if (!tableRows.isEmpty()) {
                zoneDtos.add(new TableFeedDto.ZoneFeedDto(
                        zoneNom,
                        zone.getCategory() != null ? zone.getCategory().name() : "STRICT",
                        tableRows));
            }
        }

        TableFeedDto result = new TableFeedDto(now, date, shift, effTopN, zoneDtos);
        cache.put(cacheKey, result);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        long elapsedMs = System.currentTimeMillis() - startMs;
        if (elapsedMs > 500) {
            log.warn("TableFeedRankingService.compute() took {} ms — sequences={}, zones={}",
                    elapsedMs, requestBySeq.size(), zoneDtos.size());
        } else {
            log.debug("TableFeedRankingService.compute() took {} ms", elapsedMs);
        }
        return result;
    }

    /**
     * Public/CMS-Prod contract: best Waiting/Waiting series for one physical
     * table. This reuses the same per-table ranking rendered by
     * {@code /processWorkbench} and {@code /tableFeed}.
     */
    @Transactional(readOnly = true)
    public List<TableFeedDto.CandidateDto> recommendForMachine(LocalDate date, int shift,
                                                               String machineNom, int topN) {
        if (machineNom == null || machineNom.trim().isEmpty()) return Collections.emptyList();
        int effTopN = topN > 0 ? topN : DEFAULT_TOP_N;
        TableFeedDto dto = compute(date, shift, effTopN);
        String target = machineNom.trim();
        for (TableFeedDto.ZoneFeedDto zone : dto.getZones()) {
            for (TableFeedDto.TableRowDto table : zone.getTables()) {
                if (table.getTableNom() != null && table.getTableNom().equalsIgnoreCase(target)) {
                    List<TableFeedDto.CandidateDto> candidates = table.getCandidates();
                    if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
                    return candidates.size() > effTopN
                            ? new ArrayList<>(candidates.subList(0, effTopN))
                            : new ArrayList<>(candidates);
                }
            }
        }
        return Collections.emptyList();
    }

    // ----------------------------------------------------------------- ranking

    List<TableFeedDto.CandidateDto> rankForTable(
            ProductionTable table, String mountedRef, List<Candidate> pool,
            Map<String, MaterialAvailabilityChecker.MaterialStatus> material, 
            Map<String, Double> zoneLoadByNom, int topN) {
        // ponytail: no wait-age tier — dueDate is the only starvation proxy. The projection
        // (findLiveChargeSeriesBySequences) carries no release/STARTED timestamp, so a serie
        // that keeps losing on affinity can starve indefinitely without ever tripping its due
        // date. Upgrade path: add a release/STARTED timestamp to that projection + Candidate and
        // insert a true FIFO age tier (between Tier C and Tier E) if starvation is observed.

        Double tableLength = table.getTableLength();
        String tableType = table.getMachineType() != null ? table.getMachineType().getName() : null;

        List<TableFeedDto.CandidateDto> scored = new ArrayList<>();
        for (Candidate c : pool) {
            // Only feed series whose machine type matches this table's type.
            // Compare trimmed/case-insensitively: ProductionTable.machineType.name and
            // CuttingRequestSerie.machine are free-typed SQL Server strings, so a stray
            // trailing space or casing difference must not silently drop every candidate.
            // A null/blank serie machine must NOT bypass the gate (sameMachineType returns
            // false for it) — otherwise the serie is offered to every table type in the zone.
            if (tableType != null && !sameMachineType(tableType, c.machine)) continue;

            List<String> reasons = new ArrayList<>();
            // Banded score: each tier lands in its own decimal band so a lower tier can
            // never overtake a higher one (see the W_* constants for the non-leak proof).
            double score = 0.0;

            // Tier B — finish work that is already open or locked-to-finish.
            if ("STARTED".equalsIgnoreCase(c.sequenceStatus)) {
                score += W_SEQUENCE_STARTED;
                reasons.add("Séquence déjà ouverte à finir (+" + fmt(W_SEQUENCE_STARTED) + ")");
            }
            if (c.completesLockedSequence) {
                score += W_COMPLETES_LOCKED;
                reasons.add("Termine une séquence verrouillée (table STRICT) (+"
                        + fmt(W_COMPLETES_LOCKED) + ")");
            }

            // Tier C — date pressure (exclusive: overdue OR due-today, not both).
            if (c.dueDate != null) {
                if (c.dueDate.isBefore(LocalDate.now())) {
                    score += W_DUE_OVERDUE;
                    reasons.add("En retard (due " + c.dueDate + ") (+" + fmt(W_DUE_OVERDUE) + ")");
                } else if (c.dueDate.isEqual(LocalDate.now())) {
                    score += W_DUE_TODAY;
                    reasons.add("Due aujourd'hui (+" + fmt(W_DUE_TODAY) + ")");
                }
            }

            // Tier D — anti-starvation / FIFO wait-age. Below date pressure, above affinity:
            // a sequence waiting past the threshold gets a floor + a small per-hour ramp so it
            // cannot be starved indefinitely by same-ref / material competitors.
            if (waitAgeEnabled && c.releaseProxyAt != null) {
                long ageHours = Duration.between(c.releaseProxyAt, LocalDateTime.now()).toHours();
                if (ageHours >= AGE_THRESHOLD_HOURS) {
                    double ramp = Math.min((double) ageHours, W_AGE_RAMP_CAP / W_AGE_RAMP_PER_HOUR)
                            * W_AGE_RAMP_PER_HOUR;
                    score += W_AGE_FLOOR + ramp;
                    reasons.add("En attente depuis " + ageHours + "h (+" + round2(W_AGE_FLOOR + ramp) + ")");
                }
            }

            // Tier E — affinity: same reftissu already on the table (no roll change).
            boolean sameRef = mountedRef != null && c.refTissus != null
                    && mountedRef.trim().equalsIgnoreCase(c.refTissus.trim());
            if (sameRef) {
                score += W_SAME_REFTISSU;
                reasons.add("Même réf. tissu déjà montée (pas de changement de rouleau) (+"
                        + fmt(W_SAME_REFTISSU) + ")");
            }

            // Tier F — material already on a rack in the target zone.
            String materialKey = normalizeMaterial(c.refTissus);
            MaterialAvailabilityChecker.MaterialStatus matStatus = material.get(materialKey);
            boolean matInZone = matStatus == MaterialAvailabilityChecker.MaterialStatus.AVAILABLE_IN_ZONE;
            if (matInZone) {
                score += W_MATERIAL_IN_ZONE;
                reasons.add("Matière dans la zone (" + fmt(W_MATERIAL_IN_ZONE) + ")");
            } else {
                score += P_MATERIAL_NOT_IN_ZONE;
                reasons.add("Matière hors zone (" + fmt(P_MATERIAL_NOT_IN_ZONE) + ")");
            }

            if (c.targetZone != null) {
                double load = zoneLoadByNom.getOrDefault(c.targetZone, 50.0);
                if (load >= 100.0) {
                    score += P_TARGET_ZONE_OVERLOADED;
                    reasons.add("Zone cible saturée (" + fmt(P_TARGET_ZONE_OVERLOADED) + ")");
                } else if (load < 30.0) {
                    score += W_STARVING_ZONE;
                    reasons.add("Zone cible sous-chargée (+" + fmt(W_STARVING_ZONE) + ")");
                }
            }

            // Tier G — keep the cutter busy: tiny bonus, capped below Tier F.
            double busyBonus = Math.min(Math.max(0.0, c.validated), W_BUSY_MINUTES_CAP) * W_BUSY_PER_MINUTE;
            if (busyBonus > 0.0) {
                score += busyBonus;
                reasons.add("Charge le coupeur (+" + round2(busyBonus) + ")");
            }

            // Fit — large-negative for internal ordering only; non-fitting candidates
            // are excluded from the returned top-N below so an impossible mount never
            // surfaces as a recommendation.
            double layLength = layLengthMeters(c.longueur);
            boolean fits = fitsTableLength(tableLength, c.longueur);
            if (!fits) {
                score += P_DOES_NOT_FIT;
                reasons.add("Ne tient pas sur la table ("
                        + round2(layLength) + " m > " + tableLength + " m)");
            } else if (tableLength != null && layLength > 0.0) {
                reasons.add("Tient sur la table (" + round2(layLength) + " m)");
            }

            scored.add(new TableFeedDto.CandidateDto(
                    c.serie, c.sequence, c.refTissus, c.machine, c.targetZone,
                    c.statusCoupe, c.statusMatelassage, c.sequenceStatus, false,
                    sameRef, matInZone, c.completesLockedSequence, fits,
                    c.longueur, c.nbrCouche, round2(c.requiredLength), c.dueDate, round2(c.validated),
                    round2(score), reasons));
        }

        // Best score first; tie-break on earliest due date (nulls last), then LONGER
        // validated minutes. The validated comparison is DESCENDING (flipped from the old
        // ascending) so that, all else equal, we mount the longer cut and keep the cutter
        // loaded rather than feeding it a short job it will burn through immediately.
        scored.sort((a, b) -> {
            int s = Double.compare(b.getScore(), a.getScore());
            if (s != 0) return s;
            LocalDate da = a.getDueDate();
            LocalDate db = b.getDueDate();
            if (da != null && db != null && !da.isEqual(db)) return da.compareTo(db);
            if (da == null && db != null) return 1;
            if (da != null && db == null) return -1;
            return Double.compare(b.getValidatedMinutes(), a.getValidatedMinutes());
        });
        // Exclude physically-impossible mounts: never surface a non-fitting serie as a
        // recommendation. If every candidate is non-fit, return an empty list.
        List<TableFeedDto.CandidateDto> fitting = new ArrayList<>(scored.size());
        for (TableFeedDto.CandidateDto c : scored) {
            if (c.isFitsTableLength()) fitting.add(c);
        }
        return fitting.size() > topN ? new ArrayList<>(fitting.subList(0, topN)) : fitting;
    }

    /** Render a band weight without a trailing ".0" so reason strings read cleanly. */
    private static String fmt(double v) {
        if (v == Math.rint(v) && !Double.isInfinite(v)) return Long.toString((long) v);
        return Double.toString(v);
    }

    // ----------------------------------------------------------------- helpers

    private List<Zone> orderZones(List<Zone> activeZones) {
        List<Zone> ordered = new ArrayList<>(activeZones);
        ordered.sort((a, b) -> {
            boolean aShared = a.getCategory() == Zone.Category.SHARED;
            boolean bShared = b.getCategory() == Zone.Category.SHARED;
            if (aShared != bShared) return aShared ? 1 : -1;
            return a.getNom().compareToIgnoreCase(b.getNom());
        });
        return ordered;
    }

    private Map<String, LockResolver.TableZoneInfo> loadTableZoneMap(Set<String> tableNoms) {
        if (tableNoms.isEmpty()) return Collections.emptyMap();
        List<String> list = new ArrayList<>(tableNoms);
        Map<String, LockResolver.TableZoneInfo> out = new HashMap<>(list.size() * 2);
        for (int i = 0; i < list.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = list.subList(i, Math.min(i + SQL_BATCH_SIZE, list.size()));
            for (Object[] row : productionTableRepository.findZoneInfoByTableNoms(batch)) {
                String tableNom = (String) row[0];
                String zoneNom = (String) row[1];
                Object catObj = row[2];
                Zone.Category category = catObj instanceof Zone.Category
                        ? (Zone.Category) catObj
                        : Zone.Category.valueOf(String.valueOf(catObj));
                out.put(tableNom, new LockResolver.TableZoneInfo(zoneNom, category));
            }
        }
        return out;
    }

    private static LocalDateTime earliest(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    static double requiredFabricMeters(Double longueur, Integer nbrCouche) {
        return longueur != null && nbrCouche != null ? longueur * nbrCouche : 0.0;
    }

    static double layLengthMeters(Double longueur) {
        return longueur != null ? longueur : 0.0;
    }

    static boolean fitsTableLength(Double tableLength, Double longueur) {
        double layLength = layLengthMeters(longueur);
        return tableLength == null || layLength <= 0.0 || layLength <= tableLength;
    }

    private static boolean isWaiting(String status) {
        return "Waiting".equalsIgnoreCase(status == null ? "" : status.trim());
    }

    /** Trim + case-insensitive machine-type equality (defensive against dirty strings). */
    private static boolean sameMachineType(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static boolean isFeedableSequenceStatus(String sequenceStatus) {
        return "RELEASED".equalsIgnoreCase(sequenceStatus == null ? "" : sequenceStatus.trim())
                || "STARTED".equalsIgnoreCase(sequenceStatus == null ? "" : sequenceStatus.trim());
    }

    private static String normalizeMaterial(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (v.startsWith("P") && v.length() > 1) {
            v = v.substring(1);
        }
        return v.toUpperCase();
    }

    // ----------------------------------------------------------------- carriers

    private static final class RequestLight {
        final String sequence;
        final String dispatchedZone;
        final String zoneAcceptanceStatus;
        final LocalDate dueDate;
        final String releaseZone;
        final String preferredZoneNom;
        final String sequenceStatus;
        RequestLight(String sequence, String dispatchedZone, String zoneAcceptanceStatus,
                     LocalDate dueDate, String releaseZone, String preferredZoneNom,
                     String sequenceStatus) {
            this.sequence = sequence;
            this.dispatchedZone = dispatchedZone;
            this.zoneAcceptanceStatus = zoneAcceptanceStatus;
            this.dueDate = dueDate;
            this.releaseZone = releaseZone;
            this.preferredZoneNom = preferredZoneNom;
            this.sequenceStatus = sequenceStatus;
        }
    }

    /** Mirrors {@code findLiveChargeSeriesBySequences} column order (16 cols). */
    private static final class RawSerie {
        final String serie;
        final String sequence;
        final String machine;
        final Double tempsDeCoupe;
        final Integer nbrCouche;
        final String placement;
        final String statusCoupe;
        final String statusMatelassage;
        final String tableCoupe;
        final LocalDateTime dateDebutCoupe;
        final String refTissus;
        final Double longueur;

        RawSerie(String serie, String sequence, String machine, Double tempsDeCoupe,
                 Integer nbrCouche, String placement, String statusCoupe,
                 String statusMatelassage, String tableCoupe, LocalDateTime dateDebutCoupe,
                 String refTissus, Double longueur) {
            this.serie = serie;
            this.sequence = sequence;
            this.machine = machine;
            this.tempsDeCoupe = tempsDeCoupe;
            this.nbrCouche = nbrCouche;
            this.placement = placement;
            this.statusCoupe = statusCoupe;
            this.statusMatelassage = statusMatelassage;
            this.tableCoupe = tableCoupe;
            this.dateDebutCoupe = dateDebutCoupe;
            this.refTissus = refTissus;
            this.longueur = longueur;
        }

        static RawSerie from(Object[] sr) {
            return new RawSerie(
                    (String) sr[0],
                    (String) sr[1],
                    (String) sr[2],
                    sr[3] != null ? ((Number) sr[3]).doubleValue() : null,
                    sr[4] != null ? ((Number) sr[4]).intValue() : null,
                    (String) sr[5],
                    (String) sr[6],
                    (String) sr[7],
                    (String) sr[8],
                    (LocalDateTime) sr[10],
                    sr.length > 14 ? (String) sr[14] : null,
                    sr.length > 15 && sr[15] != null ? ((Number) sr[15]).doubleValue() : null);
        }
    }

    /** A feedable serie with its resolved target zone and pre-computed signals. */
    static final class Candidate {
        final String serie;
        final String sequence;
        final String refTissus;
        final String machine;
        final String targetZone;
        final String sequenceStatus;
        final String statusCoupe;
        final String statusMatelassage;
        final boolean completesLockedSequence;
        final Double longueur;
        final Integer nbrCouche;
        final LocalDate dueDate;
        final double validated;
        final double requiredLength;
        final LocalDateTime releaseProxyAt; // dispatchedAt ?? createdAt — Tier-D wait-age source

        Candidate(String serie, String sequence, String refTissus, String machine,
                  String targetZone, String sequenceStatus, String statusCoupe,
                  String statusMatelassage, boolean completesLockedSequence,
                  Double longueur, Integer nbrCouche, LocalDate dueDate,
                  double validated, double requiredLength, LocalDateTime releaseProxyAt) {
            this.serie = serie;
            this.sequence = sequence;
            this.refTissus = refTissus;
            this.machine = machine;
            this.targetZone = targetZone;
            this.sequenceStatus = sequenceStatus;
            this.statusCoupe = statusCoupe;
            this.statusMatelassage = statusMatelassage;
            this.completesLockedSequence = completesLockedSequence;
            this.longueur = longueur;
            this.nbrCouche = nbrCouche;
            this.dueDate = dueDate;
            this.validated = validated;
            this.requiredLength = requiredLength;
            this.releaseProxyAt = releaseProxyAt;
        }
    }
}
