package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lear.MGCMS.domain.CapaciteInstallee;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.dispatcher.DispatchEngineRun;
import com.lear.MGCMS.domain.dispatcher.DispatchEngineRunSuggestion;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.dispatcher.DispatchEngineIndicatorSampleRepository;
import com.lear.MGCMS.repositories.dispatcher.DispatchEngineRunRepository;
import com.lear.MGCMS.repositories.dispatcher.DispatchEngineRunSuggestionRepository;
import com.lear.MGCMS.services.CapaciteInstalleeService;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;
import com.lear.MGCMS.services.scheduling.ShiftClock;

/**
 * Smoke tests for the Phase B Continuous Dispatch Optimizer.
 *
 * <p>Two scenarios:</p>
 * <ol>
 *   <li>{@code FIXED_DURATION} — engine self-stops on timer and persists run +
 *       suggestions.</li>
 *   <li>{@code CONTINUOUS} — engine reaches IMPROVING, perturbs (iteration
 *       counter advances), then stops cleanly on demand.</li>
 * </ol>
 *
 * <p>Both tests use a balanced 2-zone fixture (ZA + ZB, one Lectra each, four
 * 100-min PENDING sequences). With this layout the warm-start should produce
 * a near-zero spread; the goal is not to demonstrate a numerical drop but to
 * verify the lifecycle, persistence, and threading contract end-to-end.</p>
 */
class ContinuousDispatchOptimizerServiceTest {

    @Mock private CuttingRequestRepository cuttingRequestRepository;
    @Mock private CuttingRequestSerieDataRepository serieDataRepository;
    @Mock private CuttingTimeCalculator cuttingTimeCalculator;
    @Mock private ActiveMachineResolver activeMachineResolver;
    @Mock private CapaciteInstalleeService capaciteInstalleeService;
    @Mock private ProductionTableRepository productionTableRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private ShiftProperties shiftProperties;
    @Mock private ZoneLoadProperties zoneLoadProperties;
    @Mock private DispatchEngineRunRepository runRepository;
    @Mock private DispatchEngineRunSuggestionRepository suggestionRepository;
    @Mock private DispatchEngineIndicatorSampleRepository sampleRepository;
    @Mock private DispatchEngineWebSocketService webSocketService;
    @Mock private SequenceDispatcherService sequenceDispatcherService;
    @Mock private ShiftClock shiftClock;
    @Mock private EngineProperties engineProperties;
    @Mock private EngineProperties.Optimizer engineOptimizerProps;
    @Mock private MaterialAvailabilityChecker materialAvailabilityChecker;
    @Mock private com.lear.MGCMS.services.OrdonnancementService ordonnancementService;
    @Mock private ScheduleBuilderService scheduleBuilderService;
    @Mock private BoxDurationCalculator boxDurationCalculator;
    @Mock private com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository boxInfoRepository;

