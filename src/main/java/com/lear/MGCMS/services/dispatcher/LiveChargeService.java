package com.lear.MGCMS.services.dispatcher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.services.CapaciteInstalleeService;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;
import com.lear.MGCMS.services.scheduling.ShiftClock;

/**
 * Status-aware live snapshot for the {@code /processDispatcher} page.
 *
 * <h2>Output shape</h2>
 * For every active sequence (sequenceStatus = ACTIVE or null, dueDate not null)
 * the service returns:
 * <ul>
 *   <li>The <b>effective zone</b> — {@link LockResolver}'s lock zone if the
 *       sequence is locked, else {@code dispatchedZone}.</li>
 *   <li>Per serie: the validated cutting time (TimingModel-priority), the
 *       elapsed-time deduction for In-progress series, and the resulting
 *       remaining minutes.</li>
 *   <li>Per (zone, machineType) bucket: load split between locked and
 *       pending, plus the full capacity formula
 *       ({@code activeMachines × shiftMinutes × efficiencePct/100}).</li>
 * </ul>
 *
 * <h2>Performance</h2>
 * One light projection of every active sequence; one batched serie projection;
 * one {@code TimingModel} batch via {@link CuttingTimeCalculator}; one batched
 * {@code ProductionTable} lookup. All aggregation happens in Java, but the
 * dataset is bounded ({@code findAllActiveLight} returns ~400 rows in
 * production). Target: under 1.5s end-to-end.
 *
 * <h2>Read-only</h2>
 * Never mutates state; safe to call as often as the UI wants.
 */
@Service
public class LiveChargeService {

    private static final double DEFAULT_SHIFT_MINUTES_FALLBACK = 460.0;
    private static final double DEFAULT_EFFICIENCE_TARGET_FALLBACK = 90.0;
    private static final int SQL_BATCH_SIZE = 2000;
    private static final Logger log = LoggerFactory.getLogger(LiveChargeService.class);

    @Autowired private CuttingRequestRepository cuttingRequestRepository;
    @Autowired private CuttingRequestSerieDataRepository serieDataRepository;
    @Autowired private ProductionTableRepository productionTableRepository;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private CapaciteInstalleeService capaciteInstalleeService;
    @Autowired private CuttingTimeCalculator cuttingTimeCalculator;
    @Autowired private ShiftProperties shiftProperties;
    @Autowired private ActiveMachineResolver activeMachineResolver;
    @Autowired private ShiftClock shiftClock;
    /**
     * Read the engine's in-memory bestAssignment so /liveCharge reflects engine
     * proposals in real time. Without this overlay the page shows stale
     * dispatchedZone values during a run.
     */
    @Autowired private ContinuousDispatchOptimizerService optimizerService;

    // Result cache — series data changes slowly; engine proposals are overlaid separately
    private volatile LiveChargeDto cachedResult = null;
    private volatile long cachedAtMs = 0;
    private static final long CACHE_TTL_MS = 30_000;

    @Transactional(readOnly = true)
    public LiveChargeDto compute() {
        long startMs = System.currentTimeMillis();
        LiveChargeDto cached = cachedResult;
        if (cached != null && System.currentTimeMillis() - cachedAtMs < CACHE_TTL_MS) {
            log.debug("LiveChargeService cache hit — {} ms", System.currentTimeMillis() - startMs);
            return cached;
        }

        LocalDateTime now = LocalDateTime.now();
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        int shiftMinutes = shiftProperties.getDurationMinutes() > 0
                ? shiftProperties.getDurationMinutes()
                : (int) DEFAULT_SHIFT_MINUTES_FALLBACK;

        // 1. Active sequence projection scoped to current/overdue due buckets.
        List<Object[]> requestRows = cuttingRequestRepository.findActiveDueOnOrBeforeLight(
                slot.date, String.valueOf(slot.shift));
        long stepStart = System.currentTimeMillis();
        if (requestRows.isEmpty()) {
            // Fresh shift: no active sequences yet. Still build the full zone
            // slate (machine types + capacities at zero load) — the logistics
            // release balancer consumes these capacities to place the first
            // batch of the shift; returning no zones would disable balancing
            // exactly when it matters most.
            ZoneSlate slate = loadZoneSlate(slot);
            return aggregate(now, slot, shiftMinutes, new LinkedHashMap<>(),
                    slate.activeZones, slate.zoneByNom, slate.machinesByZoneByType,
                    slate.activeMachinesByZone, slate.efficienceCache);
        }
        Map<String, RequestLight> requestBySeq = new LinkedHashMap<>();
        for (Object[] r : requestRows) {
            String sequence = (String) r[0];
            LocalDate dueDate = r.length > 5 && r[5] instanceof LocalDate ? (LocalDate) r[5] : null;
            requestBySeq.put(sequence, new RequestLight(
                    sequence,
                    (String) r[1],                                       // dispatchedZone
                    (String) r[2],                                       // zoneAcceptanceStatus
                    r[3] != null && Boolean.TRUE.equals(r[3]),           // pinnedByChef
                    (String) r[4],                                     // zone.nom (preferredZone)
                    dueDate,
                    r.length > 7 ? (String) r[7] : null));               // releaseZone
        }
        List<String> sequences = new ArrayList<>(requestBySeq.keySet());

        // 2. Series — batched to stay under SQL Server's 2100-parameter IN-clause cap.
        List<Object[]> serieRows = new ArrayList<>();
        for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
            serieRows.addAll(serieDataRepository.findLiveChargeSeriesBySequences(batch));
        }
        logStep("findLiveChargeSeriesBySequences", stepStart, System.currentTimeMillis());
        stepStart = System.currentTimeMillis();

