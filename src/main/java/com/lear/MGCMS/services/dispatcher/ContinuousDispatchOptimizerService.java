package com.lear.MGCMS.services.dispatcher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.dispatcher.DispatchEngineIndicatorSample;
import com.lear.MGCMS.domain.dispatcher.DispatchEngineRun;
import com.lear.MGCMS.domain.dispatcher.DispatchEngineRunSuggestion;
import com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.dispatcher.DispatchEngineIndicatorSampleRepository;
import com.lear.MGCMS.repositories.dispatcher.DispatchEngineRunRepository;
import com.lear.MGCMS.repositories.dispatcher.DispatchEngineRunSuggestionRepository;
import com.lear.MGCMS.services.CapaciteInstalleeService;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;
// import for ScheduleBuilderService.SerieInput is implicit via @Autowired — same package

/**
 * Phase B — Continuous Dispatch Optimizer.
 *
 * <p>Moves PENDING sequences between STRICT zones to flatten the
 * {@code (machineType, zone)} load distribution. Frozen sequences
 * (pinned, accepted, or started) form the immovable baseline.</p>
 *
 * <p>Threading model: one background {@link ExecutorService} thread runs the
 * improving loop. All public methods are {@code synchronized} so state
 * transitions are atomic.</p>
 */
@Service
public class ContinuousDispatchOptimizerService {

    private static final Logger log = LoggerFactory.getLogger(ContinuousDispatchOptimizerService.class);

    private static final double DEFAULT_SHIFT_MINUTES = 460.0;
    private static final double DEFAULT_EFFICIENCE = 90.0;
    /**
     * How often (in iterations) the engine rebuilds the schedule + box-duration
     * aggregate. Builder runs ~10-30ms at ≤1000 series; refreshing every 25
     * iterations gives the cost function fresh KPI data without dominating the
     * loop budget.
     */
    private static final int BOX_DURATION_REFRESH_EVERY = 25;
    // SA knobs and phase durations are read from EngineProperties.Optimizer
    // — see optimizerCfg() accessors below.

    // ------------------------------------------------------------------ deps

    @Autowired private CuttingRequestRepository cuttingRequestRepository;
    @Autowired private com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository serieDataRepository;
    @Autowired private CuttingTimeCalculator cuttingTimeCalculator;
    @Autowired private ActiveMachineResolver activeMachineResolver;
    @Autowired private CapaciteInstalleeService capaciteInstalleeService;
    @Autowired private ProductionTableRepository productionTableRepository;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private ShiftProperties shiftProperties;
    @Autowired private ZoneLoadProperties zoneLoadProperties;

    @Autowired private DispatchEngineRunRepository runRepository;
    @Autowired private DispatchEngineRunSuggestionRepository suggestionRepository;
    @Autowired private DispatchEngineIndicatorSampleRepository sampleRepository;
    @Autowired private DispatchEngineWebSocketService webSocketService;

    @Autowired private SequenceDispatcherService sequenceDispatcherService;

    @Autowired private com.lear.MGCMS.services.scheduling.ShiftClock shiftClock;

    @Autowired private EngineProperties engineProperties;
    @Autowired private MaterialAvailabilityChecker materialAvailabilityChecker;
    @Autowired private com.lear.MGCMS.services.OrdonnancementService ordonnancementService;
    @Autowired private ScheduleBuilderService scheduleBuilderService;
    @Autowired private BoxDurationCalculator boxDurationCalculator;
    @Autowired private EngineSchedulePersistenceService schedulePersistenceService;
    @Autowired private com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository boxInfoRepository;

    // ------------------------------------------------------------------ state

    private final Object lock = new Object();
    private EngineState state = EngineState.IDLE;
    private EngineMode mode;
    private Long currentRunId;
    private LocalDate currentDate;
    private int currentShift;

    private volatile int iteration;
    private volatile double currentSpread;
    private volatile double currentRawSpread;
    private volatile double currentStdDev;
    private volatile double currentMedian;
    private volatile double initialSpread;
    private volatile double lastImprovement;
    private volatile boolean stopRequested;

    private ExecutorService executor;
    private Future<?> loopFuture;

    // Snapshot data (mutated only by the loop thread)
    private Map<String, Double> capacities = new HashMap<>(); // key = "type|zone"
    private Map<String, Double> baselineLoads = new HashMap<>(); // key = "type|zone"
    private Map<String, Double> pendingLoads = new HashMap<>(); // key = "type|zone"
    private List<Candidate> candidates = new ArrayList<>();
    /** Per-zone hosted machine types (used by per-serie target router). */
    private Map<String, Map<String, Set<String>>> snapshotMachinesByZoneByType = new HashMap<>();
    /** Routing preference order: SHARED zones first, then STRICT. */
    private List<String> snapshotZoneRoutingOrder = new ArrayList<>();
    private Map<String, String> bestAssignment = new HashMap<>(); // sequence -> zone
    private final Map<String, String> initialAssignment = new HashMap<>(); // baseline for moves count
    private double bestSpread = Double.MAX_VALUE;
    private double bestStdDev = Double.MAX_VALUE;
    private double bestMedian = Double.MAX_VALUE;
    private int improvements;
    private int noImprovementStreak;
    private int lastBestImprovementIteration;
    private boolean convergedFlag;
    private long loopStartMs;
    private Integer fixedDurationSec;

    // Simulated annealing + escape-mechanism state
    private double temperature;
    private boolean saMoveApplied = false;
    private int lastRebalanceIteration = 0;
    private int lastRandomizeIteration = 0;

    /** Shortcut: SA / weight tunables (read every iteration so live-reload works). */
    private EngineProperties.Optimizer cfg() {
        return engineProperties.getOptimizer();
    }

    // -- ALTERNATING mode phase tracking
    private volatile EnginePhase currentPhase;
    private long phaseStartMs;
    private volatile int ordonnancementIteration;
    private volatile long lastSnapshotLoadMs = 0;
    private volatile String maxLoadedSerie = null;

    // Run generation counter — prevents stale loop threads from interfering
    // after a rapid stop/start cycle (double-click race).
    private volatile long runGeneration = 0;

    /**
     * Cached material status: sequence -> zone -> count of fabric references
     * NOT on a rack in that zone. Used as a soft cost in {@link #evaluateMove}
     * (weight {@code materialAlertWeight}); never blocks dispatch.
     */
    private final Map<String, Map<String, Integer>> materialAvailabilityBySeqAndZone = new HashMap<>();

    // Cached cycle metrics (constant between snapshot rebuilds)
    private double cachedCycleComponent = 0.0;

    // Tracks whether the current run was started via startActive() (uses
    // buildSnapshotForActive) or start() (uses legacy buildSnapshot). Needed
    // so phase-switch and ordonnancement rebuilds use the correct loader.
    private boolean useActiveSnapshot = false;

    // Cached resolveTargetZone results: zone|machineType -> targetZone
    private final Map<String, String> resolveTargetZoneCache = new HashMap<>();

    /**
     * Set by event listeners ({@link SequenceAcceptanceChangedEvent},
     * {@link ZoneMachineToggledEvent}). The next loop iteration drains the
     * flag and rebuilds the snapshot. Coalesced — many events between
     * iterations produce one rebuild, not N.
     */
    private final AtomicBoolean rebuildRequested = new AtomicBoolean(false);

    /**
     * Serie-level inputs captured during {@code buildSnapshotInternal} so the
     * {@link ScheduleBuilderService} doesn't need to re-query the DB.
     * Cleared and rebuilt every snapshot.
     */
    private final List<ScheduleBuilderService.SerieInput> snapshotSerieInputs = new ArrayList<>();

    /**
     * Rich serie rows captured during {@link #buildSnapshotForActive} —
     * the 16-column projection from {@code findLiveChargeSeriesBySequences}.
     * Used by {@link #rebuildSchedule()} to construct {@code SerieInput}s
     * without a second DB hit. Empty for the legacy {@code buildSnapshot}
     * path, which produces a schedule with timestamps but no frozen anchors.
     */
    private final List<Object[]> snapshotRichSerieRows = new ArrayList<>();

    /** Current best schedule — produced after each snapshot rebuild. Read by the debug endpoint. */
    private volatile ScheduleSnapshot currentSchedule = ScheduleSnapshot.empty();
    private volatile int lastPersistedScheduleHash = 0;

    /**
     * Engine "now" — kept consistent across one snapshot rebuild so the
     * builder's relative timestamps are reproducible.
     */
    private LocalDateTime snapshotHorizon = LocalDateTime.now();

    /**
     * sequenceId -> number of boxes. Loaded at snapshot build and reused
     * across iterations — box counts change rarely (only when a box-info
     * row is added/removed for a sequence). The engine's box-duration
     * cost term reads from here.
     */
    private final Map<String, Integer> boxCountsBySequence = new HashMap<>();

    /**
     * Current best schedule's box-duration aggregate. Volatile so the state
     * snapshot accessor can publish it without holding a lock.
     */
    private volatile BoxDurationCalculator.Aggregate currentBoxDurationAggregate =
            BoxDurationCalculator.Aggregate.empty();

    /**
     * Slice 3 cost term — intra-zone machine load spread. For each (zone × MT)
     * pair, this is (max − min) of planned minutes across the machines hosting
     * that MT in that zone. Higher = more imbalanced (one Lectra crushed, the
     * other idle). Cached at snapshot rebuild time because it's a snapshot-wide
     * aggregate; Level-2 moves (when they land) will recompute it incrementally.
     * Zero when {@code intraZoneMachineLoadWeight} is disabled.
     */
    private volatile double currentIntraZoneSpread = 0.0;

    /** serieId → machineType lookup, rebuilt per snapshot. Used by Slice 3 moves. */
    private final Map<String, String> serieMtBySnapshot = new HashMap<>();

    /** Pure WAITING serie ids. Level-2/3 schedule moves must never touch committed floor work. */
    private final Set<String> movableSerieIdsBySnapshot = new HashSet<>();

    /** Factory so doStart can pass the generation token into the loop. */
    @FunctionalInterface
    private interface LoopFactory {
        void run(long generation);
    }

    // ------------------------------------------------------------------ public API

    public synchronized EngineState getState() { return state; }
    public synchronized EngineMode getMode() { return mode; }
    public synchronized Long getCurrentRunId() { return currentRunId; }
    public synchronized LocalDate getCurrentDate() { return currentDate; }
    public synchronized int getCurrentShift() { return currentShift; }
    public synchronized int getIteration() { return iteration; }
    public synchronized double getCurrentSpread() { return currentSpread; }
    public synchronized double getCurrentRawSpread() { return currentRawSpread; }
    public synchronized double getCurrentStdDev() { return currentStdDev; }
    public synchronized double getCurrentMedian() { return currentMedian; }
    public synchronized double getInitialSpread() { return initialSpread; }
    public synchronized double getLastImprovement() { return lastImprovement; }
    public synchronized double getBestSpread() { return bestSpread; }
    public synchronized double getBestStdDev() { return bestStdDev; }
    public synchronized double getBestMedian() { return bestMedian; }
    public synchronized EnginePhase getCurrentPhase() { return currentPhase; } // null when not ALTERNATING

    /**
     * Pre-loads the problem snapshot for the given date/shift so the engine
     * can start faster on the next run.  No-op if the engine is already running.
     */
    public synchronized void preloadSnapshot(LocalDate date, int shift) {
        if (state == EngineState.WARMING || state == EngineState.IMPROVING) {
            return;
        }
        log.debug("Preloading snapshot for {} shift {}", date, shift);
        try {
            buildSnapshot(date, shift);
            this.lastSnapshotLoadMs = System.currentTimeMillis();
            this.maxLoadedSerie = computeMaxSerieFromCandidates();
            log.info("Engine snapshot preloaded for {} shift {} — {} candidates", date, shift, candidates.size());
        } catch (Exception e) {
            log.warn("Failed to preload snapshot for {} shift {}", date, shift, e);
        }
    }

    /**
     * Atomic snapshot of all engine state fields read under a single lock.
     * Prevents torn reads that can happen when multiple individual getters
     * are called from the /state endpoint.
     */
    public synchronized Map<String, Object> getStateSnapshot() {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("state", state != null ? state.name() : null);
        snap.put("mode", mode != null ? mode.name() : null);
        snap.put("phase", currentPhase != null ? currentPhase.name() : null);
        snap.put("runId", currentRunId);
        snap.put("date", currentDate);
        snap.put("shift", currentShift);
        snap.put("iteration", iteration);
        snap.put("currentSpread", round2(currentSpread));
        snap.put("rawSpread", round2(currentRawSpread));
        snap.put("initialSpread", round2(initialSpread));
        snap.put("lastImprovement", round2(lastImprovement));
        snap.put("stdDev", round2(currentStdDev));
        snap.put("median", round2(currentMedian));
        snap.put("bestSpread", round2(bestSpread));
        snap.put("bestStdDev", round2(bestStdDev));
        snap.put("bestMedian", round2(bestMedian));
        snap.put("converged", convergedFlag);
        BoxDurationCalculator.Aggregate boxAgg = currentBoxDurationAggregate;
        if (boxAgg != null && boxAgg.sequencesMeasured > 0) {
            snap.put("boxDurationMean", round2(boxAgg.meanMinutesPerBox));
            snap.put("boxDurationMax", round2(boxAgg.maxMinutesPerBox));
            snap.put("boxDurationWorstSequence", boxAgg.worstSequenceId);
            snap.put("boxDurationSequencesMeasured", boxAgg.sequencesMeasured);
        }
        if (cfg().getIntraZoneMachineLoadWeight() > 0) {
            snap.put("intraZoneMachineSpread", round2(currentIntraZoneSpread));
        }
        return snap;
    }

    /**
     * Snapshot of the engine's current best (sequence → zone) map. Read by
     * {@code LiveChargeService} so the dispatcher page reflects engine
     * proposals in real time — without that, /liveCharge re-reads
     * dispatchedZone from the DB on every poll and the page looks frozen
     * while the engine is running.
     *
     * <p>Returns a defensive copy so the caller can iterate safely without
     * holding the lock. Empty when no run has been started yet.</p>
     */
    public synchronized Map<String, String> getCurrentBestAssignment() {
        return new HashMap<>(bestAssignment);
    }

    /**
     * Defensive snapshot of the engine's current schedule. Returns an empty
     * snapshot before the first build. Used by the debug
     * {@code /api/dispatcher/engine/schedule} endpoint and by the public
     * {@code /api/public/next-series} surface.
     */
    public ScheduleSnapshot getCurrentSchedule() {
        // currentSchedule is volatile; the reference itself is safe to publish.
        return currentSchedule;
    }

    /**
     * Force a snapshot rebuild on the next engine iteration. Returns immediately —
     * the rebuild happens on the engine thread. If the engine is not running,
     * this is a no-op.
     */
    public void requestRebuild() {
        rebuildRequested.set(true);
        log.info("Manual snapshot rebuild requested (engine state={})", state);
    }