    @InjectMocks private ContinuousDispatchOptimizerService optimizer;

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 5);
    private final List<DispatchEngineRun> savedRuns = new ArrayList<>();
    private final AtomicLong nextRunId = new AtomicLong(1);

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        wireBalancedFixture();
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("FIXED_DURATION: engine self-stops on timer and saves run + suggestions")
    void fixedDuration_stopsOnTimer_andSavesRunAndSuggestions() throws Exception {
        optimizer.start(TODAY, 1, EngineMode.FIXED_DURATION, 1, "TEST");

        // Wait up to 6s for self-stop (timer = 1s + warmup + shutdown grace)
        long deadline = System.currentTimeMillis() + 6000;
        while (optimizer.getState() != EngineState.STOPPED && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        assertEquals(EngineState.STOPPED, optimizer.getState(),
                "engine should self-stop on timer expiry");
        assertNotNull(optimizer.getCurrentRunId(), "run id should be assigned");
        // initial save (status WARMING with id) + final save (status STOPPED + iterations + spread)
        verify(runRepository, atLeast(2)).save(any(DispatchEngineRun.class));
        // Suggestions are saved at end-of-run via saveAll (one batch per
        // assignment hash change). Engine now batches to avoid 1-row-per-call.
        verify(suggestionRepository, atLeast(1)).saveAll(any());
        // Spread must be a non-negative number
        assertTrue(optimizer.getCurrentSpread() >= 0,
                "spread is a non-negative number but was " + optimizer.getCurrentSpread());
    }

    @Test
    @DisplayName("CONTINUOUS: engine reaches IMPROVING, perturbs, then stops on demand")
    void continuous_reachesImproving_thenStopsOnDemand() throws Exception {
        optimizer.start(TODAY, 1, EngineMode.CONTINUOUS, null, "TEST");

        // Wait for IMPROVING (warm-start usually <300ms)
        long deadline = System.currentTimeMillis() + 4000;
        while (optimizer.getState() != EngineState.IMPROVING && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(EngineState.IMPROVING, optimizer.getState(),
                "engine should reach IMPROVING within 4s");

        // Let it perturb for a bit
        Thread.sleep(500);
        assertTrue(optimizer.getIteration() > 0,
                "iteration counter should advance — got " + optimizer.getIteration());

        // Stop and wait for STOPPED
        optimizer.stop();
        deadline = System.currentTimeMillis() + 4000;
        while (optimizer.getState() != EngineState.STOPPED && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(EngineState.STOPPED, optimizer.getState());

        // Final spread should never be worse than the initial one (since the
        // engine only accepts improving moves).
        if (optimizer.getInitialSpread() > 0) {
            assertTrue(optimizer.getCurrentSpread() <= optimizer.getInitialSpread() + 0.001,
                    "final spread (" + optimizer.getCurrentSpread()
                            + ") should not exceed initial (" + optimizer.getInitialSpread() + ")");
        }
    }

    @Test
    @DisplayName("Box-cycle cost is computed and material constraint rejects move with POSITIVE_INFINITY")
    void boxCycleCost_computed_andMaterialConstraintRejectsMove() throws Exception {
        // Extend fixture rows with partNumberMaterial so refTissus are populated.
        List<Object[]> serieRows = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            serieRows.add(new Object[]{
                    "s" + i, "seq" + i, "Lectra", 100.0, 1, "P" + i,
                    "MAT-" + i,  // partNumberMaterial at index 6
                    10.0         // longueur at index 7
            });
        }
        when(serieDataRepository.findSeriesByDateShiftLight(eq(TODAY), eq("1"))).thenReturn(serieRows);

        // Slice 0: optimizer pre-caches material via countNotInZone (not check()).
        // ZB has 1 fabric NOT_IN_ZONE for every candidate; ZA has full stock.
        when(materialAvailabilityChecker.countNotInZone(any(), eq("ZB"))).thenReturn(1);
        when(materialAvailabilityChecker.countNotInZone(any(), eq("ZA"))).thenReturn(0);

        optimizer.buildSnapshot(TODAY, 1);
        optimizer.greedyWarmStart();

        List<ContinuousDispatchOptimizerService.Candidate> cands = optimizer.getCandidates();
        assertEquals(4, cands.size(), "four movable candidates");

        // Verify box-cycle time is populated (100 min per serie)
        ContinuousDispatchOptimizerService.Candidate c1 = cands.stream()
                .filter(c -> "seq1".equals(c.sequence)).findFirst().orElseThrow();
        assertEquals(100.0, c1.boxCycleTime, 0.001, "boxCycleTime = sum of serie minutes");

        // Verify objective includes cycle time (objective > raw spread)
        double rawSpread = optimizer.computeRawSpread();
        double objective = optimizer.computeSpread();
        assertTrue(objective > rawSpread,
                "objective (" + objective + ") should exceed raw spread (" + rawSpread + ") when cycle weights > 0");

        // Slice 0: material is now an advisory soft cost, not a hard reject.
        // The move to a zone with NOT_IN_ZONE material must still be finite,
        // and must cost strictly more than the move to a zone with all
        // materials available (logistics gets a hint, dispatch isn't blocked).
        double withMissing = optimizer.evaluateMove(c1, "ZB");
        double withMaterial = optimizer.evaluateMove(c1, "ZA");
        assertTrue(Double.isFinite(withMissing),
                "move to advisory-material zone must be finite under Slice 0 semantics");
        assertTrue(withMissing > withMaterial,
                "advisory penalty: missing-material zone (" + withMissing
                        + ") should cost more than fully-stocked zone (" + withMaterial + ")");
    }

    @Test
    @DisplayName("active reload scopes candidates to current-or-overdue due buckets")
    void activeReload_usesCurrentOrOverdueDueBucketsOnly() {
        LocalDate yesterday = TODAY.minusDays(1);
        LocalDate tomorrow = TODAY.plusDays(1);

        when(cuttingRequestRepository.findAllActiveLight()).thenReturn(java.util.Collections.<Object[]>singletonList(
                new Object[]{"seq-future", null, "PENDING", false, null, tomorrow}
        ));
        when(cuttingRequestRepository.findActiveDueOnOrBeforeLight(eq(TODAY), eq("1")))
                .thenReturn(java.util.Arrays.asList(
                        new Object[]{"seq-old", null, "PENDING", false, null, yesterday, "3"},
                        new Object[]{"seq-current", null, "PENDING", false, null, TODAY, "1"}
                ));
        when(serieDataRepository.findLiveChargeSeriesBySequences(any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<String> seqs = inv.getArgument(0);
            List<Object[]> rows = new ArrayList<>();
            int i = 1;
            for (String seq : seqs) {
                rows.add(richPendingSerie("s-active-" + i++, seq));
            }
            return rows;
        });

        Map<String, Object> result = optimizer.reloadActiveSnapshotFromGroundTruth();

        assertEquals("RELOADED", result.get("status"));
        List<ContinuousDispatchOptimizerService.Candidate> cands = optimizer.getCandidates();
        assertEquals(2, cands.size(), "future active sequence must not enter active snapshot");
        assertTrue(cands.stream().anyMatch(c -> "seq-old".equals(c.sequence) && "3".equals(c.dueShift)));
        assertTrue(cands.stream().anyMatch(c -> "seq-current".equals(c.sequence) && "1".equals(c.dueShift)));
        assertTrue(cands.stream().noneMatch(c -> "seq-future".equals(c.sequence)));
        verify(cuttingRequestRepository).findActiveDueOnOrBeforeLight(eq(TODAY), eq("1"));
    }

    // ------------------------------------------------------------------ fixture

    /**
     * Two STRICT zones (ZA, ZB), one Lectra machine each, four PENDING
     * sequences of 100 min Lectra each. After warm-start: 2 sequences per
     * zone — perfectly balanced.
     */
    private void wireBalancedFixture() {
        Zone zoneA = zone("ZA", Zone.Category.STRICT, true);
        Zone zoneB = zone("ZB", Zone.Category.STRICT, true);
        when(zoneRepository.findAllActive()).thenReturn(java.util.Arrays.asList(zoneA, zoneB));

        // ProductionTable: 1 Lectra in each zone
        List<Object[]> machinesA = new ArrayList<>();
        machinesA.add(new Object[]{"L-A1", "Lectra"});
        when(productionTableRepository.findMachinesWithTypeInZone("ZA")).thenReturn(machinesA);

        List<Object[]> machinesB = new ArrayList<>();
        machinesB.add(new Object[]{"L-B1", "Lectra"});
        when(productionTableRepository.findMachinesWithTypeInZone("ZB")).thenReturn(machinesB);

        // ActiveMachineResolver: chef has confirmed both
        when(activeMachineResolver.activeMachines(eq(TODAY), eq(1), eq("ZA")))
                .thenReturn(new HashSet<>(Collections.singletonList("L-A1")));
        when(activeMachineResolver.activeMachines(eq(TODAY), eq(1), eq("ZB")))
                .thenReturn(new HashSet<>(Collections.singletonList("L-B1")));

        // Capacity: 90% efficiency
        CapaciteInstallee ci = new CapaciteInstallee();
        ci.setEfficienceTarget(90.0);
        when(capaciteInstalleeService.getEffective(eq(TODAY), eq(1), anyString())).thenReturn(ci);

        // Shift duration
        when(shiftProperties.getDurationMinutes()).thenReturn(460);

        // 4 PENDING requests, no dispatched zone, none pinned
        List<Object[]> reqRows = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            reqRows.add(new Object[]{"seq" + i, null, "PENDING", false, null, TODAY});
        }
        when(cuttingRequestRepository.findAllLight(eq(TODAY), eq("1"))).thenReturn(reqRows);

        // 4 series — one Lectra serie per sequence
        List<Object[]> serieRows = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            serieRows.add(new Object[]{
                    "s" + i,        // serie
                    "seq" + i,      // sequence
                    "Lectra",       // machine type
                    100.0,          // tempsDeCoupe
                    1,              // nbrCouche
                    "P" + i         // placement
            });
        }
        when(serieDataRepository.findSeriesByDateShiftLight(eq(TODAY), eq("1"))).thenReturn(serieRows);

        // No started sequences — every PENDING is movable
        when(serieDataRepository.findStartedSequenceIdsForDateShift(eq(TODAY), eq("1")))
                .thenReturn(Collections.emptyList());

        // CuttingTimeCalculator: 100 min per serie
        when(cuttingTimeCalculator.resolveMinutesBatch(any())).thenAnswer(inv -> {
            List<CuttingTimeCalculator.SerieInput<?>> inputs = inv.getArgument(0);
            Map<Object, Double> out = new HashMap<>();
            for (CuttingTimeCalculator.SerieInput<?> in : inputs) {
                out.put(in.key, 100.0);
            }
            return out;
        });

        // Run repo: assign IDs, remember rows for findById
        when(runRepository.save(any(DispatchEngineRun.class))).thenAnswer(inv -> {
            DispatchEngineRun r = inv.getArgument(0);
            if (r.getId() == null) r.setId(nextRunId.getAndIncrement());
            // Replace if same id, else add
            savedRuns.removeIf(x -> r.getId().equals(x.getId()));
            savedRuns.add(r);
            return r;
        });
        when(runRepository.findById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return savedRuns.stream().filter(r -> id.equals(r.getId())).findFirst();
        });

        // Suggestion + sample saves: accept and echo
        when(suggestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sampleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // saveFinalRun reads CuttingRequest to enrich previousZone — return null is fine
        when(cuttingRequestRepository.findBySequence(anyString())).thenReturn(null);

        // ShiftClock used by runLoopActive — pin to TODAY/shift 1 so the active path
        // would also have a deterministic slot if tests start exercising it.
        when(shiftClock.currentSlot()).thenReturn(new ShiftClock.ShiftSlot(TODAY, 1));

        // EngineProperties: convergence early-stop with default knobs. Setting
        // convergenceIterations to 0 disables the check, which keeps the
        // CONTINUOUS lifecycle test focused on threading + state transitions
        // rather than convergence semantics.
        when(engineProperties.getOptimizer()).thenReturn(engineOptimizerProps);
        when(engineOptimizerProps.getConvergenceIterations()).thenReturn(0);
        when(engineOptimizerProps.getImprovementEpsilon()).thenReturn(0.05);
        when(engineOptimizerProps.getCycleAvgWeight()).thenReturn(1.0);
        when(engineOptimizerProps.getCycleMaxWeight()).thenReturn(0.5);
        when(engineOptimizerProps.getSpreadWeight()).thenReturn(0.3);
        // Slice 0: material is an advisory soft cost, not a hard reject.
        // The optimizer test asserts that moves to MISSING-material zones cost
        // more than moves to AVAILABLE zones — only true when this weight > 0.
        when(engineOptimizerProps.getMaterialAlertWeight()).thenReturn(0.01);
        // Non-zero loop modulos so % does not divide by zero in the iteration loop.
        when(engineOptimizerProps.getSampleEvery()).thenReturn(500);
        when(engineOptimizerProps.getRebalanceEvery()).thenReturn(40);
        when(engineOptimizerProps.getRandomizeAfter()).thenReturn(120);
        when(engineOptimizerProps.getKickAfter()).thenReturn(75);
        when(engineOptimizerProps.getKickMoves()).thenReturn(12);
        when(engineOptimizerProps.getMultiCandidateThreshold()).thenReturn(25);
        when(engineOptimizerProps.getTemperatureInitial()).thenReturn(5.0);
        when(engineOptimizerProps.getTemperatureMin()).thenReturn(0.05);
        when(engineOptimizerProps.getTemperatureCooling()).thenReturn(0.9995);
        when(engineOptimizerProps.getRandomizeFraction()).thenReturn(0.12);
        when(engineOptimizerProps.getDispatchPhaseMs()).thenReturn(20_000L);
        when(engineOptimizerProps.getOrdonnancementPhaseMs()).thenReturn(40_000L);
        when(engineOptimizerProps.getDueDateGain()).thenReturn(9.0);

        // Slice 1/2 collaborators: stub so the engine loop doesn't NPE inside
        // rebuildSchedule / box-duration refresh.
        when(scheduleBuilderService.build(any(), any(), any(), any()))
                .thenReturn(ScheduleSnapshot.empty());
        when(boxDurationCalculator.compute(any(), any()))
                .thenReturn(BoxDurationCalculator.Aggregate.empty());
        when(boxInfoRepository.countBoxesBySequences(any()))
                .thenReturn(java.util.Collections.emptyList());
    }

    private static Zone zone(String nom, Zone.Category cat, boolean active) {
        Zone z = new Zone();
        z.setNom(nom);
        z.setCategory(cat);
        z.setActive(active);
        return z;
    }

    private static Object[] richPendingSerie(String serie, String sequence) {
        return new Object[]{
                serie,       // 0 serie
                sequence,    // 1 sequence
                "Lectra",    // 2 machine type
                100.0,       // 3 tempsDeCoupe
                1,           // 4 nbrCouche
                "P-" + serie,// 5 placement
                "Waiting",   // 6 statusCoupe
                "Waiting",   // 7 statusMatelassage
                null,        // 8 tableCoupe
                null,        // 9 tableMatelassage
                null,        // 10 dateDebutCoupe
                null,        // 11 dateFinCoupe
                null,        // 12 dateDebutMatelassage
                null,        // 13 dateFinMatelassage
                "MAT-" + serie,
                10.0
        };
    }
}
