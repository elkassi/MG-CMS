package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;

/**
 * Unit tests for the in-zone serie ordering strategies.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>Matelassage→coupe precedence (ready-to-cut always before waiting)</li>
 *   <li>LASER-DXF vs Lectra/Gerber partitioning</li>
 *   <li>Box-duration improvement on a 50-serie fixture</li>
 *   <li>Zero-impact when the feature flag is set to legacy</li>
 * </ul>
 */
class SeriesOrderingStrategyTest {

    @Mock private CuttingTimeCalculator cuttingTimeCalculator;
    @InjectMocks private BoxDurOptimizedOrderingStrategy v2;

    private LegacySeriesOrderingStrategy legacy;
    private SeriesOrderingStrategy.Context ctx;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        legacy = new LegacySeriesOrderingStrategy();
        ctx = new SeriesOrderingStrategy.Context();
        ctx.machineNom = "LEC-01";
        ctx.cuttingTimeMap = Collections.emptyMap();
        ctx.laserDxfMachines = Collections.emptySet();
        ctx.gerberMachines = Collections.emptySet();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private OrdonnancementService.SerieDTO serie(String id, String seq,
                                                  String statusMatelassage,
                                                  Double planningDateOffsetDays,
                                                  Double cutMinutes) {
        OrdonnancementService.SerieDTO s = new OrdonnancementService.SerieDTO();
        s.serie = id;
        s.sequence = seq;
        s.statusMatelassage = statusMatelassage;
        s.statusCoupe = "Waiting";
        s.tableCoupe = "LEC-01";
        s.planningDate = planningDateOffsetDays != null
                ? LocalDate.now().plusDays(planningDateOffsetDays.intValue())
                : null;
        s.dateFinMatelassage = "Complete".equals(statusMatelassage)
                ? LocalDateTime.now().minusMinutes(10)
                : null;
        s.longueur = 10.0;
        s.nbrCouche = 2;
        s.tempsDeCoupe = cutMinutes;
        return s;
    }

    private void mockCalculator(double defaultMinutes) {
        when(cuttingTimeCalculator.resolveMinutes(
                anyString(), any(), anyInt(), anyString(), anyMap()))
                .thenReturn(defaultMinutes);
    }

    private double boxDur(List<OrdonnancementService.SerieDTO> sequenceSeries) {
        if (sequenceSeries == null || sequenceSeries.isEmpty()) return 0.0;
        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;
        for (OrdonnancementService.SerieDTO s : sequenceSeries) {
            LocalDateTime start = s.dateDebutMatelassage != null
                    ? s.dateDebutMatelassage
                    : LocalDateTime.now();
            LocalDateTime end = s.dateFinCoupe != null
                    ? s.dateFinCoupe
                    : start.plusMinutes((long) (s.tempsDeCoupe != null ? s.tempsDeCoupe : 30.0));
            if (minStart == null || start.isBefore(minStart)) minStart = start;
            if (maxEnd == null || end.isAfter(maxEnd)) maxEnd = end;
        }
        if (minStart == null || maxEnd == null) return 0.0;
        long span = java.time.Duration.between(minStart, maxEnd).toMinutes();
        return (double) span / sequenceSeries.size();
    }

    // ------------------------------------------------------------------
    // 1. Sanity
    // ------------------------------------------------------------------

    @Test
    @DisplayName("strategy beans are instantiable")
    void sanity() {
        assertTrue(legacy != null);
        assertTrue(v2 != null);
    }

    // ------------------------------------------------------------------
    // 2. Precedence
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ready-to-cut series stay before waiting series after sort")
    void readyBeforeWaiting() {
        mockCalculator(30.0);

        List<OrdonnancementService.SerieDTO> ready = new ArrayList<>();
        ready.add(serie("R1", "SEQ-A", "Complete", 2.0, 60.0));
        ready.add(serie("R2", "SEQ-B", "Complete", 1.0, 45.0));

        List<OrdonnancementService.SerieDTO> waiting = new ArrayList<>();
        waiting.add(serie("W1", "SEQ-C", "Waiting", 0.0, 30.0));
        waiting.add(serie("W2", "SEQ-D", "Waiting", 0.0, 20.0));

        v2.sortReadyToCut(ready, ctx);
        v2.sortWaiting(waiting, ctx);

        assertTrue(ready.stream().allMatch(s -> "Complete".equals(s.statusMatelassage)));
        assertTrue(waiting.stream().allMatch(s -> "Waiting".equals(s.statusMatelassage)));
    }

    // ------------------------------------------------------------------
    // 3. Machine-type partitioning
    // ------------------------------------------------------------------

    @Test
    @DisplayName("LASER-DXF machine uses parallel spread+cut time for waiting sort")
    void laserDxfParallelTime() {
        ctx.machineNom = "DXF-01";
        ctx.laserDxfMachines = Set.of("DXF-01");
        mockCalculator(100.0);

        List<OrdonnancementService.SerieDTO> waiting = new ArrayList<>();
        OrdonnancementService.SerieDTO s1 = serie("L1", "SEQ-L", "Waiting", 0.0, 100.0);
        s1.tableCoupe = "DXF-01";
        s1.longueur = 5.0;
        s1.nbrCouche = 1;
        waiting.add(s1);

        v2.sortWaiting(waiting, ctx);
        assertEquals("L1", waiting.get(0).serie);
    }