        // 3. Detect sequences whose every serie is Complete (no remaining work).
        //    Those drop out of the live view entirely — they don't claim capacity.
        List<String> completeSequenceIds = new ArrayList<>();
        for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
            completeSequenceIds.addAll(serieDataRepository.findSequenceIdsWithCompleteSeriesIn(batch));
        }
        logStep("findSequenceIdsWithCompleteSeriesIn", stepStart, System.currentTimeMillis());
        stepStart = System.currentTimeMillis();
        // The "drop entirely" criterion is *all* series Complete, not "any". We
        // need to detect that locally because the repo only flags "any" — a
        // sequence with one Complete + one Waiting serie still has remaining work.
        Set<String> droppable = new HashSet<>(completeSequenceIds);

        Map<String, List<RawSerieRow>> seriesBySeq = new LinkedHashMap<>();
        Set<String> tableNoms = new LinkedHashSet<>();
        Map<String, Boolean> hasNonCompleteBySeq = new HashMap<>();
        for (Object[] sr : serieRows) {
            RawSerieRow row = RawSerieRow.from(sr);
            seriesBySeq.computeIfAbsent(row.sequence, k -> new ArrayList<>()).add(row);
            if (row.tableCoupe != null && !row.tableCoupe.trim().isEmpty()) {
                tableNoms.add(row.tableCoupe);
            }
            if (!"Complete".equalsIgnoreCase(row.statusCoupe)) {
                hasNonCompleteBySeq.put(row.sequence, Boolean.TRUE);
            }
        }
        // Refine droppable: only drop if there is no non-Complete serie at all.
        droppable.removeIf(seq -> Boolean.TRUE.equals(hasNonCompleteBySeq.get(seq)));

        // 4. Table → STRICT/SHARED zone map (one batched query).
        Map<String, LockResolver.TableZoneInfo> tableToZone = loadTableZoneMap(tableNoms);
        logStep("loadTableZoneMap", stepStart, System.currentTimeMillis());
        stepStart = System.currentTimeMillis();

        // 4b. Engine's current bestAssignment overlay — surfaces live engine
        // proposals in real time. Empty when no run has started yet.
        Map<String, String> engineProposals = optimizerService.getCurrentBestAssignment();

        // 4c. Active zones + which machine types each one physically hosts.
        // Drives per-serie targetZone routing: a serie of a given machineType
        // executes in the sequence's owner zone if that zone hosts the type,
        // else in a SHARED zone that hosts it (DIE / Gerber / LASER-DXF case),
        // else in any STRICT zone hosting it. SHARED is preferred over a
        // foreign STRICT zone because that's the plant's intent — SHARED is
        // designed to be reachable from every STRICT zone.
        ZoneSlate slate = loadZoneSlate(slot);
        List<Zone> activeZones = slate.activeZones;
        Map<String, Zone> zoneByNom = slate.zoneByNom;
        Map<String, Map<String, Set<String>>> machinesByZoneByType = slate.machinesByZoneByType;
        Map<String, Set<String>> activeMachinesByZone = slate.activeMachinesByZone;
        Map<String, Double> efficienceCache = slate.efficienceCache;

        // Stable lookup order: SHARED first, then STRICT, alpha within each.
        List<String> zoneRoutingOrder = new ArrayList<>();
        for (Zone z : activeZones) if (z.getCategory() == Zone.Category.SHARED) zoneRoutingOrder.add(z.getNom());
        for (Zone z : activeZones) if (z.getCategory() != Zone.Category.SHARED) zoneRoutingOrder.add(z.getNom());

        logStep("preloadZoneMachines+activeMachines+efficience", stepStart, System.currentTimeMillis());
        stepStart = System.currentTimeMillis();

        // 5. CuttingTimeCalculator inputs — one batch on TimingModel.
        List<CuttingTimeCalculator.SerieInput<SerieKey>> timeInputs = new ArrayList<>();
        for (Map.Entry<String, List<RawSerieRow>> e : seriesBySeq.entrySet()) {
            for (RawSerieRow r : e.getValue()) {
                timeInputs.add(new CuttingTimeCalculator.SerieInput<>(
                        new SerieKey(r.sequence, r.serie),
                        r.placement, r.tempsDeCoupe, r.nbrCouche, r.machine));
            }
        }
        Map<String, CuttingTimeCalculator.TimingRow> timingMap = cuttingTimeCalculator
                .loadTimingMap(timeInputs.stream()
                        .map(s -> s.placement)
                        .collect(java.util.stream.Collectors.toList()));
        logStep("loadTimingMap", stepStart, System.currentTimeMillis());
        stepStart = System.currentTimeMillis();

        // 6. Build per-sequence DTOs with lock decision + per-serie remaining math.
        Map<String, SequenceState> stateBySeq = new LinkedHashMap<>();
        for (Map.Entry<String, List<RawSerieRow>> entry : seriesBySeq.entrySet()) {
            String sequence = entry.getKey();
            if (droppable.contains(sequence)) continue;
            RequestLight req = requestBySeq.get(sequence);
            if (req == null) continue;
            if ("REJECTED".equalsIgnoreCase(req.zoneAcceptanceStatus)) continue;

            List<RawSerieRow> rawSeries = entry.getValue();
            // Lock resolution.
            List<LockResolver.SerieLockInput> lockInputs = new ArrayList<>(rawSeries.size());
            for (RawSerieRow r : rawSeries) {
                lockInputs.add(new LockResolver.SerieLockInput(
                        r.serie, r.statusCoupe, r.tableCoupe, r.dateDebutCoupe));
            }
            Optional<LockResolver.LockResult> lockOpt = LockResolver.resolve(
                    req.dispatchedZone, req.zoneAcceptanceStatus, lockInputs, tableToZone);

            // Effective-zone priority — see SequenceDto.zoneSource for the
            // labels the UI uses. Lock zone wins because it's physical reality;
            // engine proposals override DB values during a run; otherwise
            // dispatchedZone or the CR's preferred zone (zoneFix).
            String effectiveZone;
            String zoneSource;
            boolean locked = lockOpt.isPresent();
            if (locked) {
                effectiveZone = lockOpt.get().getLockZoneNom();
                zoneSource = "LOCKED";
            } else if (engineProposals.containsKey(sequence)) {
                effectiveZone = engineProposals.get(sequence);
                zoneSource = "ENGINE_PROPOSED";
            } else if (req.releaseZone != null) {
                effectiveZone = req.releaseZone;
                zoneSource = "RELEASED";
            } else if (req.dispatchedZone != null) {
                effectiveZone = req.dispatchedZone;
                zoneSource = "DISPATCHED";
            } else if (req.preferredZoneNom != null) {
                effectiveZone = req.preferredZoneNom;
                zoneSource = "PREFERRED";
            } else {
                effectiveZone = null;
                zoneSource = "NONE";
            }
            // Mismatch flag: effectiveZone differs from the DB's dispatchedZone.
            // For LOCKED that signals an implicit override; for ENGINE_PROPOSED
            // that signals a pending move the chef hasn't published yet.
            boolean zoneMismatch = effectiveZone != null
                    && req.dispatchedZone != null
                    && !req.dispatchedZone.equalsIgnoreCase(effectiveZone);

            // Per-serie remaining math + per-serie zone routing.
            List<LiveChargeDto.SerieDto> serieDtos = new ArrayList<>(rawSeries.size());
            double totalRemaining = 0.0;
            // Lower-bound estimate of box cycle time = longest single-serie remaining
            // minutes. Real cycle time needs a schedule (start/end timestamps); until
            // then this gives the UI a meaningful "this sequence cannot finish faster
            // than X" floor that updates as series complete.
            double longestSerieMinutes = 0.0;
            // remainingByZone × machineType — sequence's load is split across zones
            // because a sequence "owned" by FirstArticle may have a LASER-DXF
            // serie that runs in SHARED. Aggregation needs the breakdown.
            Map<String, Map<String, Double>> remainingByZoneByType = new LinkedHashMap<>();
            for (RawSerieRow r : rawSeries) {
                CuttingTimeCalculator.Resolved resolved = cuttingTimeCalculator.resolve(
                        r.placement, r.tempsDeCoupe, r.nbrCouche, r.machine, timingMap);
                double validated = resolved.minutes;
                double elapsed = 0.0;
                double remaining;
                String status = r.statusCoupe == null ? "" : r.statusCoupe.trim();
                if ("Complete".equalsIgnoreCase(status)) {
                    remaining = 0.0;
                } else if ("In progress".equalsIgnoreCase(status)) {
                    if (r.dateDebutCoupe != null) {
                        elapsed = Math.max(0.0,
                                Duration.between(r.dateDebutCoupe, now).toMillis() / 60000.0);
                    }
                    remaining = Math.max(0.0, validated - elapsed);
                } else {
                    // Waiting / Incomplete / null → treat as full estimate
                    remaining = validated;
                }

                // Route this serie's load to the zone that physically hosts the
                // machine type. Owner zone wins when it can host; SHARED next
                // (per-zone preference order); finally any other STRICT zone.
                String targetZone = resolveTargetZone(
                        effectiveZone, r.machine, machinesByZoneByType,
                        zoneRoutingOrder);
                String targetCategory = null;
                if (targetZone != null) {
                    Zone tz = zoneByNom.get(targetZone);
                    targetCategory = tz != null && tz.getCategory() != null
                            ? tz.getCategory().name() : "STRICT";
                }
                // Phase 8 stub: tableLengthRequired needs longueur from projection
                double tableLengthRequired = (r.longueur != null && r.nbrCouche != null)
                        ? round2(r.longueur * r.nbrCouche) : 0.0;
                serieDtos.add(new LiveChargeDto.SerieDto(
                        r.serie, r.machine, r.tempsDeCoupe,
                        round2(validated), resolved.source.name(),
                        r.statusCoupe, r.statusMatelassage,
                        r.tableCoupe, r.tableMatelassage,
                        r.dateDebutCoupe, r.dateFinCoupe,
                        r.dateDebutMatelassage, r.dateFinMatelassage,
                        round2(elapsed), round2(remaining),
                        targetZone, targetCategory,
                        r.refTissus, null, tableLengthRequired));
                if (remaining > 0 && r.machine != null && targetZone != null) {
                    remainingByZoneByType
                            .computeIfAbsent(targetZone, k -> new LinkedHashMap<>())
                            .merge(r.machine, remaining, Double::sum);
                }
                totalRemaining += remaining;
                if (remaining > longestSerieMinutes) longestSerieMinutes = remaining;
            }

            // Phase 8 stub: boxCycleTimeMinutes and materialStatus ship later
            LiveChargeDto.SequenceDto seqDto = new LiveChargeDto.SequenceDto(
                    sequence, req.preferredZoneNom, req.dispatchedZone,
                    effectiveZone, zoneSource, zoneMismatch, locked,
                    locked ? lockOpt.get().getReason().name() : null,
                    locked ? lockOpt.get().getLockingSerieId() : null,
                    locked ? lockOpt.get().getLockingTableNom() : null,
                    locked ? lockOpt.get().getLockingStatusCoupe() : null,
                    req.pinnedByChef, req.zoneAcceptanceStatus,
                    round2(totalRemaining), serieDtos,
                    req.dueDate, round2(longestSerieMinutes), null);

            stateBySeq.put(sequence, new SequenceState(seqDto, effectiveZone, locked, remainingByZoneByType));
        }

        // 7. Group by zone + compute per-(zone, machineType) capacity.
        LiveChargeDto result = aggregate(now, slot, shiftMinutes, stateBySeq,
                activeZones, zoneByNom, machinesByZoneByType,
                activeMachinesByZone, efficienceCache);
        logStep("aggregate", stepStart, System.currentTimeMillis());
        long elapsedMs = System.currentTimeMillis() - startMs;
        if (elapsedMs > 500) {
            log.warn("LiveChargeService.compute() took {} ms — candidates={}, zones={}", elapsedMs, requestBySeq.size(), result.getZones() != null ? result.getZones().size() : 0);
        } else {
            log.debug("LiveChargeService.compute() took {} ms", elapsedMs);
        }
        return result;
    }

    // ----------------------------------------------------------------- aggregation

    private LiveChargeDto aggregate(LocalDateTime now, ShiftClock.ShiftSlot slot,
                                    int shiftMinutes,
                                    Map<String, SequenceState> stateBySeq,
                                    List<Zone> activeZones,
                                    Map<String, Zone> zoneByNom,
                                    Map<String, Map<String, Set<String>>> machinesByZoneByType,
                                    Map<String, Set<String>> activeMachinesByZone,
                                    Map<String, Double> efficienceCache) {

        // Group sequences by ownership (sequence's effectiveZone) — drives the
        // "Locked" / "Mobile" sequence lists per zone card. Independent of how
        // each serie's load is routed: a sequence owned by FirstArticle may
        // have a LASER-DXF serie whose load lives in a SHARED zone.
        Map<String, List<SequenceState>> seqsByOwnerZone = new LinkedHashMap<>();
        List<SequenceState> unassigned = new ArrayList<>();
        for (SequenceState st : stateBySeq.values()) {
            if (st.effectiveZone == null) {
                unassigned.add(st);
            } else {
                seqsByOwnerZone.computeIfAbsent(st.effectiveZone, k -> new ArrayList<>()).add(st);
            }
        }

        // Per-(zone, MT) load split by locked vs pending. Built by walking
        // every serie's targetZone (NOT the parent sequence's owner zone).
        Map<String, Map<String, Double>> lockedLoad = new HashMap<>();   // zoneNom → MT → min
        Map<String, Map<String, Double>> pendingLoad = new HashMap<>();
        for (SequenceState st : stateBySeq.values()) {
            Map<String, Map<String, Double>> sink = st.locked ? lockedLoad : pendingLoad;
            for (Map.Entry<String, Map<String, Double>> ze : st.remainingByZoneByType.entrySet()) {
                String tz = ze.getKey();
                Map<String, Double> bucket = sink.computeIfAbsent(tz, k -> new HashMap<>());
                for (Map.Entry<String, Double> mt : ze.getValue().entrySet()) {
                    bucket.merge(mt.getKey(), mt.getValue(), Double::sum);
                }
            }
        }

        // Build zone DTOs in stable order: STRICT first (alpha), then SHARED.
        List<String> orderedZones = new ArrayList<>(seqsByOwnerZone.keySet());
        // Append every active zone even if it has no sequences, so the UI always
        // shows the zone slate — empty zones still carry capacity worth knowing,
        // and SHARED zones almost always lack "owned" sequences but absorb load.
        for (Zone z : activeZones) {
            if (!orderedZones.contains(z.getNom())) orderedZones.add(z.getNom());
        }
        orderedZones.sort((a, b) -> {
            Zone za = zoneByNom.get(a);
            Zone zb = zoneByNom.get(b);
            boolean aShared = za != null && za.getCategory() == Zone.Category.SHARED;
            boolean bShared = zb != null && zb.getCategory() == Zone.Category.SHARED;
            if (aShared != bShared) return aShared ? 1 : -1;
            return a.compareToIgnoreCase(b);
        });

        List<LiveChargeDto.ZoneChargeDto> zoneDtos = new ArrayList<>();
        int totalSequences = 0;
        int totalLocked = 0;
        int totalPending = 0;
        double totalRemainingAll = 0.0;
        double totalCapacityAll = 0.0;

        for (String zoneNom : orderedZones) {
            Zone zone = zoneByNom.get(zoneNom);
            List<SequenceState> here = seqsByOwnerZone.getOrDefault(zoneNom, Collections.emptyList());

            // Locked/pending load for THIS zone — series targeted here from any
            // owner. May include load from sequences owned by other zones.
            Map<String, Double> lockedByType = lockedLoad.getOrDefault(zoneNom, Collections.emptyMap());
            Map<String, Double> pendingByType = pendingLoad.getOrDefault(zoneNom, Collections.emptyMap());
            Set<String> typesPresent = new LinkedHashSet<>();
            typesPresent.addAll(lockedByType.keySet());
            typesPresent.addAll(pendingByType.keySet());
            // Also surface machine types that exist physically in the zone but
            // currently carry no work — capacity bar still meaningful.
            Map<String, Set<String>> machinesByType = machinesByZoneByType
                    .getOrDefault(zoneNom, Collections.emptyMap());
            typesPresent.addAll(machinesByType.keySet());

            // Per-type capacity.
            Set<String> upInZone = activeMachinesByZone.getOrDefault(zoneNom, Collections.emptySet());
            List<LiveChargeDto.MachineTypeChargeDto> typeDtos = new ArrayList<>();
            double zoneRemaining = 0.0;
            double zoneCapacity = 0.0;
            List<String> sortedTypes = new ArrayList<>(typesPresent);
            sortedTypes.sort(String.CASE_INSENSITIVE_ORDER);
            for (String type : sortedTypes) {
                int activeMachines = countActiveMachinesOfType(upInZone, machinesByType, type);
                double effPct = efficienceCache.getOrDefault(groupeOf(type), DEFAULT_EFFICIENCE_TARGET_FALLBACK);
                double cap = activeMachines * shiftMinutes * (effPct / 100.0);
                double lockedRem = lockedByType.getOrDefault(type, 0.0);
                double pendingRem = pendingByType.getOrDefault(type, 0.0);
                double totalRem = lockedRem + pendingRem;
                double loadPct = cap > 0 ? (totalRem / cap) * 100.0 : 0.0;
                typeDtos.add(new LiveChargeDto.MachineTypeChargeDto(
                        type, groupeOf(type), activeMachines, shiftMinutes,
                        round2(effPct), round2(cap),
                        round2(lockedRem), round2(pendingRem),
                        round2(totalRem), round2(loadPct)));
                zoneRemaining += totalRem;
                zoneCapacity += cap;
            }

            // Sort sequences for display: highest remaining first.
            List<LiveChargeDto.SequenceDto> lockedDtos = new ArrayList<>();
            List<LiveChargeDto.SequenceDto> pendingDtos = new ArrayList<>();
            for (SequenceState st : here) {
                if (st.locked) lockedDtos.add(st.dto);
                else pendingDtos.add(st.dto);
            }
            Comparator<LiveChargeDto.SequenceDto> byRemaining =
                    (a, b) -> Double.compare(b.getTotalRemainingMinutes(), a.getTotalRemainingMinutes());
            lockedDtos.sort(byRemaining);
            pendingDtos.sort(byRemaining);

            double zoneLoadPct = zoneCapacity > 0 ? (zoneRemaining / zoneCapacity) * 100.0 : 0.0;
            zoneDtos.add(new LiveChargeDto.ZoneChargeDto(
                    zoneNom,
                    zone == null || zone.getCategory() == null ? "STRICT" : zone.getCategory().name(),
                    typeDtos, lockedDtos, pendingDtos,
                    round2(zoneRemaining), round2(zoneCapacity), round2(zoneLoadPct)));

            totalSequences += here.size();
            totalLocked += lockedDtos.size();
            totalPending += pendingDtos.size();
            totalRemainingAll += zoneRemaining;
            totalCapacityAll += zoneCapacity;
        }

        // Unassigned bucket (no effectiveZone resolved).
        List<LiveChargeDto.SequenceDto> unassignedDtos = new ArrayList<>();
        for (SequenceState st : unassigned) unassignedDtos.add(st.dto);
        unassignedDtos.sort((a, b) ->
                Double.compare(b.getTotalRemainingMinutes(), a.getTotalRemainingMinutes()));

        LiveChargeDto.TotalsDto totals = new LiveChargeDto.TotalsDto(
                totalSequences + unassignedDtos.size(),
                totalLocked, totalPending, unassignedDtos.size(),
                round2(totalRemainingAll), round2(totalCapacityAll));

        LiveChargeDto result = new LiveChargeDto(now, slot.date, slot.shift, shiftMinutes,
                totals, zoneDtos, unassignedDtos);
        cachedResult = result;
        cachedAtMs = System.currentTimeMillis();
        return result;
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Count the machines of {@code type} in this zone that are <b>actually
     * active</b> right now. Active = either (a) the chef-de-zone has confirmed
     * the machine for this (date, shift), or (b) no chef confirmation exists
     * but the machine's current {@code EtatMachineHistorique} status is
     * {@code 'M'} (or null, which the resolver treats as 'M').
     *
     * <p>Both checks are already aggregated into {@code upInZone} by
     * {@link ActiveMachineResolver}. So an empty {@code upInZone} means
     * "no active machines in this zone, period" — the cell's capacity is 0
     * and we report 0 here, matching the engine's
     * {@code ContinuousDispatchOptimizerService.buildSnapshotInternal}
     * convention.</p>
     */
    private static int countActiveMachinesOfType(Set<String> upInZone,
                                                  Map<String, Set<String>> machinesByType,
                                                  String type) {
        Set<String> ofType = machinesByType.getOrDefault(type, Collections.emptySet());
        if (ofType.isEmpty()) return 0;
        if (upInZone == null || upInZone.isEmpty()) {
            // Zone has no active machines (neither chef-confirmed nor M-status
            // in EtatMachineHistorique). Capacity = 0; cell shows 0.
            return 0;
        }
        Set<String> intersect = new HashSet<>(ofType);
        intersect.retainAll(upInZone);
        return intersect.size();
    }

    private double lookupEfficiencePct(LocalDate date, int shift, String machineType) {
        String groupe = groupeOf(machineType);
        var ci = capaciteInstalleeService.getEffective(date, shift, groupe);
        if (ci == null || ci.getEfficienceTarget() == null) {
            return DEFAULT_EFFICIENCE_TARGET_FALLBACK;
        }
        return ci.getEfficienceTarget();
    }

    private static String groupeOf(String machineType) {
        if (machineType == null) return "Coupe";
        String t = machineType.trim();
        if (t.equalsIgnoreCase("LASER-DXF") || t.equalsIgnoreCase("LASER-LSR")
                || t.equalsIgnoreCase("LASER")) {
            return "Laser";
        }
        return "Coupe";
    }

    /**
     * Zone topology preload shared by the normal path and the fresh-shift
     * (no active sequences) path: active zones, machines per (zone, type),
     * active machines per zone, efficience per groupe.
     */
    private ZoneSlate loadZoneSlate(ShiftClock.ShiftSlot slot) {
        ZoneSlate slate = new ZoneSlate();
        slate.activeZones = zoneRepository.findAllActive();
        slate.zoneByNom = new HashMap<>();
        List<String> zoneNoms = new ArrayList<>(slate.activeZones.size());
        for (Zone z : slate.activeZones) {
            slate.zoneByNom.put(z.getNom(), z);
            zoneNoms.add(z.getNom());
        }

        // Batch load machines for all zones in ONE query (was N per-zone queries).
        slate.machinesByZoneByType = new HashMap<>();
        if (!zoneNoms.isEmpty()) {
            List<Object[]> rows = productionTableRepository.findMachinesWithTypeInZones(zoneNoms);
            for (Object[] row : rows) {
                String zoneNom = (String) row[0];
                String machineNom = (String) row[1];
                String typeName = (String) row[2];
                if (typeName == null) continue;
                slate.machinesByZoneByType
                        .computeIfAbsent(zoneNom, k -> new HashMap<>())
                        .computeIfAbsent(typeName, t -> new LinkedHashSet<>())
                        .add(machineNom);
            }
        }
        for (String zn : zoneNoms) {
            slate.machinesByZoneByType.computeIfAbsent(zn, k -> new HashMap<>());
        }

        // Pre-load active machines for all zones ONCE (was one call per zone inside aggregate).
        slate.activeMachinesByZone = new HashMap<>();
        for (Zone z : slate.activeZones) {
            slate.activeMachinesByZone.put(z.getNom(),
                    activeMachineResolver.activeMachines(slot.date, slot.shift, z.getNom()));
        }

        // Pre-load efficience for this (date, shift) — only 2 groupes exist.
        slate.efficienceCache = new HashMap<>();
        for (String groupe : Arrays.asList("Coupe", "Laser")) {
            var ci = capaciteInstalleeService.getEffective(slot.date, slot.shift, groupe);
            slate.efficienceCache.put(groupe,
                    ci != null && ci.getEfficienceTarget() != null
                            ? ci.getEfficienceTarget()
                            : DEFAULT_EFFICIENCE_TARGET_FALLBACK);
        }
        return slate;
    }

    /** Carrier for {@link #loadZoneSlate}. */
    private static final class ZoneSlate {
        List<Zone> activeZones;
        Map<String, Zone> zoneByNom;
        Map<String, Map<String, Set<String>>> machinesByZoneByType;
        Map<String, Set<String>> activeMachinesByZone;
        Map<String, Double> efficienceCache;
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

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private void logStep(String step, long startMs, long endMs) {
        long elapsed = endMs - startMs;
        if (elapsed > 100) {
            log.warn("LiveChargeService step '{}' took {} ms", step, elapsed);
        } else {
            log.debug("LiveChargeService step '{}' took {} ms", step, elapsed);
        }
    }

    // ----------------------------------------------------------------- internal carriers

    private static final class RequestLight {
        final String sequence;
        final String dispatchedZone;
        final String zoneAcceptanceStatus;
        final boolean pinnedByChef;
        final String preferredZoneNom;
        final LocalDate dueDate;
        final String releaseZone;
        RequestLight(String sequence, String dispatchedZone, String zoneAcceptanceStatus,
                     boolean pinnedByChef, String preferredZoneNom, LocalDate dueDate,
                     String releaseZone) {
            this.sequence = sequence;
            this.dispatchedZone = dispatchedZone;
            this.zoneAcceptanceStatus = zoneAcceptanceStatus;
            this.pinnedByChef = pinnedByChef;
            this.preferredZoneNom = preferredZoneNom;
            this.dueDate = dueDate;
            this.releaseZone = releaseZone;
        }
    }

    /**
     * Light projection row carrier. Mirrors the column order of
     * {@link CuttingRequestSerieDataRepository#findLiveChargeSeriesBySequences}.
     */
    private static final class RawSerieRow {
        final String serie;
        final String sequence;
        final String machine;
        final Double tempsDeCoupe;
        final Integer nbrCouche;
        final String placement;
        final String statusCoupe;
        final String statusMatelassage;
        final String tableCoupe;
        final String tableMatelassage;
        final LocalDateTime dateDebutCoupe;
        final LocalDateTime dateFinCoupe;
        final LocalDateTime dateDebutMatelassage;
        final LocalDateTime dateFinMatelassage;
        final String refTissus;
        final Double longueur;

        RawSerieRow(String serie, String sequence, String machine, Double tempsDeCoupe,
                    Integer nbrCouche, String placement, String statusCoupe,
                    String statusMatelassage, String tableCoupe, String tableMatelassage,
                    LocalDateTime dateDebutCoupe, LocalDateTime dateFinCoupe,
                    LocalDateTime dateDebutMatelassage, LocalDateTime dateFinMatelassage,
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
            this.tableMatelassage = tableMatelassage;
            this.dateDebutCoupe = dateDebutCoupe;
            this.dateFinCoupe = dateFinCoupe;
            this.dateDebutMatelassage = dateDebutMatelassage;
            this.dateFinMatelassage = dateFinMatelassage;
            this.refTissus = refTissus;
            this.longueur = longueur;
        }

        static RawSerieRow from(Object[] sr) {
            // TODO: if projection findLiveChargeSeriesBySequences is extended with
            // partNumberMaterial (idx 14) and longueur (idx 15), read them here.
            String refTissus = sr.length > 14 ? (String) sr[14] : null;
            Double longueur = sr.length > 15 && sr[15] != null ? ((Number) sr[15]).doubleValue() : null;
            return new RawSerieRow(
                    (String) sr[0],
                    (String) sr[1],
                    (String) sr[2],
                    sr[3] != null ? ((Number) sr[3]).doubleValue() : null,
                    sr[4] != null ? ((Number) sr[4]).intValue() : null,
                    (String) sr[5],
                    (String) sr[6],
                    (String) sr[7],
                    (String) sr[8],
                    (String) sr[9],
                    (LocalDateTime) sr[10],
                    (LocalDateTime) sr[11],
                    (LocalDateTime) sr[12],
                    (LocalDateTime) sr[13],
                    refTissus,
                    longueur);
        }
    }

    private static final class SerieKey {
        final String sequence;
        final String serie;
        SerieKey(String sequence, String serie) {
            this.sequence = sequence; this.serie = serie;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SerieKey)) return false;
            SerieKey k = (SerieKey) o;
            return sequence.equals(k.sequence) && serie.equals(k.serie);
        }
        @Override public int hashCode() {
            return sequence.hashCode() * 31 + serie.hashCode();
        }
    }

    /**
     * Internal carrier: per-sequence state used during aggregation.
     *
     * <p>{@code remainingByZoneByType} carries the per-serie routing decision —
     * outer key is the targetZone (which may differ from {@code effectiveZone}
     * when a serie's machine type lives in SHARED), inner key is the machine
     * type, value is remaining minutes for that (zone, type) cell.</p>
     */
    private static final class SequenceState {
        final LiveChargeDto.SequenceDto dto;
        final String effectiveZone; // sequence's owner zone — may be null
        final boolean locked;
        final Map<String, Map<String, Double>> remainingByZoneByType;
        SequenceState(LiveChargeDto.SequenceDto dto, String effectiveZone,
                      boolean locked,
                      Map<String, Map<String, Double>> remainingByZoneByType) {
            this.dto = dto;
            this.effectiveZone = effectiveZone;
            this.locked = locked;
            this.remainingByZoneByType = remainingByZoneByType;
        }
    }

    /**
     * Decide where a serie's load actually goes, given its parent sequence's
     * owner zone and the machine type. See {@link LiveChargeDto.SerieDto}'s
     * targetZoneNom doc for the priority order.
     *
     * <p>Package-private so {@link ContinuousDispatchOptimizerService} can use
     * the same routing rule for its load math — the engine and the live view
     * must agree on where each MT's load ends up, otherwise the heatmap and
     * the engine's spread metric drift apart.</p>
     *
     * @return zone name where the serie executes, or null when no active zone
     *         hosts the machine type.
     */
    static String resolveTargetZone(
            String ownerZone, String machineType,
            Map<String, Map<String, Set<String>>> machinesByZoneByType,
            List<String> zoneRoutingOrder) {

        if (machineType == null || machineType.trim().isEmpty()) return ownerZone;
        // Owner first.
        if (ownerZone != null) {
            Map<String, Set<String>> mbt = machinesByZoneByType.get(ownerZone);
            if (mbt != null && mbt.containsKey(machineType)) return ownerZone;
        }
        // Walk in routing order — SHARED first, then other STRICT zones.
        for (String z : zoneRoutingOrder) {
            if (z.equals(ownerZone)) continue;
            Map<String, Set<String>> mbt = machinesByZoneByType.get(z);
            if (mbt != null && mbt.containsKey(machineType)) return z;
        }
        return null;
    }
}