    /**
     * Re-read active production data immediately when the engine is idle. When
     * the loop is running, coalesce the request into the next iteration to avoid
     * mutating the live snapshot from two threads.
     */
    public synchronized Map<String, Object> reloadActiveSnapshotFromGroundTruth() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("engineState", state != null ? state.name() : null);
        if (state == EngineState.WARMING || state == EngineState.IMPROVING || state == EngineState.PAUSED) {
            rebuildRequested.set(true);
            out.put("status", "REBUILD_REQUESTED");
            return out;
        }

        try {
            this.useActiveSnapshot = true;
            com.lear.MGCMS.services.scheduling.ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
            this.currentDate = slot.date;
            this.currentShift = slot.shift;
            buildSnapshotForActive();
            this.lastSnapshotLoadMs = System.currentTimeMillis();
            this.maxLoadedSerie = computeMaxSerieFromCandidates();
            greedyWarmStart();
            this.currentSpread = computeSpread();
            this.currentRawSpread = computeRawSpread();
            this.currentStdDev = computeStdDev();
            this.currentMedian = computeMedian();
            this.bestSpread = this.currentSpread;
            this.bestStdDev = this.currentStdDev;
            this.bestMedian = this.currentMedian;
            saveBestAssignment();
            rebuildSchedule();
            out.put("status", "RELOADED");
            out.put("date", this.currentDate);
            out.put("shift", this.currentShift);
            out.put("candidates", candidates.size());
            out.put("scheduleSlots", currentSchedule != null ? currentSchedule.size() : 0);
        } catch (Exception e) {
            log.warn("Manual active snapshot reload failed", e);
            out.put("status", "FAILED");
            out.put("error", e.getMessage());
        }
        return out;
    }

    // ============================ Slice 3 / 4 move attempts ============================
    //
    // Both move kinds mutate `currentSchedule` in place, recompute the affected
    // cached aggregate, re-evaluate the cost, and revert on rejection. They are
    // gated by EngineProperties flags (default false) so the historic engine
    // behavior is preserved until the flags are flipped on.

    /**
     * Slice 3 — Level-2 move: relocate one serie to a different machine of the
     * same machine type within its current zone. Hits the {@code currentIntraZoneSpread}
     * cost term. Returns true if a move was accepted.
     *
     * <p>Guards: serie's slot must not have started yet (plannedStart in the
     * future) so we never disturb in-progress cutting work.</p>
     */
    private boolean tryLevel2Move() {
        if (!cfg().isLevel2Enabled()) return false;
        if (currentSchedule == null || currentSchedule.size() == 0) return false;
        if (snapshotMachinesByZoneByType == null || snapshotMachinesByZoneByType.isEmpty()) return false;
        if (cfg().getIntraZoneMachineLoadWeight() <= 0) return false; // cost term inactive

        java.util.List<ScheduleSnapshot.PlannedSlot> all =
                new ArrayList<>(currentSchedule.copyOfSlots().values());
        if (all.isEmpty()) return false;
        ScheduleSnapshot.PlannedSlot chosen = all.get(ThreadLocalRandom.current().nextInt(all.size()));
        if (chosen.getZoneNom() == null || chosen.getMachineNom() == null) return false;
        if (!isMovableScheduledSlot(chosen)) return false;
        if (chosen.getPlannedStart() == null) return false;
        if (chosen.getPlannedStart().isBefore(LocalDateTime.now())) return false; // already started

        String mt = serieMtBySnapshot.get(chosen.getSerieId());
        if (mt == null) return false;
        Map<String, Set<String>> byType = snapshotMachinesByZoneByType.get(chosen.getZoneNom());
        if (byType == null) return false;
        Set<String> sameMtMachines = byType.get(mt);
        if (sameMtMachines == null || sameMtMachines.size() < 2) return false;

        List<String> candidates = new ArrayList<>(sameMtMachines);
        candidates.remove(chosen.getMachineNom());
        if (candidates.isEmpty()) return false;
        String newMachine = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        double before = computeSpread();
        double beforeIntra = currentIntraZoneSpread;
        ScheduleSnapshot.PlannedSlot prev = currentSchedule.mutateSlotMachine(
                chosen.getSerieId(), chosen.getPhase(), newMachine, chosen.getZoneNom());
        if (prev == null) return false;
        currentIntraZoneSpread = computeIntraZoneMachineSpread();
        double after = computeSpread();
        if (after + 1e-9 < before) {
            return true; // accept
        }
        // Revert
        currentSchedule.mutateSlotMachine(chosen.getSerieId(), chosen.getPhase(),
                prev.getMachineNom(), prev.getZoneNom());
        currentIntraZoneSpread = beforeIntra;
        return false;
    }

    /**
     * Slice 4 — Level-3 move: swap two adjacent slots in a machine's queue.
     * Adjacent swap preserves the total span of the two slots, so only their
     * own (start, end) shift — downstream slots are untouched. Hits
     * {@code currentBoxDurationAggregate}. Returns true on accept.
     *
     * <p>Guards: neither slot may have started executing. Both must be future.</p>
     */
    private boolean tryLevel3Move() {
        if (!cfg().isLevel3Enabled()) return false;
        if (currentSchedule == null || currentSchedule.size() < 2) return false;
        if (cfg().getBoxDurationWeight() <= 0 && cfg().getBoxDurationMaxWeight() <= 0) return false;

        Map<String, java.util.List<ScheduleSnapshot.PlannedSlot>> byMachine =
                currentSchedule.groupByMachine();
        List<String> machines = new ArrayList<>();
        for (Map.Entry<String, java.util.List<ScheduleSnapshot.PlannedSlot>> e : byMachine.entrySet()) {
            if (e.getValue().size() >= 2) machines.add(e.getKey());
        }
        if (machines.isEmpty()) return false;
        String machine = machines.get(ThreadLocalRandom.current().nextInt(machines.size()));
        java.util.List<ScheduleSnapshot.PlannedSlot> queue = byMachine.get(machine);
        List<Integer> eligible = new ArrayList<>();
        for (int i = 0; i < queue.size() - 1; i++) {
            ScheduleSnapshot.PlannedSlot left = queue.get(i);
            ScheduleSnapshot.PlannedSlot right = queue.get(i + 1);
            if (left.getPhase() == ScheduleSnapshot.Phase.COUPE
                    && right.getPhase() == ScheduleSnapshot.Phase.COUPE
                    && isMovableScheduledSlot(left)
                    && isMovableScheduledSlot(right)) {
                eligible.add(i);
            }
        }
        if (eligible.isEmpty()) return false;
        int lowerIdx = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
        ScheduleSnapshot.PlannedSlot a = queue.get(lowerIdx);
        ScheduleSnapshot.PlannedSlot b = queue.get(lowerIdx + 1);

        LocalDateTime now = LocalDateTime.now();
        if (a.getPlannedStart() == null || b.getPlannedStart() == null) return false;
        if (a.getPlannedStart().isBefore(now) || b.getPlannedStart().isBefore(now)) return false;

        double before = computeSpread();
        BoxDurationCalculator.Aggregate beforeAgg = currentBoxDurationAggregate;
        if (!currentSchedule.swapAdjacentInQueue(machine, lowerIdx)) return false;
        if (!coupePrecedenceOk(a.getSerieId()) || !coupePrecedenceOk(b.getSerieId())) {
            currentSchedule.swapAdjacentInQueue(machine, lowerIdx);
            return false;
        }
        currentBoxDurationAggregate = boxDurationCalculator.compute(currentSchedule, boxCountsBySequence);
        double after = computeSpread();
        if (after + 1e-9 < before) {
            return true; // accept
        }
        // Revert
        currentSchedule.swapAdjacentInQueue(machine, lowerIdx);
        currentBoxDurationAggregate = beforeAgg;
        return false;
    }

    private boolean isMovableScheduledSlot(ScheduleSnapshot.PlannedSlot slot) {
        return slot != null && movableSerieIdsBySnapshot.contains(slot.getSerieId());
    }

    private boolean coupePrecedenceOk(String serieId) {
        if (currentSchedule == null || serieId == null) return true;
        ScheduleSnapshot.PlannedSlot mat = currentSchedule.get(serieId, ScheduleSnapshot.Phase.MATELASSAGE);
        ScheduleSnapshot.PlannedSlot coupe = currentSchedule.get(serieId, ScheduleSnapshot.Phase.COUPE);
        if (mat == null || coupe == null || mat.getPlannedEnd() == null || coupe.getPlannedStart() == null) {
            return true;
        }
        return !coupe.getPlannedStart().isBefore(mat.getPlannedEnd());
    }

    /** Test-only access to the current candidate list (populated after buildSnapshot). */
    synchronized List<Candidate> getCandidates() {
        return new ArrayList<>(candidates);
    }

    public synchronized boolean start(LocalDate date, int shift, EngineMode mode,
                                   Integer durationSec, String startedByUserId) {
        if (state == EngineState.WARMING || state == EngineState.IMPROVING) {
            log.warn("Engine start ignored — already running: {}", state);
            return false;
        }
        doStart(mode, durationSec, startedByUserId, gen -> runLoop(date, shift, gen));
        return true;
    }

    /**
     * Start the engine for all active sequences (sequenceStatus = ACTIVE or null),
     * using the current wall-clock date/shift for zone resolution.
     */
    public synchronized boolean startActive(EngineMode mode, Integer durationSec, String startedByUserId) {
        if (state == EngineState.WARMING || state == EngineState.IMPROVING) {
            log.warn("Engine start ignored — already running: {}", state);
            return false;
        }
        doStart(mode, durationSec, startedByUserId, gen -> runLoopActive(gen));
        return true;
    }

    private void doStart(EngineMode mode, Integer durationSec, String startedByUserId, LoopFactory loopFactory) {
        shutdownExecutor();
        runGeneration++;
        final long myGeneration = runGeneration;
        this.mode = mode;
        // Force ALTERNATING mode regardless of request
        this.mode = EngineMode.ALTERNATING;
        this.currentPhase = EnginePhase.DISPATCH;
        this.phaseStartMs = System.currentTimeMillis();
        this.ordonnancementIteration = 0;
        this.fixedDurationSec = durationSec;
        this.stopRequested = false;
        this.iteration = 0;
        this.improvements = 0;
        this.noImprovementStreak = 0;
        this.lastBestImprovementIteration = 0;
        this.convergedFlag = false;
        this.lastImprovement = 0;
        this.bestSpread = Double.MAX_VALUE;
        this.bestStdDev = Double.MAX_VALUE;
        this.bestMedian = Double.MAX_VALUE;
        this.bestAssignment.clear();
        this.initialAssignment.clear();
        this.temperature = cfg().getTemperatureInitial();
        this.lastRebalanceIteration = 0;
        this.lastRandomizeIteration = 0;

        // Persist run row
        DispatchEngineRun run = new DispatchEngineRun();
        run.setStartedAt(LocalDateTime.now());
        run.setMode(DispatchEngineRun.Mode.valueOf(mode.name()));
        run.setDurationSec(durationSec);
        run.setStartedBy(startedByUserId != null ? startedByUserId : "SYSTEM");
        run.setFinalState(DispatchEngineRun.FinalState.STOPPED); // default until clean stop
        run.setIterations(0);
        run.setImprovements(0);
        run = runRepository.save(run);
        this.currentRunId = run.getId();

        transitionTo(EngineState.WARMING);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "dispatch-engine-" + myGeneration);
            t.setDaemon(true);
            return t;
        });
        this.loopFuture = executor.submit(() -> loopFactory.run(myGeneration));
    }

    public synchronized void pause() {
        if (state == EngineState.IMPROVING) {
            transitionTo(EngineState.PAUSED);
        }
    }

    public synchronized void resume() {
        if (state == EngineState.PAUSED) {
            transitionTo(EngineState.IMPROVING);
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    public synchronized void stop() {
        stopRequested = true;
        if (state == EngineState.IMPROVING || state == EngineState.PAUSED || state == EngineState.WARMING) {
            transitionTo(EngineState.STOPPED);
        }
        saveFinalRun();
        shutdownExecutor();
    }

    /**
     * Mark the engine for a snapshot rebuild on the next loop iteration.
     * Coalesced: rapid bursts of events produce exactly one rebuild.
     * No-op when the engine is not running.
     */
    @EventListener
    public void onSequenceAcceptanceChanged(SequenceAcceptanceChangedEvent event) {
        if (state == EngineState.IMPROVING || state == EngineState.WARMING) {
            rebuildRequested.set(true);
        }
    }

    @EventListener
    public void onZoneMachineToggled(ZoneMachineToggledEvent event) {
        if (state == EngineState.IMPROVING || state == EngineState.WARMING) {
            rebuildRequested.set(true);
        }
    }

    @EventListener
    public void onShiftZoneConfirmed(ShiftZoneConfirmedEvent event) {
        if (state == EngineState.IMPROVING || state == EngineState.WARMING) {
            rebuildRequested.set(true);
        }
    }

    // ------------------------------------------------------------------ query API

    @Transactional(readOnly = true)
    public Page<DispatchEngineRun> findRuns(Pageable pageable) {
        return runRepository.findAllByOrderByStartedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<DispatchEngineIndicatorSample> findSamples(Long runId) {
        return sampleRepository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<DispatchEngineRunSuggestion> findSuggestions(Long runId) {
        return suggestionRepository.findByRunId(runId);
    }

    @Transactional
    public int publishRun(Long runId, String byMatricule) {
        List<DispatchEngineRunSuggestion> suggestions = findSuggestions(runId);
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (DispatchEngineRunSuggestion s : suggestions) {
            String seq = s.getId() != null ? s.getId().getSequence() : null;
            String zone = s.getSuggestedZone();
            if (seq == null || zone == null) continue;
            CuttingRequest cr = cuttingRequestRepository.findBySequence(seq);
            if (cr == null) continue;
            cr.setDispatchedZone(zone);
            cr.setZoneAcceptanceStatus("PENDING");
            cr.setDispatchedAt(now);
            cr.setDispatchedBy(byMatricule);
            cuttingRequestRepository.save(cr);
            count++;
        }
        if (count > 0 && ordonnancementService != null) {
            ordonnancementService.invalidateTimelineCache();
        }
        return count;
    }

    // ------------------------------------------------------------------ loop

    private void runLoop(LocalDate date, int shift, long generation) {
        try {
            this.useActiveSnapshot = false;
            this.currentDate = date;
            this.currentShift = shift;
            buildSnapshot(date, shift);
            this.lastSnapshotLoadMs = System.currentTimeMillis();
            this.maxLoadedSerie = computeMaxSerieFromCandidates();
            greedyWarmStart();
            this.initialSpread = computeRawSpread();
            this.currentSpread = computeSpread();
            this.currentRawSpread = computeRawSpread();
            this.currentStdDev = computeStdDev();
            this.currentMedian = computeMedian();
            this.bestSpread = this.currentSpread;
            this.bestStdDev = this.currentStdDev;
            this.bestMedian = this.currentMedian;
            saveBestAssignment();
            rebuildSchedule();
            this.initialAssignment.clear();
            this.initialAssignment.putAll(this.bestAssignment);

            updateRunInitialSpread();
            transitionTo(EngineState.IMPROVING);
            this.loopStartMs = System.currentTimeMillis();
            this.lastBestImprovementIteration = 0;

            while (!stopRequested) {
                synchronized (lock) {
                    while (state == EngineState.PAUSED && !stopRequested) {
                        lock.wait(500);
                    }
                }
                if (stopRequested) break;
                if (state != EngineState.IMPROVING) break;

                if (!doLoopIteration()) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Engine loop interrupted");
        } catch (Exception e) {
            log.error("Engine loop crashed", e);
        } finally {
            synchronized (this) {
                if (generation == runGeneration && state != EngineState.STOPPED) {
                    state = EngineState.STOPPED;
                    saveFinalRun();
                    webSocketService.publishState(state, mode, currentRunId);
                }
            }
            if (generation == runGeneration) {
                shutdownExecutor();
            }
        }
    }

    private void runLoopActive(long generation) {
        try {
            this.useActiveSnapshot = true;
            com.lear.MGCMS.services.scheduling.ShiftClock.ShiftSlot loopSlot = shiftClock.currentSlot();
            this.currentDate = loopSlot.date;
            this.currentShift = loopSlot.shift;

            buildSnapshotForActive();
            this.lastSnapshotLoadMs = System.currentTimeMillis();
            this.maxLoadedSerie = computeMaxSerieFromCandidates();
            greedyWarmStart();
            this.initialSpread = computeRawSpread();
            this.currentSpread = computeSpread();
            this.currentRawSpread = computeRawSpread();
            this.currentStdDev = computeStdDev();
            this.currentMedian = computeMedian();
            this.bestSpread = this.currentSpread;
            this.bestStdDev = this.currentStdDev;
            this.bestMedian = this.currentMedian;
            saveBestAssignment();
            rebuildSchedule();
            this.initialAssignment.clear();
            this.initialAssignment.putAll(this.bestAssignment);

            updateRunInitialSpread();
            transitionTo(EngineState.IMPROVING);
            this.loopStartMs = System.currentTimeMillis();
            this.lastBestImprovementIteration = 0;

            while (!stopRequested) {
                synchronized (lock) {
                    while (state == EngineState.PAUSED && !stopRequested) {
                        lock.wait(500);
                    }
                }
                if (stopRequested) break;
                if (state != EngineState.IMPROVING) break;

                if (!doLoopIteration()) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Engine loop interrupted");
        } catch (Exception e) {
            log.error("Engine loop crashed", e);
        } finally {
            synchronized (this) {
                if (generation == runGeneration && state != EngineState.STOPPED) {
                    state = EngineState.STOPPED;
                    saveFinalRun();
                    webSocketService.publishState(state, mode, currentRunId);
                }
            }
            if (generation == runGeneration) {
                shutdownExecutor();
            }
        }
    }

    // ------------------------------------------------------------------ shared iteration logic

    /**
     * Single iteration of the engine loop, handling phase switching for
     * {@link EngineMode#ALTERNATING} mode.
     * @return false when the loop should exit (converged / duration exceeded)
     */
    private boolean doLoopIteration() {
        long iterStart = System.currentTimeMillis();

        // Fixed-duration timer check
        if (fixedDurationSec != null) {
            long elapsedMs = System.currentTimeMillis() - loopStartMs;
            if (elapsedMs >= fixedDurationSec * 1000L) {
                stopRequested = true;
                return false;
            }
        }

        // External rebuild signal — fired by event listeners (acceptance change,
        // machine toggle). Coalesced: many events between iterations trigger
        // exactly one rebuild here.
        if (rebuildRequested.compareAndSet(true, false)) {
            try {
                if (currentDate != null) {
                    if (useActiveSnapshot) {
                        buildSnapshotForActive();
                    } else {
                        buildSnapshot(currentDate, currentShift);
                    }
                    if (!bestAssignment.isEmpty()) {
                        restoreAssignment(bestAssignment);
                    }
                    currentSpread = computeSpread();
                    currentRawSpread = computeRawSpread();
                    currentStdDev = computeStdDev();
                    currentMedian = computeMedian();
                    lastSnapshotLoadMs = System.currentTimeMillis();
                    maxLoadedSerie = computeMaxSerieFromCandidates();
                    rebuildSchedule();
                    log.info("Engine snapshot rebuilt on event signal at iter {}", iteration);
                }
            } catch (Exception ex) {
                log.warn("Event-driven snapshot rebuild failed", ex);
            }
        }

        // Convergence throttle — the engine runs indefinitely, but when the
        // best cost has not improved for {@code convergenceIterations} we
        // slow the loop so we don't burn CPU spinning. A future event (chef
        // accept, machine toggle, etc.) sets `rebuildRequested` which forces
        // a fresh snapshot and resets `lastBestImprovementIteration`.
        int convThreshold = cfg().getConvergenceIterations();
        if (convThreshold > 0
                && iteration - lastBestImprovementIteration >= convThreshold
                && temperature <= cfg().getTemperatureMin() * 2.0) {
            if (!convergedFlag) {
                convergedFlag = true;
                log.info("Engine converged at iter {} — best spread {} stable for {} iters; throttling",
                        iteration, round2(bestSpread), convThreshold);
            }
            long sleepMs = cfg().getConvergedSleepMs();
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        } else if (convergedFlag) {
            // Left convergence — clear flag so UI shows IMPROVING again.
            convergedFlag = false;
        }

        // Phase switching for ALTERNATING mode
        if (mode == EngineMode.ALTERNATING) {
            long phaseElapsed = System.currentTimeMillis() - phaseStartMs;
            if (currentPhase == EnginePhase.DISPATCH && phaseElapsed >= cfg().getDispatchPhaseMs()) {
                currentPhase = EnginePhase.ORDONNANCEMENT;
                phaseStartMs = System.currentTimeMillis();
                ordonnancementIteration = 0;
                log.info("Engine switching to ORDONNANCEMENT phase (runId={})", currentRunId);
            } else if (currentPhase == EnginePhase.ORDONNANCEMENT && phaseElapsed >= cfg().getOrdonnancementPhaseMs()) {
                currentPhase = EnginePhase.DISPATCH;
                phaseStartMs = System.currentTimeMillis();
                // Rebuild snapshot with fresh DB state before returning to dispatch
                if (currentDate != null && shouldRebuildSnapshot()) {
                    try {
                        if (useActiveSnapshot) {
                            buildSnapshotForActive();
                        } else {
                            buildSnapshot(currentDate, currentShift);
                        }
                        // Restore best assignment instead of resetting to DB state
                        if (!bestAssignment.isEmpty()) {
                            restoreAssignment(bestAssignment);
                        }
                        // Recalculate metrics after snapshot rebuild
                        currentSpread = computeSpread();
                        currentRawSpread = computeRawSpread();
                        currentStdDev = computeStdDev();
                        currentMedian = computeMedian();
                        this.lastSnapshotLoadMs = System.currentTimeMillis();
                        this.maxLoadedSerie = computeMaxSerieFromCandidates();
                        rebuildSchedule();
                    } catch (Exception ex) {
                        log.warn("Snapshot rebuild failed during phase switch", ex);
                    }
                }
                // Reset streak counters so dispatch phase starts fresh
                noImprovementStreak = 0;
                lastBestImprovementIteration = iteration;
                log.info("Engine switching to DISPATCH phase (runId={})", currentRunId);
            }
        }

        // Simulated annealing: cool temperature every iteration
        if (temperature > cfg().getTemperatureMin()) {
            temperature *= cfg().getTemperatureCooling();
        }

        // Periodic rebalance: target the most imbalanced machine type
        if (iteration - lastRebalanceIteration >= cfg().getRebalanceEvery()) {
            doRebalanceMove();
            lastRebalanceIteration = iteration;
        }

        // Periodic randomization when stuck for a long time
        if (noImprovementStreak >= cfg().getRandomizeAfter()
                && iteration - lastRandomizeIteration >= cfg().getRandomizeAfter()) {
            doRandomizeSubset();
            lastRandomizeIteration = iteration;
            noImprovementStreak = noImprovementStreak / 2;
        }

        double prevBestSpread = bestSpread;
        boolean improved;
        if (mode == EngineMode.ALTERNATING && currentPhase == EnginePhase.ORDONNANCEMENT) {
            improved = doOrdonnancementIteration();
            iteration++;
            // During ordonnancement phase we don't track dispatch convergence
            // and we don't kick — just yield CPU and refresh snapshot periodically.
            if (iteration % cfg().getSampleEvery() == 0) {
                saveSample();
            }
            webSocketService.publishSample(currentRunId, iteration, currentRawSpread,
                    getMaxLoadPct(), getMinLoadPct(), currentStdDev, currentMedian);
            return true; // keep looping; phase switch handled at top
        }

        // Normal dispatch iteration (also used in DISPATCH phase of ALTERNATING)
        improved = doOneIteration();
        iteration++;

        double bestDelta = prevBestSpread - bestSpread;
        if (bestDelta > engineProperties.getOptimizer().getImprovementEpsilon()) {
            lastBestImprovementIteration = iteration;
            lastImprovement = bestDelta;
        }
        if (improved) {
            improvements++;
            noImprovementStreak = 0;
            temperature = Math.min(temperature * 1.2, cfg().getTemperatureInitial()); // reheat on improvement
        } else {
            if (saMoveApplied) {
                noImprovementStreak = Math.max(0, noImprovementStreak - 10);
                saMoveApplied = false;
            } else {
                noImprovementStreak++;
            }
        }

        if (iteration % cfg().getSampleEvery() == 0) {
            saveSample();
        }
        webSocketService.publishSample(currentRunId, iteration, currentRawSpread,
                getMaxLoadPct(), getMinLoadPct(), currentStdDev, currentMedian);

        // Refresh schedule + box-duration aggregate every BOX_DURATION_REFRESH_EVERY
        // iterations so the cost function has fresh KPI data without paying the
        // builder cost on every move. Only kicks in when a box-duration weight is set.
        if ((cfg().getBoxDurationWeight() > 0 || cfg().getBoxDurationMaxWeight() > 0)
                && iteration % BOX_DURATION_REFRESH_EVERY == 0) {
            try {
                rebuildSchedule();
                currentSpread = computeSpread();
                if (currentSpread < bestSpread) {
                    bestSpread = currentSpread;
                    bestStdDev = currentStdDev;
                    bestMedian = currentMedian;
                    saveBestAssignment();
                }
            } catch (Exception ex) {
                log.warn("Periodic box-duration refresh failed", ex);
            }
        }

        // Slice 3 / Slice 4 — opt-in move kinds. Each fires with its configured
        // probability per iteration. They mutate currentSchedule and the cached
        // aggregate cost terms, reverting cleanly when the cost would worsen.
        if (cfg().isLevel2Enabled()
                && ThreadLocalRandom.current().nextDouble() < cfg().getLevel2MoveProbability()) {
            try {
                if (tryLevel2Move()) {
                    currentSpread = computeSpread();
                    if (currentSpread < bestSpread) {
                        bestSpread = currentSpread;
                        bestStdDev = currentStdDev;
                        bestMedian = currentMedian;
                        saveBestAssignment();
                    }
                }
            } catch (Exception ex) {
                log.warn("Level-2 move attempt failed", ex);
            }
        }
        if (cfg().isLevel3Enabled()
                && ThreadLocalRandom.current().nextDouble() < cfg().getLevel3MoveProbability()) {
            try {
                if (tryLevel3Move()) {
                    currentSpread = computeSpread();
                    if (currentSpread < bestSpread) {
                        bestSpread = currentSpread;
                        bestStdDev = currentStdDev;
                        bestMedian = currentMedian;
                        saveBestAssignment();
                    }
                }
            } catch (Exception ex) {
                log.warn("Level-3 move attempt failed", ex);
            }
        }

        if (noImprovementStreak >= cfg().getKickAfter()) {
            doKick();
            noImprovementStreak = 0;
        }

        long iterMs = System.currentTimeMillis() - iterStart;
        if (iterMs > 200) {
            log.warn("Engine iteration {} took {} ms (phase={})", iteration, iterMs,
                    currentPhase != null ? currentPhase.name() : "N/A");
        }
        return true;
    }

    /**
     * Ordonnancement-phase iteration.
     * Rebuilds the snapshot periodically to keep scheduling data fresh,
     * then prioritises the oldest sequences (earliest dueDate) by trying
     * to place them in the least-loaded valid zone.
     */
    private boolean doOrdonnancementIteration() {
        ordonnancementIteration++;
        // Rebuild snapshot at most once every 10 seconds during ordonnancement
        // to avoid spending the whole phase reloading from the DB.
        long msSinceRebuild = System.currentTimeMillis() - lastSnapshotLoadMs;
        if (msSinceRebuild > 10_000 && currentDate != null && shouldRebuildSnapshot()) {
            try {
                if (useActiveSnapshot) {
                    buildSnapshotForActive();
                } else {
                    buildSnapshot(currentDate, currentShift);
                }
                // Restore best assignment instead of resetting to DB state
                if (!bestAssignment.isEmpty()) {
                    restoreAssignment(bestAssignment);
                }
                currentSpread = computeSpread();
                currentRawSpread = computeRawSpread();
                currentStdDev = computeStdDev();
                currentMedian = computeMedian();
                this.lastSnapshotLoadMs = System.currentTimeMillis();
                this.maxLoadedSerie = computeMaxSerieFromCandidates();
                rebuildSchedule();
            } catch (Exception ex) {
                log.warn("Snapshot refresh during ordonnancement phase failed", ex);
            }
        }

        // Due-date priority: sort candidates by oldest dueDate first,
        // then try to move each to its best zone (lowest load spread).
        int movesThisIter = 0;
        if (!candidates.isEmpty()) {
            List<Candidate> sorted = new ArrayList<>(candidates);
            sorted.sort((a, b) -> {
                if (a.dueDate == null && b.dueDate == null) return 0;
                if (a.dueDate == null) return 1; // null = less urgent
                if (b.dueDate == null) return -1;
                int cmp = a.dueDate.compareTo(b.dueDate);
                if (cmp != 0) return cmp;
                cmp = Integer.compare(shiftOrder(a.dueShift), shiftOrder(b.dueShift));
                if (cmp != 0) return cmp;
                // Same due date: longer boxCycleTime = more urgent
                return Double.compare(b.boxCycleTime, a.boxCycleTime);
            });

            int limit = Math.min(8, sorted.size());
            for (int i = 0; i < limit; i++) {
                Candidate cand = sorted.get(i);
                if (cand.possibleZones.size() < 2) continue;

                String bestZone = cand.currentZone;
                double bestScore = evaluateMove(cand, bestZone);

                for (String z : cand.possibleZones) {
                    if (z.equals(bestZone)) continue;
                    double sp = evaluateMove(cand, z);
                    if (sp < bestScore) {
                        bestScore = sp;
                        bestZone = z;
                    }
                }

                if (!bestZone.equals(cand.currentZone)) {
                    String origZone = cand.currentZone;
                    applyMove(cand, bestZone);
                    movesThisIter++;
                    webSocketService.publishSuggestion(currentRunId, cand.sequence,
                            origZone, bestZone);
                }
            }

            if (movesThisIter > 0) {
                currentSpread = computeSpread();
                currentRawSpread = computeRawSpread();
                currentStdDev = computeStdDev();
                currentMedian = computeMedian();
                if (currentSpread < bestSpread) {
                    bestSpread = currentSpread;
                    bestStdDev = currentStdDev;
                    bestMedian = currentMedian;
                    saveBestAssignment();
                }
                log.debug("Ordonnancement improvement at iter {} — {} moves, weightedSpread={}",
                        iteration, movesThisIter, round2(currentSpread));
            }
        }

        // Yield CPU briefly so we don't spin at 100 %
        try {
            Thread.sleep(20);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return movesThisIter > 0; // report improvement if any moves were made
    }

    private String computeMaxSerieFromCandidates() {
        String max = null;
        for (Candidate c : candidates) {
            String s = c.getSerie();
            if (s != null && (max == null || s.compareTo(max) > 0)) {
                max = s;
            }
        }
        return max;
    }

    /**
     * Rebuild the in-memory schedule from the current snapshot's rich serie
     * rows and the current best (sequence → zone) assignment.
     *
     * <p>No-op when the rich rows are not available (legacy date+shift path)
     * — the engine still optimizes load spread but the schedule remains
     * empty. The active path always populates {@link #snapshotRichSerieRows}.</p>
     */
    private void rebuildSchedule() {
        if (snapshotRichSerieRows.isEmpty() || scheduleBuilderService == null) {
            currentSchedule = ScheduleSnapshot.empty();
            snapshotSerieInputs.clear();
            movableSerieIdsBySnapshot.clear();
            if (useActiveSnapshot) {
                persistCurrentScheduleIfChanged();
            }
            return;
        }
        // Reuse cuttingTimeCalculator to produce per-serie minutes when not
        // already supplied — same source of truth the rest of the engine uses.
        List<CuttingTimeCalculator.SerieInput<SerieKey>> timeInputs = new ArrayList<>();
        for (Object[] sr : snapshotRichSerieRows) {
            String serieId = (String) sr[0];
            String sequence = (String) sr[1];
            String machine = (String) sr[2];
            Double tempsDeCoupe = sr[3] != null ? ((Number) sr[3]).doubleValue() : null;
            Integer nbrCouche = sr[4] != null ? ((Number) sr[4]).intValue() : null;
            String placement = (String) sr[5];
            if (machine == null || machine.isBlank()) continue;
            timeInputs.add(new CuttingTimeCalculator.SerieInput<>(
                    new SerieKey(sequence, serieId), placement, tempsDeCoupe, nbrCouche, machine));
        }
        Map<SerieKey, Double> minutesBySerie = cuttingTimeCalculator.resolveMinutesBatch(timeInputs);

        List<ScheduleBuilderService.SerieInput> inputs = new ArrayList<>(snapshotRichSerieRows.size());
        for (Object[] sr : snapshotRichSerieRows) {
            String serieId = (String) sr[0];
            String sequence = (String) sr[1];
            String machineType = (String) sr[2];
            Integer nbrCouche = sr[4] != null ? ((Number) sr[4]).intValue() : null;
            String statusCoupe = (String) sr[6];
            String statusMatelassage = (String) sr[7];
            String tableCoupe = (String) sr[8];
            String tableMatelassage = (String) sr[9];
            LocalDateTime dateDebutCoupe = (LocalDateTime) sr[10];
            LocalDateTime dateFinCoupe = (LocalDateTime) sr[11];
            LocalDateTime dateDebutMatelassage = (LocalDateTime) sr[12];
            LocalDateTime dateFinMatelassage = (LocalDateTime) sr[13];
            String partNumberMaterial = sr.length > 14 ? (String) sr[14] : null;
            Double longueur = sr.length > 15 && sr[15] != null ? ((Number) sr[15]).doubleValue() : null;

            // Skip Complete series — no remaining work to plan for.
            if ("Complete".equalsIgnoreCase(statusCoupe == null ? "" : statusCoupe.trim())) continue;
            if (machineType == null || machineType.isBlank()) continue;

            Double cuttingMinutes = minutesBySerie.get(new SerieKey(sequence, serieId));
            boolean frozen = (statusCoupe != null && !"Waiting".equalsIgnoreCase(statusCoupe.trim()))
                    || (statusMatelassage != null && !"Waiting".equalsIgnoreCase(statusMatelassage.trim()));

            LocalDate dueDate = null;
            String dueShift = null;
            // dueDate/dueShift are not in the rich projection; rely on the candidate map.
            for (Candidate c : candidates) {
                if (c.sequence.equals(sequence)) {
                    dueDate = c.dueDate;
                    dueShift = c.dueShift;
                    break;
                }
            }

            inputs.add(new ScheduleBuilderService.SerieInput(
                    serieId, sequence, machineType,
                    cuttingMinutes, longueur, nbrCouche, partNumberMaterial,
                    statusCoupe, statusMatelassage, dueDate, dueShift,
                    tableMatelassage, tableCoupe, frozen,
                    dateDebutMatelassage, dateFinMatelassage,
                    dateDebutCoupe, dateFinCoupe));
        }
        snapshotSerieInputs.clear();
        snapshotSerieInputs.addAll(inputs);
        movableSerieIdsBySnapshot.clear();
        for (ScheduleBuilderService.SerieInput input : snapshotSerieInputs) {
            if (input.isMovableWaiting() && input.serieId != null) {
                movableSerieIdsBySnapshot.add(input.serieId);
            }
        }

        Map<String, String> seqToZone = new HashMap<>();
        for (Candidate c : candidates) {
            seqToZone.put(c.sequence, c.currentZone);
        }
        currentSchedule = scheduleBuilderService.build(
                snapshotHorizon, snapshotSerieInputs, seqToZone, snapshotMachinesByZoneByType);

        // Load box counts (cached across iterations — reloaded each rebuild).
        loadBoxCounts(seqToZone.keySet());

        // Compute initial box-duration aggregate so cost terms have data to
        // work with as soon as the snapshot is alive.
        currentBoxDurationAggregate = boxDurationCalculator.compute(currentSchedule, boxCountsBySequence);

        // Slice 3 — intra-zone machine load spread. Skip the walk when disabled.
        currentIntraZoneSpread = cfg().getIntraZoneMachineLoadWeight() > 0
                ? computeIntraZoneMachineSpread()
                : 0.0;

        log.debug("Engine rebuildSchedule — {} serie inputs, {} planned slots, "
                + "{} sequences measured, mean box-dur {} min",
                snapshotSerieInputs.size(), currentSchedule.size(),
                currentBoxDurationAggregate.sequencesMeasured,
                round2(currentBoxDurationAggregate.meanMinutesPerBox));

        persistCurrentScheduleIfChanged();
    }

    private void persistCurrentScheduleIfChanged() {
        if (schedulePersistenceService == null || currentSchedule == null) return;
        try {
            Map<ScheduleSnapshot.Key, ScheduleSnapshot.PlannedSlot> slots = currentSchedule.copyOfSlots();
            int hash = Objects.hash(currentRunId, slots.size());
            for (ScheduleSnapshot.PlannedSlot slot : slots.values()) {
                hash = 31 * hash + Objects.hash(
                        slot.getSerieId(), slot.getSequenceId(), slot.getPhase(),
                        slot.getMachineNom(), slot.getZoneNom(),
                        slot.getPlannedStart(), slot.getPlannedEnd());
            }
            if (hash == lastPersistedScheduleHash) return;

            LocalDateTime plannedAt = LocalDateTime.now();
            List<EngineScheduleEntry> entries = new ArrayList<>(slots.size());
            for (ScheduleSnapshot.PlannedSlot slot : slots.values()) {
                EngineScheduleEntry.Phase phase =
                        EngineScheduleEntry.Phase.valueOf(slot.getPhase().name());
                entries.add(new EngineScheduleEntry(
                        slot.getSerieId(),
                        phase,
                        slot.getMachineNom(),
                        slot.getSequenceId(),
                        slot.getZoneNom(),
                        slot.getPlannedStart(),
                        slot.getPlannedEnd(),
                        currentRunId,
                        plannedAt));
            }
            schedulePersistenceService.replaceCurrentSchedule(entries);
            lastPersistedScheduleHash = hash;
            log.debug("Persisted current engine schedule — {} slots", entries.size());
        } catch (Exception ex) {
            log.warn("Failed to persist current engine schedule", ex);
        }
    }

    /**
     * Refresh the {@link #boxCountsBySequence} cache for the given sequences.
     * Called once per snapshot rebuild — box info rows are stable within a
     * shift, so we don't need to reload between iterations.
     */
    private void loadBoxCounts(java.util.Collection<String> sequences) {
        boxCountsBySequence.clear();
        if (sequences == null || sequences.isEmpty() || boxInfoRepository == null) return;
        List<String> seqList = new ArrayList<>(sequences);
        for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
            for (Object[] row : boxInfoRepository.countBoxesBySequences(batch)) {
                String seq = (String) row[0];
                Number count = (Number) row[1];
                if (seq != null && count != null) {
                    boxCountsBySequence.put(seq, count.intValue());
                }
            }
        }
    }

    private long lastShouldRebuildCheckMs = 0;
    private boolean lastShouldRebuildResult = false;

    private boolean shouldRebuildSnapshot() {
        if (rebuildRequested.get()) {
            // Don't clear here — the engine loop's compareAndSet at line ~662
            // owns the clear so the rebuild actually runs there. We just
            // report "yes, rebuild needed" to any other caller checking us.
            lastShouldRebuildResult = true;
            lastShouldRebuildCheckMs = System.currentTimeMillis();
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lastShouldRebuildCheckMs < 2000) {
            return lastShouldRebuildResult;
        }
        lastShouldRebuildCheckMs = now;
        // Check 1: any series updated since last load?
        LocalDateTime since = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(lastSnapshotLoadMs), ZoneId.systemDefault());
        Long changedCount = serieDataRepository.countChangedSince(since);
        // Check 2: any new series with serie > maxLoadedSerie?
        Long newMax = serieDataRepository.getMaxNserie();
        boolean hasNewSeries = (newMax != null && maxLoadedSerie != null
            && newMax > Long.parseLong(maxLoadedSerie));
        lastShouldRebuildResult = (changedCount != null && changedCount > 0) || hasNewSeries;
        return lastShouldRebuildResult;
    }

    // ------------------------------------------------------------------ snapshot

    void buildSnapshot(LocalDate date, int shift) {
        long startMs = System.currentTimeMillis();
        // Reset captured rich rows so the schedule builder doesn't reuse a
        // stale active-path payload. Legacy path produces a minimal schedule
        // because the 6-col projection lacks status/timestamps.
        snapshotRichSerieRows.clear();
        // Legacy date+shift path — keep simple zone routing (no per-serie SHARED
        // overflow); active path supplies the rich routing map for the new
        // SHARED-aware logic.
        buildSnapshotInternal(date, shift,
                cuttingRequestRepository.findAllLight(date, String.valueOf(shift)),
                serieDataRepository.findStartedSequenceIdsForDateShift(date, String.valueOf(shift)),
                serieDataRepository.findSeriesByDateShiftLight(date, String.valueOf(shift)),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyList());
        log.debug("buildSnapshot(date,shift) took {} ms", System.currentTimeMillis() - startMs);
    }

    private static final int SQL_BATCH_SIZE = 2000;

    private void buildSnapshotForActive() {
        long startMs = System.currentTimeMillis();
        com.lear.MGCMS.services.scheduling.ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        LocalDate nowDate = slot.date;
        int nowShift = slot.shift;
        snapshotHorizon = LocalDateTime.now();
        List<Object[]> requestRows = cuttingRequestRepository.findActiveDueOnOrBeforeLight(
                nowDate, String.valueOf(nowShift));
        List<String> sequences = new ArrayList<>();
        for (Object[] r : requestRows) {
            sequences.add((String) r[0]);
        }

        // Rich light projection — gives us statusCoupe + tableCoupe per serie so
        // LockResolver can detect implicit locks (cutting started on a STRICT
        // zone's table). Replaces the prior startedIds + completeIds path.
        List<Object[]> serieRowsRich = new ArrayList<>();
        if (!sequences.isEmpty()) {
            for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
                List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
                serieRowsRich.addAll(serieDataRepository.findLiveChargeSeriesBySequences(batch));
            }
        }
        // Keep a reference for {@link #rebuildSchedule()} to avoid a second query.
        snapshotRichSerieRows.clear();
        snapshotRichSerieRows.addAll(serieRowsRich);
        // Slice 3 lookup: serieId → machineType. Rebuilt every snapshot so the
        // Level-2 move can constrain target machines to the same MT in O(1).
        serieMtBySnapshot.clear();
        for (Object[] sr : serieRowsRich) {
            if (sr.length >= 3 && sr[0] != null && sr[2] != null) {
                serieMtBySnapshot.put((String) sr[0], (String) sr[2]);
            }
        }

        // Group rich rows by sequence; collect non-Waiting tableCoupe values.
        // Build the engine's existing 6-column shape from the same rows so we
        // don't issue a second query.
        Map<String, List<LockResolver.SerieLockInput>> lockInputsBySeq = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> physicalTablesBySeq = new LinkedHashMap<>();
        Set<String> startedSequences = new HashSet<>();
        Set<String> tableNoms = new LinkedHashSet<>();
        List<Object[]> serieRows = new ArrayList<>(serieRowsRich.size());
        for (Object[] sr : serieRowsRich) {
            String serieId = (String) sr[0];
            String sequence = (String) sr[1];
            String machine = (String) sr[2];
            Double tempsDeCoupe = sr[3] != null ? ((Number) sr[3]).doubleValue() : null;
            Integer nbrCouche = sr[4] != null ? ((Number) sr[4]).intValue() : null;
            String placement = (String) sr[5];
            String statusCoupe = (String) sr[6];
            String statusMatelassage = (String) sr[7];
            String tableCoupe = (String) sr[8];
            String tableMatelassage = (String) sr[9];
            LocalDateTime dateDebutCoupe = (LocalDateTime) sr[10];

            // Engine load math currently ignores Complete series — preserve that.
            if (!"Complete".equalsIgnoreCase(statusCoupe == null ? "" : statusCoupe.trim())) {
                String partNumberMaterial = sr.length > 14 ? (String) sr[14] : null;
                Double longueur = sr.length > 15 && sr[15] != null ? ((Number) sr[15]).doubleValue() : null;
                serieRows.add(new Object[]{serieId, sequence, machine, tempsDeCoupe, nbrCouche, placement, partNumberMaterial, longueur});
            }
            lockInputsBySeq.computeIfAbsent(sequence, k -> new ArrayList<>())
                    .add(new LockResolver.SerieLockInput(serieId, statusCoupe, tableCoupe, dateDebutCoupe));
            if (isNonWaitingStatus(statusCoupe) || isNonWaitingStatus(statusMatelassage)) {
                startedSequences.add(sequence);
            }
            if (tableCoupe != null && !tableCoupe.trim().isEmpty()
                    && statusCoupe != null && !"Waiting".equalsIgnoreCase(statusCoupe.trim())) {
                tableNoms.add(tableCoupe);
                physicalTablesBySeq.computeIfAbsent(sequence, k -> new LinkedHashSet<>()).add(tableCoupe);
            }
            if (tableMatelassage != null && !tableMatelassage.trim().isEmpty()
                    && statusMatelassage != null && !"Waiting".equalsIgnoreCase(statusMatelassage.trim())) {
                tableNoms.add(tableMatelassage);
                physicalTablesBySeq.computeIfAbsent(sequence, k -> new LinkedHashSet<>()).add(tableMatelassage);
            }
        }

        // Table → STRICT/SHARED zone map (one batched query).
        Map<String, LockResolver.TableZoneInfo> tableToZone = loadTableZoneMap(tableNoms);

        // Apply LockResolver per sequence → effectiveZoneBySeq + lockedSequences.
        Map<String, String> effectiveZoneBySeq = new HashMap<>();
        Set<String> lockedSequences = new HashSet<>();
        for (Object[] r : requestRows) {
            String seq = (String) r[0];
            String dispatchedZone = (String) r[1];
            String accStatus = (String) r[2];
            List<LockResolver.SerieLockInput> inputs = lockInputsBySeq.getOrDefault(seq, java.util.Collections.emptyList());
            Optional<LockResolver.LockResult> lock = LockResolver.resolve(
                    dispatchedZone, accStatus, inputs, tableToZone);
            if (lock.isPresent()) {
                effectiveZoneBySeq.put(seq, lock.get().getLockZoneNom());
                lockedSequences.add(seq);
            } else {
                String physicalZone = resolvePhysicalZone(physicalTablesBySeq.get(seq), tableToZone);
                if (physicalZone != null) {
                    effectiveZoneBySeq.put(seq, physicalZone);
                    lockedSequences.add(seq);
                }
            }
        }
        lockedSequences.addAll(startedSequences);

        // Active-zones index used by the per-serie target routing (engine must
        // agree with LiveChargeService on where each serie's load lands).
        List<Zone> allActiveZones = zoneRepository.findAllActive();
        Map<String, Map<String, Set<String>>> machinesByZoneByType = new HashMap<>();
        List<String> zoneRoutingOrder = new ArrayList<>();
        for (Zone z : allActiveZones) {
            List<Object[]> rowsZT = productionTableRepository.findMachinesWithTypeInZone(z.getNom());
            Set<String> upMachines = activeMachineResolver.activeMachines(nowDate, nowShift, z.getNom());
            Map<String, Set<String>> mbt = new HashMap<>();
            for (Object[] row : rowsZT) {
                String typeName = (String) row[1];
                String machineNom = (String) row[0];
                if (typeName == null) continue;
                if (machineNom == null || !upMachines.contains(machineNom)) continue;
                mbt.computeIfAbsent(typeName, t -> new HashSet<>()).add(machineNom);
            }
            machinesByZoneByType.put(z.getNom(), mbt);
        }
        for (Zone z : allActiveZones) if (z.getCategory() == Zone.Category.SHARED) zoneRoutingOrder.add(z.getNom());
        for (Zone z : allActiveZones) if (z.getCategory() != Zone.Category.SHARED) zoneRoutingOrder.add(z.getNom());

        buildSnapshotInternal(nowDate, nowShift, requestRows,
                new ArrayList<>(lockedSequences), serieRows, effectiveZoneBySeq,
                machinesByZoneByType, zoneRoutingOrder);
        log.debug("buildSnapshotForActive() took {} ms — sequences={}, serieRows={}",
                System.currentTimeMillis() - startMs, sequences.size(), serieRows.size());
    }

    /**
     * Batch resolve {@code tableNom → ZoneInfo}. Called once per snapshot.
     * Returns an empty map when {@code tableNoms} is empty.
     */
    private Map<String, LockResolver.TableZoneInfo> loadTableZoneMap(Set<String> tableNoms) {
        if (tableNoms.isEmpty()) return java.util.Collections.emptyMap();
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

    private String resolvePhysicalZone(Set<String> tableNoms,
                                       Map<String, LockResolver.TableZoneInfo> tableToZone) {
        if (tableNoms == null || tableNoms.isEmpty() || tableToZone == null) return null;
        for (String table : tableNoms) {
            LockResolver.TableZoneInfo info = tableToZone.get(table);
            if (info != null && info.zoneNom != null) {
                return info.zoneNom;
            }
        }
        return null;
    }

    private boolean isNonWaitingStatus(String status) {
        return status != null && !status.trim().isEmpty()
                && !"Waiting".equalsIgnoreCase(status.trim());
    }

    private void buildSnapshotInternal(LocalDate date, int shift,
                                       List<Object[]> requestRows,
                                       List<String> frozenSequenceIds,
                                       List<Object[]> serieRows,
                                       Map<String, String> effectiveZoneBySeq,
                                       Map<String, Map<String, Set<String>>> machinesByZoneByType,
                                       List<String> zoneRoutingOrder) {
        capacities.clear();
        baselineLoads.clear();
        pendingLoads.clear();
        candidates.clear();
        if (materialAvailabilityChecker != null) {
            materialAvailabilityChecker.clearSnapshotCache();
        }

        List<Zone> strictZones = zoneRepository.findAllActive().stream()
                .filter(z -> z.getCategory() != Zone.Category.SHARED)
                .collect(Collectors.toList());

        // Compute capacity per (zone, machineType) using ActiveMachineResolver,
        // which already implements: chef confirmation if present, else M-only
        // fallback (status M or null = active). When neither produces a machine,
        // active = 0 — the zone is effectively closed for this shift.
        for (Zone z : strictZones) {
            List<Object[]> rows = productionTableRepository.findMachinesWithTypeInZone(z.getNom());
            Set<String> upMachines = activeMachineResolver.activeMachines(date, shift, z.getNom());
            Map<String, Set<String>> typeToMachines = new HashMap<>();
            for (Object[] row : rows) {
                String machineNom = (String) row[0];
                String typeName = (String) row[1];
                if (typeName == null) continue;
                typeToMachines.computeIfAbsent(typeName, t -> new HashSet<>()).add(machineNom);
            }
            for (Map.Entry<String, Set<String>> e : typeToMachines.entrySet()) {
                String type = e.getKey();
                Set<String> machines = e.getValue();
                int active = 0;
                if (!upMachines.isEmpty()) {
                    Set<String> intersect = new HashSet<>(machines);
                    intersect.retainAll(upMachines);
                    active = intersect.size();
                }
                // else: no chef confirmation AND no M-only machine — zone closed.
                String zk = zKey(z.getNom(), type);
                double eff = lookupEfficience(date, shift, type);
                double shiftMin = shiftProperties.getDurationMinutes() > 0
                        ? shiftProperties.getDurationMinutes() : DEFAULT_SHIFT_MINUTES;
                double cap = active * shiftMin * (eff / 100.0);
                capacities.put(zk, cap);
            }
        }

        // Load requests and series
        Map<String, RequestLight> requestBySeq = new LinkedHashMap<>();
        for (Object[] r : requestRows) {
            LocalDate dueDate = r.length > 5 && r[5] instanceof LocalDate ? (LocalDate) r[5] : null;
            String dueShift = r.length > 6 && r[6] != null ? String.valueOf(r[6]) : null;
            requestBySeq.put((String) r[0],
                    new RequestLight((String) r[0], (String) r[1], (String) r[2],
                            r[3] != null && Boolean.TRUE.equals(r[3]), (String) r[4], dueDate, dueShift));
        }

        // Sequences with any started serie (statusMatelassage or statusCoupe
        // != Waiting) OR any Complete serie are frozen — the engine must never move them.
        Set<String> frozenSequences = new HashSet<>(frozenSequenceIds);

        // Build cutting time inputs
        List<CuttingTimeCalculator.SerieInput<SerieKey>> timeInputs = new ArrayList<>();
        Map<String, List<SerieRow>> seriesBySequence = new LinkedHashMap<>();
        for (Object[] sr : serieRows) {
            String serieId = (String) sr[0];
            String sequence = (String) sr[1];
            String machineType = (String) sr[2];
            Double tempsDeCoupe = sr[3] != null ? ((Number) sr[3]).doubleValue() : null;
            Integer nbrCouche = sr[4] != null ? ((Number) sr[4]).intValue() : null;
            String placement = (String) sr[5];
            String partNumberMaterial = sr.length > 6 ? (String) sr[6] : null;
            Double longueur = sr.length > 7 && sr[7] != null ? ((Number) sr[7]).doubleValue() : null;

            if (machineType == null || machineType.trim().isEmpty()) continue;
            RequestLight req = requestBySeq.get(sequence);
            if (req == null) continue;

            SerieKey k = new SerieKey(sequence, serieId);
            seriesBySequence.computeIfAbsent(sequence, s -> new ArrayList<>())
                    .add(new SerieRow(k, machineType, tempsDeCoupe, nbrCouche, placement, partNumberMaterial, longueur));
            timeInputs.add(new CuttingTimeCalculator.SerieInput<>(
                    k, placement, tempsDeCoupe, nbrCouche, machineType));
        }

        Map<SerieKey, Double> minutesBySerie = cuttingTimeCalculator.resolveMinutesBatch(timeInputs);

        // Aggregate per sequence per machine type
        for (Map.Entry<String, List<SerieRow>> e : seriesBySequence.entrySet()) {
            String sequence = e.getKey();
            RequestLight req = requestBySeq.get(sequence);
            if (req == null) continue;

            // REJECTED — chef said no. The engine must not propose a new zone for
            // these; they need explicit re-dispatch via the chef's redispatch flow.
            if ("REJECTED".equalsIgnoreCase(req.zoneAcceptanceStatus)) continue;

            Map<String, Double> loadByType = new HashMap<>();
            // Lower-bound estimate of box cycle time: longest single serie's
            // minutes. Real cycle time = max(estimatedFinCoupe) − min(estimatedDebutCoupe),
            // which requires a schedule the engine does not produce. The previous
            // implementation summed minutes (an upper bound that is also zone-
            // invariant); longest-single is at least directionally meaningful and
            // strictly ≤ the schedule-derived value.
            double seqBoxCycle = 0.0;
            Set<String> seqRefTissus = new HashSet<>();
            for (SerieRow sr : e.getValue()) {
                double min = minutesBySerie.getOrDefault(sr.key, 0.0);
                loadByType.merge(sr.machineType, min, Double::sum);
                if (min > seqBoxCycle) seqBoxCycle = min;
                if (sr.partNumberMaterial != null && !sr.partNumberMaterial.isBlank()) {
                    seqRefTissus.add(sr.partNumberMaterial.trim());
                }
            }

            // Frozen: pinned by chef, accepted by chef, locked by LockResolver
            // (ACCEPTED or implicit-table-STRICT — see {@link LockResolver}), or
            // already started on the floor (legacy date+shift path only).
            boolean frozen = req.pinnedByChef
                    || "ACCEPTED".equalsIgnoreCase(req.zoneAcceptanceStatus)
                    || frozenSequences.contains(sequence);
            // For locked sequences, prefer the LockResolver's effective zone so
            // implicit-table locks land their load in the physical zone — not
            // the (potentially stale) dispatchedZone DB field.
            String currentZone = effectiveZoneBySeq != null
                    ? effectiveZoneBySeq.getOrDefault(sequence, req.dispatchedZone)
                    : req.dispatchedZone;

            if (frozen) {
                // Per-serie routing: each MT's load lands in the zone that
                // physically hosts it (owner first, else SHARED, else other
                // STRICT). Falls back to currentZone when nothing hosts it
                // (treated as "UNKNOWN" load; spread metric ignores cells
                // absent from capacities).
                for (Map.Entry<String, Double> le : loadByType.entrySet()) {
                    String mt = le.getKey();
                    String target = LiveChargeService.resolveTargetZone(
                            currentZone, mt, machinesByZoneByType, zoneRoutingOrder);
                    if (target == null) target = currentZone != null ? currentZone : "UNKNOWN";
                    baselineLoads.merge(zKey(target, mt), le.getValue(), Double::sum);
                }
            } else {
                // Possible STRICT zones — relaxed to "hosts ≥1 of the seq's MTs".
                // SHARED-only MTs (e.g. LASER-DXF) follow the per-serie router
                // automatically; the engine balances the STRICT-hostable part.
                List<String> possible = new ArrayList<>();
                for (Zone z : strictZones) {
                    boolean canHost = false;
                    for (String mt : loadByType.keySet()) {
                        String zk = zKey(z.getNom(), mt);
                        if (capacities.containsKey(zk) && capacities.get(zk) > 0) {
                            canHost = true;
                            break;
                        }
                    }
                    if (canHost) possible.add(z.getNom());
                }
                if (possible.isEmpty()) continue; // unassignable — skip

                if (currentZone == null || !possible.contains(currentZone)) {
                    // Prefer the CR's zoneFix (preferredZoneNom) when it's a valid
                    // STRICT zone for this sequence — that's what the user sees as
                    // the "default" zone in the live view, so the engine should
                    // start there instead of an arbitrary alphabetical first.
                    if (req.preferredZoneNom != null && possible.contains(req.preferredZoneNom)) {
                        currentZone = req.preferredZoneNom;
                    } else {
                        currentZone = possible.get(0);
                    }
                }

                String seqMaxSerie = null;
                for (SerieRow sr : e.getValue()) {
                    if (sr.key.serieId != null && (seqMaxSerie == null || sr.key.serieId.compareTo(seqMaxSerie) > 0)) {
                        seqMaxSerie = sr.key.serieId;
                    }
                }
                Candidate cand = new Candidate(sequence, currentZone, possible, loadByType,
                        seqBoxCycle, req.dueDate, req.dueShift, seqRefTissus, seqMaxSerie);
                candidates.add(cand);
                // Initial pending load — routed per serie via the same rule.
                for (Map.Entry<String, Double> le : loadByType.entrySet()) {
                    String mt = le.getKey();
                    String target = LiveChargeService.resolveTargetZone(
                            currentZone, mt, machinesByZoneByType, zoneRoutingOrder);
                    if (target == null) continue;
                    pendingLoads.merge(zKey(target, mt), le.getValue(), Double::sum);
                }
            }
        }

        // Cache the routing maps so applyMove / evaluateMove can use the same
        // per-serie router on every move evaluation.
        this.snapshotMachinesByZoneByType = machinesByZoneByType;
        this.snapshotZoneRoutingOrder = zoneRoutingOrder;

        // Pre-check material availability for all candidate-zone pairs so
        // evaluateMove() does not hit the DB on every evaluation.
        materialAvailabilityBySeqAndZone.clear();
        if (materialAvailabilityChecker != null) {
            for (Candidate cand : candidates) {
                if (cand.refTissus.isEmpty()) continue;
                Map<String, Integer> byZone = new HashMap<>();
                for (String zone : cand.possibleZones) {
                    byZone.put(zone, materialAvailabilityChecker.countNotInZone(cand.refTissus, zone));
                }
                materialAvailabilityBySeqAndZone.put(cand.sequence, byZone);
            }
        }

        // Recompute constant cycle metrics after candidates change
        recomputeCachedCycleMetrics();

        // Clear target-zone cache since routing maps may have changed
        resolveTargetZoneCache.clear();
    }

    void greedyWarmStart() {
        // Place each candidate in the zone that minimizes the resulting spread
        for (Candidate cand : candidates) {
            String bestZone = cand.currentZone;
            double bestSpread = evaluateMove(cand, bestZone);
            for (String z : cand.possibleZones) {
                if (z.equals(bestZone)) continue;
                double sp = evaluateMove(cand, z);
                if (sp < bestSpread) {
                    bestSpread = sp;
                    bestZone = z;
                }
            }
            if (!bestZone.equals(cand.currentZone)) {
                applyMove(cand, bestZone);
            }
        }
    }

    // ------------------------------------------------------------------ perturbation

    private boolean doOneIteration() {
        long startMs = System.currentTimeMillis();
        if (candidates.isEmpty()) return false;

        List<Candidate> movable = new ArrayList<>(candidates.size());
        for (Candidate c : candidates) {
            if (c.possibleZones.size() >= 2) movable.add(c);
        }
        if (movable.isEmpty()) return false;

        double before = currentMoveObjective();

        // --- Phase 1: strict improving move ---
        // When stuck, probe multiple candidates to find the best improving move
        int probeCount = (noImprovementStreak > cfg().getMultiCandidateThreshold())
                ? Math.min(8, movable.size()) : 1;

        Candidate bestCand = null;
        String bestZone = null;
        double bestScore = before;

        for (int p = 0; p < probeCount; p++) {
            Candidate cand = movable.get(ThreadLocalRandom.current().nextInt(movable.size()));
            for (String z : cand.possibleZones) {
                if (z.equals(cand.currentZone)) continue;
                double sp = evaluateMove(cand, z);
                if (sp < bestScore) {
                    bestScore = sp;
                    bestZone = z;
                    bestCand = cand;
                }
            }
        }

        if (bestCand != null && bestZone != null && !bestZone.equals(bestCand.currentZone)) {
            String origZone = bestCand.currentZone;
            applyMove(bestCand, bestZone);
            updateMetricsAfterMove();
            log.debug("Dispatch improvement at iter {} — weightedSpread={} byType=[{}]",
                    iteration, round2(currentSpread), formatPerMachineTypeSpreads());
            webSocketService.publishSuggestion(currentRunId, bestCand.sequence,
                    origZone, bestZone);
            return true;
        }

        // --- Phase 2: Simulated Annealing — accept worsening single move ---
        if (temperature > cfg().getTemperatureMin() && !movable.isEmpty()) {
            Candidate cand = movable.get(ThreadLocalRandom.current().nextInt(movable.size()));
            List<String> otherZones = new ArrayList<>(cand.possibleZones);
            otherZones.remove(cand.currentZone);
            if (!otherZones.isEmpty()) {
                Collections.shuffle(otherZones, ThreadLocalRandom.current());
                for (int t = 0; t < Math.min(3, otherZones.size()); t++) {
                    String z = otherZones.get(t);
                    double sp = evaluateMove(cand, z);
                    if (acceptWorseMove(before, sp)) {
                        String origZone = cand.currentZone;
                        applyMove(cand, z);
                        updateMetricsAfterMove();
                        saMoveApplied = true;
                        log.debug("SA reassign at iter {} — accepted worse move {}->{} (before={} after={}) temp={}",
                                iteration, origZone, z, round2(before), round2(sp), round2(temperature));
                        webSocketService.publishSuggestion(currentRunId, cand.sequence,
                                origZone, z);
                        return false;
                    }
                }
            }
        }

        // --- Phase 3: Swap attempt (strict or SA) ---
        if (movable.size() >= 2) {
            Candidate c1 = movable.get(ThreadLocalRandom.current().nextInt(movable.size()));
            Candidate c2 = movable.get(ThreadLocalRandom.current().nextInt(movable.size()));
            if (c1 != c2) {
                final String z1 = c1.currentZone;
                final String z2 = c2.currentZone;
                if (!z1.equals(z2) && c1.possibleZones.contains(z2) && c2.possibleZones.contains(z1)) {
                    applyMove(c1, z2);
                    applyMove(c2, z1);
                    double after = currentMoveObjective();
                    if (after < before) {
                        updateMetricsAfterMove();
                        log.debug("Swap improvement at iter {} — weightedSpread={} byType=[{}]",
                                iteration, round2(currentSpread), formatPerMachineTypeSpreads());
                        webSocketService.publishSuggestion(currentRunId, c1.sequence,
                                z1, z2);
                        return true;
                    } else if (acceptWorseMove(before, after)) {
                        updateMetricsAfterMove();
                        saMoveApplied = true;
                        return false;
                    } else {
                        applyMove(c1, z1);
                        applyMove(c2, z2);
                    }
                }
            }
        }

        // --- Phase 4: Block-rotate (strict or SA) ---
        if (movable.size() >= 3) {
            int j1 = ThreadLocalRandom.current().nextInt(movable.size());
            int j2 = ThreadLocalRandom.current().nextInt(movable.size());
            int j3 = ThreadLocalRandom.current().nextInt(movable.size());
            if (j1 != j2 && j2 != j3 && j1 != j3) {
                Candidate c1 = movable.get(j1);
                Candidate c2 = movable.get(j2);
                Candidate c3 = movable.get(j3);
                String z1 = c1.currentZone;
                String z2 = c2.currentZone;
                String z3 = c3.currentZone;
                if (!z1.equals(z2) && !z2.equals(z3) && !z1.equals(z3)
                        && c1.possibleZones.contains(z2)
                        && c2.possibleZones.contains(z3)
                        && c3.possibleZones.contains(z1)) {
                    applyMove(c1, z2);
                    applyMove(c2, z3);
                    applyMove(c3, z1);
                    double after = currentMoveObjective();
                    if (after < before) {
                        updateMetricsAfterMove();
                        log.debug("Block-rotate improvement at iter {} — weightedSpread={} byType=[{}]",
                                iteration, round2(currentSpread), formatPerMachineTypeSpreads());
                        webSocketService.publishSuggestion(currentRunId, c1.sequence, z1, z2);
                        webSocketService.publishSuggestion(currentRunId, c2.sequence, z2, z3);
                        webSocketService.publishSuggestion(currentRunId, c3.sequence, z3, z1);
                        return true;
                    } else if (acceptWorseMove(before, after)) {
                        updateMetricsAfterMove();
                        saMoveApplied = true;
                        return false;
                    } else {
                        applyMove(c1, z1);
                        applyMove(c2, z2);
                        applyMove(c3, z3);
                    }
                }
            }
        }

        long elapsedMs = System.currentTimeMillis() - startMs;
        if (elapsedMs > 100) {
            log.warn("Engine doOneIteration() took {} ms — candidates={}", elapsedMs, candidates.size());
        }
        return false;
    }

    private void doKick() {
        if (candidates.isEmpty()) return;

        // Build load map per zone|type
        Map<String, Double> loadPct = new HashMap<>();
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
            double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
            loadPct.put(ce.getKey(), ((base + pend) / cap) * 100.0);
        }

        // Sort zone|types by load
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(loadPct.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        List<String> overloaded = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            overloaded.add(sorted.get(i).getKey());
        }

        int movesDone = 0;

        // Smart moves: move from highest-load zones to lowest-load zones
        outer:
        for (String overKey : overloaded) {
            if (movesDone >= cfg().getKickMoves()) break;
            int pipe = overKey.lastIndexOf('|');
            String overZone = pipe >= 0 ? overKey.substring(0, pipe) : overKey;
            String overType = pipe >= 0 ? overKey.substring(pipe + 1) : null;

            for (Candidate cand : candidates) {
                if (movesDone >= cfg().getKickMoves()) break outer;
                if (cand.possibleZones.size() < 2) continue;
                Double load = cand.loadByType.get(overType);
                if (load == null || load <= 0) continue;
                String currentTarget = cachedResolveTargetZone(cand.currentZone, overType);
                if (!overZone.equals(currentTarget)) continue;

                // Find the best underloaded zone this candidate can go to
                String bestTarget = null;
                double bestLoad = Double.POSITIVE_INFINITY;
                for (String z : cand.possibleZones) {
                    String target = cachedResolveTargetZone(z, overType);
                    if (target == null) continue;
                    double lp = loadPct.getOrDefault(zKey(target, overType), 0.0);
                    if (lp < bestLoad) {
                        bestLoad = lp;
                        bestTarget = z;
                    }
                }

                if (bestTarget != null && !bestTarget.equals(cand.currentZone)) {
                    applyMove(cand, bestTarget);
                    movesDone++;
                    // Update load pct for subsequent moves
                    for (Map.Entry<String, Double> ce : capacities.entrySet()) {
                        double cap = ce.getValue();
                        if (cap <= 0) continue;
                        double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
                        double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
                        loadPct.put(ce.getKey(), ((base + pend) / cap) * 100.0);
                    }
                }
            }
        }

        // Fallback to random moves for any remaining kick budget
        List<Candidate> movable = new ArrayList<>();
        for (Candidate c : candidates) {
            if (c.possibleZones.size() >= 2) movable.add(c);
        }
        while (movesDone < cfg().getKickMoves() && !movable.isEmpty()) {
            Candidate cand = movable.get(ThreadLocalRandom.current().nextInt(movable.size()));
            List<String> zones = new ArrayList<>(cand.possibleZones);
            zones.remove(cand.currentZone);
            if (!zones.isEmpty()) {
                applyMove(cand, zones.get(ThreadLocalRandom.current().nextInt(zones.size())));
                movesDone++;
            } else {
                movable.remove(cand);
            }
        }

        currentSpread = computeSpread();
        currentRawSpread = computeRawSpread();
        currentStdDev = computeStdDev();
        currentMedian = computeMedian();

        if (currentSpread < bestSpread) {
            bestSpread = currentSpread;
            bestStdDev = currentStdDev;
            bestMedian = currentMedian;
            saveBestAssignment();
            log.debug("Kick improved best at iter {} — weightedSpread={} byType=[{}]",
                    iteration, round2(currentSpread), formatPerMachineTypeSpreads());
        }
        // Do NOT restore to best — the kick is meant to escape local optima.
        // If the kicked state is worse, SA may still explore from it.
        log.info("Kick applied at iter {} — {} moves, spread={} (best={}) temp={}",
                iteration, movesDone, round2(currentSpread), round2(bestSpread), round2(temperature));
    }

    private boolean acceptWorseMove(double before, double after) {
        if (after <= before) return false; // not worse, or better
        if (temperature <= cfg().getTemperatureMin()) return false;
        double delta = after - before;
        double prob = Math.exp(-delta / temperature);
        boolean accepted = ThreadLocalRandom.current().nextDouble() < prob;
        if (accepted) {
            log.debug("SA accepted worse move: delta={} prob={} temp={}",
                    round2(delta), round2(prob), round2(temperature));
        }
        return accepted;
    }

    private void updateMetricsAfterMove() {
        currentSpread = computeSpread();
        currentRawSpread = computeRawSpread();
        currentStdDev = computeStdDev();
        currentMedian = computeMedian();
        if (currentSpread < bestSpread) {
            bestSpread = currentSpread;
            bestStdDev = currentStdDev;
            bestMedian = currentMedian;
            saveBestAssignment();
        }
    }

    private void doRebalanceMove() {
        if (candidates.isEmpty()) return;

        // Find most overloaded zone|type
        String mostOverloaded = null;
        double maxLoad = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
            double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            if (pct > maxLoad) {
                maxLoad = pct;
                mostOverloaded = ce.getKey();
            }
        }
        if (mostOverloaded == null) return;
        int pipe = mostOverloaded.lastIndexOf('|');
        String overZone = pipe >= 0 ? mostOverloaded.substring(0, pipe) : mostOverloaded;
        String overType = pipe >= 0 ? mostOverloaded.substring(pipe + 1) : null;

        // Find most underloaded zone|type of same machine type
        String mostUnderloaded = null;
        double minLoad = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            int p = ce.getKey().lastIndexOf('|');
            String mt = p >= 0 ? ce.getKey().substring(p + 1) : ce.getKey();
            if (!mt.equals(overType)) continue;
            double cap = ce.getValue();
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
            double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            if (pct < minLoad) {
                minLoad = pct;
                mostUnderloaded = ce.getKey();
            }
        }
        if (mostUnderloaded == null) return;
        int up = mostUnderloaded.lastIndexOf('|');
        String underZone = up >= 0 ? mostUnderloaded.substring(0, up) : mostUnderloaded;

        // Find best candidate to move from overloaded to underloaded
        Candidate bestCand = null;
        String bestZone = null;
        double bestDelta = Double.NEGATIVE_INFINITY;
        double before = currentMoveObjective();

        for (Candidate cand : candidates) {
            if (cand.possibleZones.size() < 2) continue;
            Double load = cand.loadByType.get(overType);
            if (load == null || load <= 0) continue;
            String currentTarget = cachedResolveTargetZone(cand.currentZone, overType);
            if (!overZone.equals(currentTarget)) continue;

            for (String z : cand.possibleZones) {
                if (z.equals(cand.currentZone)) continue;
                String newTarget = cachedResolveTargetZone(z, overType);
                if (!underZone.equals(newTarget)) continue;
                double sp = evaluateMove(cand, z);
                double delta = before - sp; // positive = improvement
                if (delta > bestDelta) {
                    bestDelta = delta;
                    bestCand = cand;
                    bestZone = z;
                }
            }
        }

        if (bestCand != null && bestZone != null) {
            String origZone = bestCand.currentZone;
            applyMove(bestCand, bestZone);
            updateMetricsAfterMove();
            // log.info("Rebalance move at iter {}: {} {} -> {} (delta={}) over={}|{} under={}|{}",
            //         iteration, bestCand.sequence, origZone, bestZone,
            //         round2(bestDelta), overZone, overType, underZone, overType);
        }
    }

    private void doRandomizeSubset() {
        if (candidates.isEmpty()) return;
        List<Candidate> movable = new ArrayList<>();
        for (Candidate c : candidates) {
            if (c.possibleZones.size() >= 2) movable.add(c);
        }
        int count = Math.max(1, (int) (movable.size() * cfg().getRandomizeFraction()));
        Collections.shuffle(movable, ThreadLocalRandom.current());
        for (int i = 0; i < Math.min(count, movable.size()); i++) {
            Candidate cand = movable.get(i);
            List<String> zones = new ArrayList<>(cand.possibleZones);
            zones.remove(cand.currentZone);
            if (!zones.isEmpty()) {
                String z = zones.get(ThreadLocalRandom.current().nextInt(zones.size()));
                applyMove(cand, z);
            }
        }
        currentSpread = computeSpread();
        currentRawSpread = computeRawSpread();
        currentStdDev = computeStdDev();
        currentMedian = computeMedian();
        log.info("Randomized subset of {} candidates after {} stagnant iterations (spread={})",
                count, noImprovementStreak, round2(currentSpread));
    }

    // ------------------------------------------------------------------ objective

    /**
     * Composite objective used by the optimizer.
     * cost = w1 * avgBoxCycleTime + w2 * maxBoxCycleTime + w3 * loadSpread + w4 * latenessPenalty
     * where loadSpread = max-min spread + 0.35 * stdDev.
     */
    double computeSpread() {
        double loadSpread = computeLoadSpread();
        if (candidates.isEmpty()) return loadSpread;
        double w3 = cfg().getSpreadWeight();
        double w4 = cfg().getLatenessWeight();
        double wBoxMean = cfg().getBoxDurationWeight();
        double wBoxMax = cfg().getBoxDurationMaxWeight();
        double boxComponent = 0.0;
        if ((wBoxMean > 0 || wBoxMax > 0) && currentBoxDurationAggregate != null
                && currentBoxDurationAggregate.sequencesMeasured > 0) {
            boxComponent = wBoxMean * currentBoxDurationAggregate.meanMinutesPerBox
                    + wBoxMax * currentBoxDurationAggregate.maxMinutesPerBox;
        }
        // Slice 3 — intra-zone machine load spread. Cached at snapshot rebuild
        // time so the per-move cost stays O(1); recompute happens in rebuildSchedule().
        double wIntraZone = cfg().getIntraZoneMachineLoadWeight();
        double intraZoneComponent = wIntraZone > 0 ? wIntraZone * currentIntraZoneSpread : 0.0;
        return cachedCycleComponent
                + w3 * loadSpread
                + w4 * computeLatenessPenalty()
                + boxComponent
                + intraZoneComponent;
    }

    private double currentMoveObjective() {
        double value = cfg().getSpreadWeight() * computeLoadSpread()
                + cfg().getLatenessWeight() * computeLatenessPenalty();
        value += boxDurationComponent(currentBoxDurationAggregate);
        double wIntraZone = cfg().getIntraZoneMachineLoadWeight();
        if (wIntraZone > 0) {
            value += wIntraZone * currentIntraZoneSpread;
        }
        return value;
    }

    private double boxDurationComponent(BoxDurationCalculator.Aggregate aggregate) {
        double wBoxMean = cfg().getBoxDurationWeight();
        double wBoxMax = cfg().getBoxDurationMaxWeight();
        if ((wBoxMean <= 0 && wBoxMax <= 0) || aggregate == null || aggregate.sequencesMeasured <= 0) {
            return 0.0;
        }
        return wBoxMean * aggregate.meanMinutesPerBox + wBoxMax * aggregate.maxMinutesPerBox;
    }

    /**
     * Lateness penalty: penalizes assignments that place urgent sequences
     * (early dueDate) into overloaded zones. Approximates lateness without
     * calling the full scheduler — fast enough for the optimization loop.
     */
    private double computeLatenessPenalty() {
        double total = 0.0;
        for (Candidate c : candidates) {
            if (c.dueDate == null) continue;
            for (Map.Entry<String, Double> le : c.loadByType.entrySet()) {
                String mt = le.getKey();
                String target = cachedResolveTargetZone(c.currentZone, mt);
                if (target == null) continue;
                String zk = zKey(target, mt);
                double cap = capacities.getOrDefault(zk, 0.0);
                if (cap <= 0) continue;
                double load = baselineLoads.getOrDefault(zk, 0.0) + pendingLoads.getOrDefault(zk, 0.0);
                double pct = (load / cap) * 100.0;
                if (pct > 100.0) {
                    double overload = pct - 100.0;
                    double urgency = dueDateWeight(c.dueDate);
                    total += overload * urgency * 0.01;
                }
            }
        }
        return total;
    }

    /**
     * Slice 3 — intra-zone machine load imbalance. For each (zone × machineType),
     * compares the planned minutes loaded on each hosting machine and sums the
     * (max − min) spread. The schedule snapshot owns the per-machine planned
     * load (one slot per serie, each with {@code machineNom}, {@code zoneNom},
     * {@code plannedMinutes}). Machines that host the type but have no slot
     * still count as 0 minutes so an idle machine pulls the spread up.
     *
     * <p>Returns 0 when the snapshot is empty or the machine-by-zone-by-type
     * map is unpopulated (legacy date+shift path before active-snapshot wiring).</p>
     */
    double computeIntraZoneMachineSpread() {
        if (currentSchedule == null || currentSchedule.size() == 0) return 0.0;
        if (snapshotMachinesByZoneByType == null || snapshotMachinesByZoneByType.isEmpty()) return 0.0;
        // (zone, machineType, machine) → planned minutes
        Map<String, Map<String, Map<String, Double>>> loaded = new HashMap<>();
        for (ScheduleSnapshot.PlannedSlot slot : currentSchedule.copyOfSlots().values()) {
            String zone = slot.getZoneNom();
            String machine = slot.getMachineNom();
            if (zone == null || machine == null) continue;
            // Resolve the MT for this machine via the zone map (one walk; small N).
            String mt = null;
            Map<String, Set<String>> byType = snapshotMachinesByZoneByType.get(zone);
            if (byType != null) {
                for (Map.Entry<String, Set<String>> e : byType.entrySet()) {
                    if (e.getValue() != null && e.getValue().contains(machine)) {
                        mt = e.getKey();
                        break;
                    }
                }
            }
            if (mt == null) continue;
            loaded.computeIfAbsent(zone, k -> new HashMap<>())
                  .computeIfAbsent(mt, k -> new HashMap<>())
                  .merge(machine, (double) slot.getPlannedMinutes(), Double::sum);
        }

        double spreadSum = 0.0;
        for (Map.Entry<String, Map<String, Set<String>>> zEntry : snapshotMachinesByZoneByType.entrySet()) {
            String zone = zEntry.getKey();
            Map<String, Map<String, Double>> zLoaded = loaded.getOrDefault(zone, java.util.Collections.emptyMap());
            for (Map.Entry<String, Set<String>> tEntry : zEntry.getValue().entrySet()) {
                String mt = tEntry.getKey();
                Set<String> machines = tEntry.getValue();
                if (machines == null || machines.size() < 2) continue; // 0/1 machine: no intra-zone spread
                Map<String, Double> mLoaded = zLoaded.getOrDefault(mt, java.util.Collections.emptyMap());
                double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
                for (String m : machines) {
                    double v = mLoaded.getOrDefault(m, 0.0);
                    if (v < min) min = v;
                    if (v > max) max = v;
                }
                if (max > min) spreadSum += (max - min);
            }
        }
        return spreadSum;
    }

    private double dueDateWeight(LocalDate dueDate) {
        if (dueDate == null) return 1.0;
        LocalDate ref = currentDate != null ? currentDate : LocalDate.now();
        long days = ChronoUnit.DAYS.between(ref, dueDate);
        if (days < 0) days = 0;
        return 1.0 + cfg().getDueDateGain() / (1.0 + days);
    }

    /**
     * Computes the load spread (max-min + 0.35*stdDev) independently for EACH
     * machine type.  The key is the machine type name; the value is the spread
     * across all zones that host that type.
     */
    Map<String, Double> computeLoadSpreadByMachineType() {
        // Group load percentages by machine type
        Map<String, List<Double>> pctsByType = new HashMap<>();
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            String key = ce.getKey();
            int pipe = key.indexOf('|');
            String machineType = pipe >= 0 ? key.substring(pipe + 1) : key;
            double base = baselineLoads.getOrDefault(key, 0.0);
            double pend = pendingLoads.getOrDefault(key, 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            pctsByType.computeIfAbsent(machineType, k -> new ArrayList<>()).add(pct);
        }

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, List<Double>> e : pctsByType.entrySet()) {
            List<Double> pcts = e.getValue();
            if (pcts.isEmpty()) continue;
            double max = Double.NEGATIVE_INFINITY;
            double min = Double.POSITIVE_INFINITY;
            double mean = 0.0;
            double m2 = 0.0;
            int count = 0;
            for (double pct : pcts) {
                if (pct > max) max = pct;
                if (pct < min) min = pct;
                count++;
                double delta = pct - mean;
                mean += delta / count;
                double delta2 = pct - mean;
                m2 += delta * delta2;
            }
            double spread = Math.max(0.0, max - min);
            double variance = count > 0 ? m2 / count : 0.0;
            result.put(e.getKey(), spread + 0.35 * Math.sqrt(variance));
        }
        return result;
    }

    /**
     * Composite load-spread objective used by the optimizer.
     * <p>Instead of a single global spread, we compute the spread per machine
     * type and return a load-weighted sum.  This forces the engine to balance
     * Lectra across Lectra zones, IP6 across IP6 zones, etc., independently.
     */
    double computeLoadSpread() {
        Map<String, Double> spreadsByType = computeLoadSpreadByMachineType();
        if (spreadsByType.isEmpty()) return 0.0;

        // Compute total load per machine type to use as weight.
        // More heavily loaded types get higher optimization priority.
        Map<String, Double> loadByType = new HashMap<>();
        double totalLoad = 0.0;
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            String key = ce.getKey();
            int pipe = key.indexOf('|');
            String machineType = pipe >= 0 ? key.substring(pipe + 1) : key;
            double base = baselineLoads.getOrDefault(key, 0.0);
            double pend = pendingLoads.getOrDefault(key, 0.0);
            double load = base + pend;
            loadByType.merge(machineType, load, Double::sum);
            totalLoad += load;
        }

        if (totalLoad <= 0) return 0.0;

        double weightedSum = 0.0;
        double maxSpread = 0.0;
        for (Map.Entry<String, Double> e : spreadsByType.entrySet()) {
            double typeLoad = loadByType.getOrDefault(e.getKey(), 0.0);
            double weight = typeLoad / totalLoad;
            weightedSum += e.getValue() * weight;
            if (e.getValue() > maxSpread) maxSpread = e.getValue();
        }
        // Fairness term: penalize the worst-imbalanced MT directly. Without
        // this, the load-share weighting lets a high-volume MT (Lectra) eat
        // the gradient and a low-volume MT (Lectra IP6) sits at huge spread
        // because it weighs little in the mean.
        double wFair = cfg().getMtFairnessWeight();
        return weightedSum + wFair * maxSpread + 0.5 * wFair * maxSpread * maxSpread / 100.0;
    }

    /** Helper to format per-machine-type spreads for logging. */
    private String formatPerMachineTypeSpreads() {
        Map<String, Double> byType = computeLoadSpreadByMachineType();
        return byType.entrySet().stream()
                .map(e -> e.getKey() + "=" + round2(e.getValue()))
                .collect(Collectors.joining(", "));
    }

    /** Pure max-min spread for display (not used for optimization decisions). */
    double computeRawSpread() {
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        boolean any = false;
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
            double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            if (pct > max) max = pct;
            if (pct < min) min = pct;
            any = true;
        }
        return any ? Math.max(0.0, max - min) : 0.0;
    }

    private double computeStdDev() {
        double mean = 0.0;
        double m2 = 0.0;
        int count = 0;
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
            double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            count++;
            double delta = pct - mean;
            mean += delta / count;
            double delta2 = pct - mean;
            m2 += delta * delta2;
        }
        if (count == 0) return 0.0;
        double variance = m2 / count;
        return Math.sqrt(variance);
    }

    private double computeMedian() {
        double[] pcts = new double[capacities.size()];
        int idx = 0;
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
            double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            pcts[idx++] = pct;
        }
        if (idx == 0) return 0.0;
        java.util.Arrays.sort(pcts, 0, idx);
        return pcts[idx / 2];
    }

    private void recomputeCachedCycleMetrics() {
        if (candidates.isEmpty()) {
            cachedCycleComponent = 0.0;
            return;
        }
        double totalWeighted = 0.0;
        double totalWeight = 0.0;
        double maxCycle = 0.0;
        for (Candidate c : candidates) {
            double weight = dueDateWeight(c.dueDate);
            totalWeighted += c.boxCycleTime * weight;
            totalWeight += weight;
            if (c.boxCycleTime > maxCycle) maxCycle = c.boxCycleTime;
        }
        double avgCycle = totalWeight > 0 ? totalWeighted / totalWeight : 0.0;
        double w1 = engineProperties.getOptimizer().getCycleAvgWeight();
        double w2 = engineProperties.getOptimizer().getCycleMaxWeight();
        cachedCycleComponent = w1 * avgCycle + w2 * maxCycle;
    }

    private String cachedResolveTargetZone(String ownerZone, String machineType) {
        if (machineType == null || machineType.trim().isEmpty()) return ownerZone;
        String key = ownerZone + "|" + machineType;
        String cached = resolveTargetZoneCache.get(key);
        if (cached != null) return cached;
        String result = LiveChargeService.resolveTargetZone(
                ownerZone, machineType, snapshotMachinesByZoneByType, snapshotZoneRoutingOrder);
        resolveTargetZoneCache.put(key, result);
        return result;
    }

    double evaluateMove(Candidate cand, String targetZone) {
        // Per-serie routing: each MT's target zone can differ between the
        // current placement and the candidate placement.
        for (Map.Entry<String, Double> e : cand.loadByType.entrySet()) {
            String mt = e.getKey();
            double minutes = e.getValue();
            String oldTarget = cachedResolveTargetZone(cand.currentZone, mt);
            String newTarget = cachedResolveTargetZone(targetZone, mt);
            if (oldTarget != null && !oldTarget.equals(newTarget)) {
                pendingLoads.merge(zKey(oldTarget, mt), -minutes, Double::sum);
            }
            if (newTarget != null && !newTarget.equals(oldTarget)) {
                pendingLoads.merge(zKey(newTarget, mt), minutes, Double::sum);
            }
        }
        double spread = cfg().getSpreadWeight() * computeLoadSpread();
        
        double overloadPenalty = 0.0;
        double overloadWeight = cfg().getOverloadWeight();
        double latenessPenalty = 0.0;
        double latenessWeight = cfg().getLatenessWeight();
        
        for (Map.Entry<String, Double> le : cand.loadByType.entrySet()) {
            String mt = le.getKey();
            String target = cachedResolveTargetZone(targetZone, mt);
            if (target == null) continue;
            String zk = zKey(target, mt);
            double cap = capacities.getOrDefault(zk, 0.0);
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(zk, 0.0);
            double pend = pendingLoads.getOrDefault(zk, 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            if (pct > 100.0) {
                double overload = pct - 100.0;
                overloadPenalty += overload;
                if (cand.dueDate != null && latenessWeight > 0) {
                    double urgency = dueDateWeight(cand.dueDate);
                    latenessPenalty += overload * urgency * 0.01;
                }
            }
        }

        // Revert — same deltas, opposite sign.
        for (Map.Entry<String, Double> e : cand.loadByType.entrySet()) {
            String mt = e.getKey();
            double minutes = e.getValue();
            String oldTarget = cachedResolveTargetZone(cand.currentZone, mt);
            String newTarget = cachedResolveTargetZone(targetZone, mt);
            if (oldTarget != null && !oldTarget.equals(newTarget)) {
                pendingLoads.merge(zKey(oldTarget, mt), minutes, Double::sum);
            }
            if (newTarget != null && !newTarget.equals(oldTarget)) {
                pendingLoads.merge(zKey(newTarget, mt), -minutes, Double::sum);
            }
        }
        // Material soft cost — count of fabric references NOT on a rack in
        // the target zone. Cached during snapshot build; trust the cache.
        double materialPenalty = 0.0;
        double materialWeight = cfg().getMaterialAlertWeight();
        if (materialWeight > 0) {
            Map<String, Integer> byZone = materialAvailabilityBySeqAndZone.get(cand.sequence);
            Integer notInZone = byZone != null ? byZone.get(targetZone) : null;
            if (notInZone != null) {
                materialPenalty = notInZone;
            }
        }
        return spread
                + overloadWeight * overloadPenalty
                + latenessWeight * latenessPenalty
                + materialWeight * materialPenalty
                + estimateBoxDurationComponent(cand, targetZone);
    }

    private double estimateBoxDurationComponent(Candidate cand, String targetZone) {
        if (cand == null || targetZone == null) return 0.0;
        double wBoxMean = cfg().getBoxDurationWeight();
        double wBoxMax = cfg().getBoxDurationMaxWeight();
        if (wBoxMean <= 0 && wBoxMax <= 0) return 0.0;
        if (scheduleBuilderService == null || boxDurationCalculator == null
                || snapshotSerieInputs.isEmpty() || snapshotMachinesByZoneByType.isEmpty()) {
            return boxDurationComponent(currentBoxDurationAggregate);
        }
        if (targetZone.equals(cand.currentZone)) {
            return boxDurationComponent(currentBoxDurationAggregate);
        }

        try {
            Map<String, String> seqToZone = new HashMap<>();
            for (Candidate c : candidates) {
                seqToZone.put(c.sequence, c == cand ? targetZone : c.currentZone);
            }
            ScheduleSnapshot projected = scheduleBuilderService.build(
                    snapshotHorizon, snapshotSerieInputs, seqToZone, snapshotMachinesByZoneByType);
            return boxDurationComponent(boxDurationCalculator.compute(projected, boxCountsBySequence));
        } catch (Exception ex) {
            log.debug("Box-duration move estimate failed for {} -> {}", cand.sequence, targetZone, ex);
            return boxDurationComponent(currentBoxDurationAggregate);
        }
    }

    private void applyMove(Candidate cand, String targetZone) {
        for (Map.Entry<String, Double> e : cand.loadByType.entrySet()) {
            String mt = e.getKey();
            double minutes = e.getValue();
            String oldTarget = cachedResolveTargetZone(cand.currentZone, mt);
            String newTarget = cachedResolveTargetZone(targetZone, mt);
            if (oldTarget != null && !oldTarget.equals(newTarget)) {
                pendingLoads.merge(zKey(oldTarget, mt), -minutes, Double::sum);
            }
            if (newTarget != null && !newTarget.equals(oldTarget)) {
                pendingLoads.merge(zKey(newTarget, mt), minutes, Double::sum);
            }
        }
        cand.currentZone = targetZone;
    }

    private double getMaxLoadPct() {
        double max = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
            double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            if (pct > max) max = pct;
        }
        return max == Double.NEGATIVE_INFINITY ? 0.0 : max;
    }

    private double getMinLoadPct() {
        double min = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Double> ce : capacities.entrySet()) {
            double cap = ce.getValue();
            if (cap <= 0) continue;
            double base = baselineLoads.getOrDefault(ce.getKey(), 0.0);
            double pend = pendingLoads.getOrDefault(ce.getKey(), 0.0);
            double pct = ((base + pend) / cap) * 100.0;
            if (pct < min) min = pct;
        }
        return min == Double.POSITIVE_INFINITY ? 0.0 : min;
    }

    // ------------------------------------------------------------------ persistence

    private void saveBestAssignment() {
        bestAssignment.clear();
        for (Candidate c : candidates) {
            bestAssignment.put(c.sequence, c.currentZone);
        }
    }

    private void restoreAssignment(Map<String, String> assignment) {
        for (Candidate c : candidates) {
            String target = assignment.get(c.sequence);
            if (target != null && !target.equals(c.currentZone)) {
                applyMove(c, target);
            }
        }
    }

    private volatile int lastSavedAssignmentHash = 0;

    private void saveSuggestions() {
        if (currentRunId == null) return;
        try {
            int hash = bestAssignment.hashCode();
            if (hash == lastSavedAssignmentHash) return; // nothing changed

            // Batch-load previous zones in ONE query
            List<String> sequences = new ArrayList<>(bestAssignment.keySet());
            Map<String, String> prevZoneMap = new HashMap<>();
            for (int i = 0; i < sequences.size(); i += 1000) {
                List<String> batch = sequences.subList(i, Math.min(i + 1000, sequences.size()));
                for (Object[] row : cuttingRequestRepository.findDispatchedZonesBySequences(batch)) {
                    prevZoneMap.put((String) row[0], (String) row[1]);
                }
            }

            List<DispatchEngineRunSuggestion> toSave = new ArrayList<>();
            for (Map.Entry<String, String> e : bestAssignment.entrySet()) {
                DispatchEngineRunSuggestion s = new DispatchEngineRunSuggestion();
                s.setId(new DispatchEngineRunSuggestion.Pk(currentRunId, e.getKey()));
                s.setSuggestedZone(e.getValue());
                s.setPreviousZone(prevZoneMap.get(e.getKey()));
                toSave.add(s);
            }
            if (!toSave.isEmpty()) {
                suggestionRepository.saveAll(toSave);
            }
            lastSavedAssignmentHash = hash;
        } catch (Exception ex) {
            log.warn("Failed to save suggestions", ex);
        }
    }

    private void saveSample() {
        if (currentRunId == null) return;
        try {
            DispatchEngineIndicatorSample s = new DispatchEngineIndicatorSample();
            s.setId(new DispatchEngineIndicatorSample.Pk(currentRunId, iteration));
            s.setSampleAt(LocalDateTime.now());
            s.setSpreadPct(BigDecimal.valueOf(round2(currentSpread)));
            s.setMaxLoadPct(BigDecimal.valueOf(round2(getMaxLoadPct())));
            s.setMinLoadPct(BigDecimal.valueOf(round2(getMinLoadPct())));
            sampleRepository.save(s);
        } catch (Exception e) {
            log.warn("Failed to save sample", e);
        }

        // Log per-machine-type spread breakdown so operators can see which
        // machine types are balanced and which are not.
        try {
            Map<String, Double> byType = computeLoadSpreadByMachineType();
            if (!byType.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Sample iter=").append(iteration)
                  .append(" weightedSpread=").append(round2(currentSpread))
                  .append(" perMachineType={");
                boolean first = true;
                for (Map.Entry<String, Double> e : byType.entrySet()) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append(e.getKey()).append("=").append(round2(e.getValue()));
                }
                sb.append("}");
                log.info(sb.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to log per-machine-type sample", e);
        }
    }

    private void updateRunInitialSpread() {
        if (currentRunId == null) return;
        Optional<DispatchEngineRun> opt = runRepository.findById(currentRunId);
        if (opt.isPresent()) {
            DispatchEngineRun r = opt.get();
            r.setInitialSpread(BigDecimal.valueOf(round2(initialSpread)));
            runRepository.save(r);
        }
    }

    private synchronized void saveFinalRun() {
        if (currentRunId == null) return;
        try {
            Optional<DispatchEngineRun> opt = runRepository.findById(currentRunId);
            if (!opt.isPresent()) return;
            DispatchEngineRun r = opt.get();
            r.setEndedAt(LocalDateTime.now());
            r.setFinalState(DispatchEngineRun.FinalState.STOPPED);
            r.setIterations(iteration);
            r.setImprovements(improvements);
            r.setFinalSpread(BigDecimal.valueOf(round2(bestSpread)));
            r.setNotes(buildRunNotes());
            runRepository.save(r);

            // Persist final best suggestions.
            saveSuggestions();
        } catch (Exception ex) {
            log.error("Failed to save final run", ex);
        }
    }

    /**
     * Compose the {@code dispatch_engine_run.notes} blob — used by the runs
     * panel in the UI and by anyone diffing engine performance over time.
     * Format is stable {@code key=value; key=value} so it stays grep-friendly.
     */
    private String buildRunNotes() {
        int moves = countMovesFromInitial();
        StringBuilder sb = new StringBuilder();
        sb.append("moves=").append(moves);
        if (convergedFlag) {
            sb.append("; converged=true");
        }
        return sb.toString();
    }

    /**
     * Number of sequences whose best-known assignment differs from the
     * post-warm-start baseline. Lets the chef see at a glance "this run
     * proposes to reroute N sequences" before publishing.
     */
    private int countMovesFromInitial() {
        if (initialAssignment.isEmpty()) return 0;
        int moves = 0;
        for (Map.Entry<String, String> e : bestAssignment.entrySet()) {
            String orig = initialAssignment.get(e.getKey());
            if (orig == null || !orig.equals(e.getValue())) moves++;
        }
        return moves;
    }

    // ------------------------------------------------------------------ helpers

    private void transitionTo(EngineState newState) {
        this.state = newState;
        webSocketService.publishState(newState, mode, currentRunId);
    }

    private void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            executor = null;
        }
        if (loopFuture != null) {
            loopFuture.cancel(true);
            loopFuture = null;
        }
    }

    private static String zKey(String zoneNom, String machineType) {
        return zoneNom + "|" + machineType;
    }

    private double lookupEfficience(LocalDate date, int shift, String machineType) {
        String groupe = "Coupe";
        if (machineType != null) {
            String t = machineType.trim();
            if (t.equalsIgnoreCase("LASER-DXF") || t.equalsIgnoreCase("LASER-LSR")
                    || t.equalsIgnoreCase("LASER")) {
                groupe = "Laser";
            }
        }
        var ci = capaciteInstalleeService.getEffective(date, shift, groupe);
        if (ci == null || ci.getEfficienceTarget() == null) {
            return DEFAULT_EFFICIENCE;
        }
        return ci.getEfficienceTarget();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static int shiftOrder(String dueShift) {
        if (dueShift == null || dueShift.trim().isEmpty()) return 99;
        try {
            int parsed = Integer.parseInt(dueShift.trim());
            return parsed >= 1 && parsed <= 3 ? parsed : 99;
        } catch (NumberFormatException ignored) {
            return 99;
        }
    }

    // ------------------------------------------------------------------ inner

    private static final class RequestLight {
        final String sequence;
        final String dispatchedZone;
        final String zoneAcceptanceStatus;
        final boolean pinnedByChef;
        final String preferredZoneNom;
        final LocalDate dueDate;
        final String dueShift;
        RequestLight(String sequence, String dispatchedZone, String zoneAcceptanceStatus,
                     boolean pinnedByChef, String preferredZoneNom, LocalDate dueDate,
                     String dueShift) {
            this.sequence = sequence;
            this.dispatchedZone = dispatchedZone;
            this.zoneAcceptanceStatus = zoneAcceptanceStatus;
            this.pinnedByChef = pinnedByChef;
            this.preferredZoneNom = preferredZoneNom;
            this.dueDate = dueDate;
            this.dueShift = dueShift;
        }
    }

    private static final class SerieKey {
        final String sequence;
        final String serieId;
        SerieKey(String sequence, String serieId) {
            this.sequence = sequence; this.serieId = serieId;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SerieKey)) return false;
            SerieKey k = (SerieKey) o;
            return sequence.equals(k.sequence) && serieId.equals(k.serieId);
        }
        @Override public int hashCode() {
            return sequence.hashCode() * 31 + serieId.hashCode();
        }
    }

    private static final class SerieRow {
        final SerieKey key;
        final String machineType;
        final Double tempsDeCoupe;
        final Integer nbrCouche;
        final String placement;
        final String partNumberMaterial;
        final Double longueur;
        SerieRow(SerieKey key, String machineType, Double tempsDeCoupe,
                 Integer nbrCouche, String placement, String partNumberMaterial, Double longueur) {
            this.key = key; this.machineType = machineType;
            this.tempsDeCoupe = tempsDeCoupe; this.nbrCouche = nbrCouche; this.placement = placement;
            this.partNumberMaterial = partNumberMaterial;
            this.longueur = longueur;
        }
    }

    static final class Candidate {
        final String sequence;
        String currentZone;
        final List<String> possibleZones;
        final Map<String, Double> loadByType;
        final double boxCycleTime;
        final LocalDate dueDate;
        final String dueShift;
        final Set<String> refTissus;
        final String serie;

        Candidate(String sequence, String currentZone, List<String> possibleZones,
                  Map<String, Double> loadByType, double boxCycleTime,
                  LocalDate dueDate, String dueShift, Set<String> refTissus, String serie) {
            this.sequence = sequence;
            this.currentZone = currentZone;
            this.possibleZones = possibleZones;
            this.loadByType = loadByType;
            this.boxCycleTime = boxCycleTime;
            this.dueDate = dueDate;
            this.dueShift = dueShift;
            this.refTissus = refTissus;
            this.serie = serie;
        }

        String getSerie() {
            return serie;
        }
    }
}