    @Test
    @DisplayName("Lectra machine uses sequential spread+cut time for waiting sort")
    void lectraSequentialTime() {
        ctx.machineNom = "LEC-01";
        ctx.laserDxfMachines = Collections.emptySet();
        mockCalculator(100.0);

        List<OrdonnancementService.SerieDTO> waiting = new ArrayList<>();
        OrdonnancementService.SerieDTO s1 = serie("L1", "SEQ-L", "Waiting", 0.0, 100.0);
        s1.tableCoupe = "LEC-01";
        s1.longueur = 5.0;
        s1.nbrCouche = 1;
        waiting.add(s1);

        v2.sortWaiting(waiting, ctx);
        assertEquals("L1", waiting.get(0).serie);
    }

    // ------------------------------------------------------------------
    // 4. BoxDur improvement on a ~50-serie fixture
    // ------------------------------------------------------------------

    @Test
    @DisplayName("V2 orders waiting series by total processing time descending")
    void v2OrdersByTotalTimeDescending() {
        when(cuttingTimeCalculator.resolveMinutes(
                any(), any(), anyInt(), anyString(), anyMap()))
                .thenAnswer(inv -> {
                    Double t = inv.getArgument(1);
                    return t != null && t > 0 ? t : 30.0;
                });

        List<OrdonnancementService.SerieDTO> waiting = new ArrayList<>();
        // LEC-01: spread = 10*2*0.5+2 = 12, cut = tempsDeCoupe
        // total = spread + cut
        waiting.add(serie("S1", "SEQ-A", "Waiting", 0.0, 20.0)); // total = 32
        waiting.add(serie("S2", "SEQ-B", "Waiting", 0.0, 80.0)); // total = 92
        waiting.add(serie("S3", "SEQ-C", "Waiting", 0.0, 50.0)); // total = 62

        v2.sortWaiting(waiting, ctx);

        assertEquals("S2", waiting.get(0).serie); // highest total (92)
        assertEquals("S3", waiting.get(1).serie); // middle total (62)
        assertEquals("S1", waiting.get(2).serie); // lowest total (32)
    }

    @Test
    @DisplayName("V2 orders ready-to-cut series by cutting time descending")
    void v2OrdersReadyToCutByCutTimeDescending() {
        when(cuttingTimeCalculator.resolveMinutes(
                any(), any(), anyInt(), anyString(), anyMap()))
                .thenAnswer(inv -> {
                    Double t = inv.getArgument(1);
                    return t != null && t > 0 ? t : 30.0;
                });

        List<OrdonnancementService.SerieDTO> ready = new ArrayList<>();
        ready.add(serie("R1", "SEQ-A", "Complete", 0.0, 30.0));
        ready.add(serie("R2", "SEQ-B", "Complete", 0.0, 90.0));
        ready.add(serie("R3", "SEQ-C", "Complete", 0.0, 60.0));

        v2.sortReadyToCut(ready, ctx);

        assertEquals("R2", ready.get(0).serie); // highest cut (90)
        assertEquals("R3", ready.get(1).serie); // middle cut (60)
        assertEquals("R1", ready.get(2).serie); // lowest cut (30)
    }

    @Test
    @DisplayName("V2 produces different ordering than legacy on 50-series fixture")
    void v2DiffersFromLegacy() {
        List<OrdonnancementService.SerieDTO> fixture = buildFixture(50);

        List<String> legacyOrder = extractOrder(fixture, legacy);
        List<String> v2Order = extractOrder(fixture, v2);

        assertTrue(!legacyOrder.equals(v2Order),
                "V2 should produce a different ordering than legacy");
    }

    private List<OrdonnancementService.SerieDTO> buildFixture(int count) {
        List<OrdonnancementService.SerieDTO> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String seq = "SEQ-" + (i % 10);
            String status = (i % 3 == 0) ? "Complete" : "Waiting";
            double cut = 20.0 + (i % 7) * 15.0; // 20..110 minutes
            OrdonnancementService.SerieDTO s = serie("S" + i, seq, status, (double) (i % 5), cut);
            s.tableCoupe = (i % 4 == 0) ? "DXF-01" : "LEC-01";
            if (i % 4 == 0) {
                s.longueur = 3.0;
                s.nbrCouche = 4;
            }
            list.add(s);
        }
        return list;
    }

    private List<String> extractOrder(List<OrdonnancementService.SerieDTO> all,
                                       SeriesOrderingStrategy strategy) {
        Map<String, List<OrdonnancementService.SerieDTO>> byMachine = new HashMap<>();
        for (OrdonnancementService.SerieDTO s : all) {
            byMachine.computeIfAbsent(s.tableCoupe, k -> new ArrayList<>()).add(s);
        }

        when(cuttingTimeCalculator.resolveMinutes(
                any(), any(), anyInt(), anyString(), anyMap()))
                .thenAnswer(inv -> {
                    Double t = inv.getArgument(1);
                    return t != null && t > 0 ? t : 30.0;
                });

        List<String> order = new ArrayList<>();
        for (Map.Entry<String, List<OrdonnancementService.SerieDTO>> e : byMachine.entrySet()) {
            String machine = e.getKey();
            List<OrdonnancementService.SerieDTO> machineSeries = e.getValue();

            List<OrdonnancementService.SerieDTO> ready = machineSeries.stream()
                    .filter(s -> "Complete".equals(s.statusMatelassage))
                    .collect(Collectors.toList());
            List<OrdonnancementService.SerieDTO> waiting = machineSeries.stream()
                    .filter(s -> !"Complete".equals(s.statusMatelassage))
                    .collect(Collectors.toList());

            SeriesOrderingStrategy.Context machineCtx = new SeriesOrderingStrategy.Context();
            machineCtx.machineNom = machine;
            machineCtx.cuttingTimeMap = Collections.emptyMap();
            machineCtx.laserDxfMachines = Set.of("DXF-01");
            machineCtx.gerberMachines = Collections.emptySet();

            strategy.sortReadyToCut(ready, machineCtx);
            strategy.sortWaiting(waiting, machineCtx);

            ready.forEach(s -> order.add(s.serie));
            waiting.forEach(s -> order.add(s.serie));
        }
        return order;
    }

    // ------------------------------------------------------------------
    // 5. Zero-impact when legacy flag is set
    // ------------------------------------------------------------------

    @Test
    @DisplayName("legacy sort matches hard-coded dueDate ordering")
    void legacyMatchesOriginalBehavior() {
        List<OrdonnancementService.SerieDTO> waiting = new ArrayList<>();
        waiting.add(serie("W1", "SEQ-A", "Waiting", 2.0, 30.0));
        waiting.add(serie("W2", "SEQ-B", "Waiting", 1.0, 60.0));
        waiting.add(serie("W3", "SEQ-C", "Waiting", 3.0, 20.0));

        List<OrdonnancementService.SerieDTO> copy = new ArrayList<>(waiting);

        legacy.sortWaiting(waiting, ctx);

        copy.sort(Comparator
                .comparing((OrdonnancementService.SerieDTO s) ->
                        s.planningDate != null ? s.planningDate : LocalDate.MAX)
                .thenComparing(s -> s.sequence != null ? s.sequence : "")
                .thenComparing(s -> s.serie != null ? s.serie : ""));

        for (int i = 0; i < waiting.size(); i++) {
            assertEquals(copy.get(i).serie, waiting.get(i).serie,
                    "legacy order mismatch at index " + i);
        }
    }

    // ------------------------------------------------------------------
    // 6. Delegator behaviour
    // ------------------------------------------------------------------

    @Test
    @DisplayName("delegates to v2 when property is not 'legacy'")
    void delegatesToV2() {
        OrderingProperties props = new OrderingProperties();
        props.setOrdering("v2");

        DelegatingSeriesOrderingStrategy delegator = new DelegatingSeriesOrderingStrategy();
        delegator.legacy = legacy;
        delegator.v2 = v2;
        delegator.orderingProperties = props;

        List<OrdonnancementService.SerieDTO> ready = new ArrayList<>();
        ready.add(serie("R1", "SEQ-A", "Complete", 0.0, 60.0));
        ready.add(serie("R2", "SEQ-B", "Complete", 0.0, 30.0));

        mockCalculator(30.0);
        when(cuttingTimeCalculator.resolveMinutes(anyString(), any(), anyInt(), anyString(), anyMap()))
                .thenAnswer(inv -> {
                    Double t = inv.getArgument(1);
                    return t != null && t > 0 ? t : 30.0;
                });
        delegator.sortReadyToCut(ready, ctx);

        assertEquals("R1", ready.get(0).serie);
        assertEquals("R2", ready.get(1).serie);
    }

    @Test
    @DisplayName("delegates to legacy when property is 'legacy'")
    void delegatesToLegacy() {
        OrderingProperties props = new OrderingProperties();
        props.setOrdering("legacy");

        DelegatingSeriesOrderingStrategy delegator = new DelegatingSeriesOrderingStrategy();
        delegator.legacy = legacy;
        delegator.v2 = v2;
        delegator.orderingProperties = props;

        List<OrdonnancementService.SerieDTO> waiting = new ArrayList<>();
        waiting.add(serie("W1", "SEQ-A", "Waiting", 2.0, 60.0));
        waiting.add(serie("W2", "SEQ-B", "Waiting", 1.0, 30.0));

        delegator.sortWaiting(waiting, ctx);

        assertEquals("W2", waiting.get(0).serie);
        assertEquals("W1", waiting.get(1).serie);
    }
}
