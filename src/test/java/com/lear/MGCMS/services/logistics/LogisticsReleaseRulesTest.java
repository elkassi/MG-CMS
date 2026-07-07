package com.lear.MGCMS.services.logistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.dispatcher.LiveChargeDto;
import com.lear.MGCMS.services.dispatcher.LiveChargeService;
import com.lear.cms.repositories.SuiviPlanningRepository;

/**
 * Unit tests for the TRUE-stock deduction and the three transfer/return rules
 * (RULE 1 oversized→transfer, RULE 2 insufficient→smallest-sufficient fill with
 * anti-double-counting, RULE 3 no-serie→return) plus the advisory
 * {@code materialMissingSuggested} flag in {@link LogisticsReleaseService}.
 *
 * <p>Candidates are the IMPORTED sequences loaded via
 * {@code findImportedDueOnOrBeforeLight} + {@code findBySequencesArr}; the live
 * charge only supplies per-zone capacity context for the zone recommendation.</p>
 */
class LogisticsReleaseRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    @Mock private LiveChargeService liveChargeService;
    @Mock private ScanRouleauRepository scanRouleauRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private SuiviPlanningRepository suiviPlanningRepository;
    @Mock private AllocationService allocationService;
    @Mock private SerieRouleauTempService serieRouleauTempService;
    @Mock private CuttingRequestRepository cuttingRequestRepository;
    @Mock private CuttingRequestSerieDataRepository cuttingRequestSerieDataRepository;

    @InjectMocks private LogisticsReleaseService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Two zones: R1→Z1, R2→Z2.
        when(zoneRepository.findAllActive()).thenReturn(Arrays.asList(zone("Z1", "R1"), zone("Z2", "R2")));
        // Sensible empty defaults — individual tests override what they need.
        when(allocationService.reservedMetersByMaterialZone()).thenReturn(Collections.emptyMap());
        when(serieRouleauTempService.getAll()).thenReturn(Collections.emptyList());
        when(cuttingRequestRepository.findInProductionMaterials()).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("TRUE stock subtracts both allocation reservations and on-table SerieRouleauTemp")
    @SuppressWarnings("unchecked")
    void trueStock_deductsReservationsAndOnTable() {
        // 100m of AA on rack R1 (zone Z1).
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.singletonList(
                rack("r1", "PAA", "R1", 100.0)));
        // 30m reserved for AA|Z1, plus 20m of roll r1 pulled onto a table.
        Map<String, Double> reserved = new LinkedHashMap<>();
        reserved.put("AA|Z1", 30.0);
        when(allocationService.reservedMetersByMaterialZone()).thenReturn(reserved);
        when(serieRouleauTempService.getAll()).thenReturn(Collections.singletonList(
                onTable("r1", "PAA", 20.0)));

        // One sequence in Z1 needing 40m of AA.
        stubCandidates(seq("s1", "Z1", serie("se1", "AA", "Z1", 40.0)));

        Map<String, Object> result = service.build(TODAY, 2);

        Map<String, Object> aa = material(result, "AA");
        // 100 - 30 (reserved) - 20 (on table) = 50 available.
        assertEquals(50.0, ((Number) aa.get("availableTotal")).doubleValue(), 0.001);
    }

    @Test
    @DisplayName("RULE 1: a roll bigger than total cross-zone need gets a transferAfterUse to the next zone")
    @SuppressWarnings("unchecked")
    void rule1_oversizedRollTransfersAfterUse() {
        // One 200m roll of AA in Z1.
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.singletonList(
                rack("big", "PAA", "R1", 200.0)));
        // Z1 (due earlier) needs 30, Z2 (due later) needs 30 → totalNeed 60 < 200.
        stubCandidates(
                seqDue("s1", "Z1", TODAY, serie("se1", "AA", "Z1", 30.0)),
                seqDue("s2", "Z2", TODAY.plusDays(1), serie("se2", "AA", "Z2", 30.0)));

        Map<String, Object> result = service.build(TODAY, 2);

        List<Map<String, Object>> transfers = (List<Map<String, Object>>) result.get("transferAfterUse");
        assertEquals(1, transfers.size());
        Map<String, Object> step = transfers.get(0);
        assertEquals("big", step.get("serialId"));
        assertEquals("Z1", step.get("useInZone"));
        assertEquals("Z2", step.get("transferAfterUseTo"));

        // The step is stamped onto the matching roll placement in Z1's serie.
        Map<String, Object> s1 = sequence(result, "s1");
        List<Map<String, Object>> series = (List<Map<String, Object>>) s1.get("series");
        List<Map<String, Object>> placements = (List<Map<String, Object>>) series.get(0).get("rollPlacements");
        assertEquals("Z2", placements.get(0).get("transferAfterUse"));
    }

    @Test
    @DisplayName("RULE 2: shortage fills the priority zone with the fewest rolls and never double-counts the next zone")
    @SuppressWarnings("unchecked")
    void rule2_smallestSufficientFill_noDoubleCount() {
        // Two rolls 40 + 30 = 70m total.
        when(scanRouleauRepository.findAllLight()).thenReturn(Arrays.asList(
                rack("r40", "PAA", "R1", 40.0),
                rack("r30", "PAA", "R1", 30.0)));
        // Z1 (earlier) needs 50, Z2 (later) needs 50 → totalNeed 100 > 70 (shortage).
        stubCandidates(
                seqDue("s1", "Z1", TODAY, serie("se1", "AA", "Z1", 50.0)),
                seqDue("s2", "Z2", TODAY.plusDays(1), serie("se2", "AA", "Z2", 50.0)));

        Map<String, Object> result = service.build(TODAY, 2);

        List<Map<String, Object>> fill = (List<Map<String, Object>>) result.get("fillPlan");
        // Only Z1 (priority) gets a plan; Z2 sees no rolls left → anti-double-count.
        List<Map<String, Object>> z1Plans = new ArrayList<>();
        List<Map<String, Object>> z2Plans = new ArrayList<>();
        for (Map<String, Object> p : fill) {
            if ("Z1".equals(p.get("zone"))) z1Plans.add(p);
            if ("Z2".equals(p.get("zone"))) z2Plans.add(p);
        }
        assertEquals(1, z1Plans.size(), "priority zone Z1 should be filled");
        assertTrue(z2Plans.isEmpty(), "Z2 must not reuse rolls already reserved for Z1");

        Map<String, Object> z1 = z1Plans.get(0);
        List<Map<String, Object>> rolls = (List<Map<String, Object>>) z1.get("rolls");
        assertEquals(2, rolls.size(), "smallest-sufficient: 40 + 30 clears the 50 gap");
        assertEquals(Boolean.TRUE, z1.get("sufficient"));
        assertEquals(70.0, ((Number) z1.get("covered")).doubleValue(), 0.001);
    }

    @Test
    @DisplayName("RULE 3: a rack roll whose material no in-production serie needs is flagged return-to-magasin")
    @SuppressWarnings("unchecked")
    void rule3_returnToMagasin() {
        when(scanRouleauRepository.findAllLight()).thenReturn(Arrays.asList(
                rack("rAA", "PAA", "R1", 50.0),
                rack("rBB", "PBB", "R2", 60.0)));
        // Only AA is in production; BB is not needed by anyone.
        when(cuttingRequestRepository.findInProductionMaterials()).thenReturn(Collections.singletonList("AA"));
        stubCandidates(seq("s1", "Z1", serie("se1", "AA", "Z1", 30.0)));

        Map<String, Object> result = service.build(TODAY, 2);

        List<Map<String, Object>> returns = (List<Map<String, Object>>) result.get("returnToMagasin");
        assertEquals(1, returns.size());
        assertEquals("rBB", returns.get(0).get("serialId"));
        assertEquals("BB", returns.get(0).get("refTissus"));
    }

    @Test
    @DisplayName("materialMissingSuggested is true when no serie can be spread from TRUE stock, false otherwise")
    @SuppressWarnings("unchecked")
    void materialMissingSuggested_flag() {
        // AA has plenty of stock; CC has none on any rack.
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.singletonList(
                rack("rAA", "PAA", "R1", 500.0)));
        stubCandidates(
                seq("s-ok", "Z1", serie("se-ok", "AA", "Z1", 100.0)),
                seq("s-missing", "Z2", serie("se-missing", "CC", "Z2", 100.0)));

        Map<String, Object> result = service.build(TODAY, 2);

        Map<String, Object> ok = sequence(result, "s-ok");
        Map<String, Object> missing = sequence(result, "s-missing");
        assertEquals(Boolean.FALSE, ok.get("materialMissingSuggested"));
        assertEquals(Boolean.TRUE, missing.get("materialMissingSuggested"));
    }

    @Test
    @DisplayName("dispatch consolidates same-material sequences into one zone (fewest distinct refs)")
    @SuppressWarnings("unchecked")
    void dispatchConsolidatesSameMaterialIntoOneZone() {
        // No rack stock anywhere → locality is 0 for every zone, isolating the material
        // CONSOLIDATION signal from locality. Two AA sequences at equal charge must land in
        // the SAME zone so that zone finishes with the fewest distinct fabric refs.
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.emptyList());
        stubCandidates(
                seq("a1", "Z1", serie("sa1", "AA", "Z1", 10.0)),
                seq("a2", "Z1", serie("sa2", "AA", "Z1", 10.0)));

        Map<String, Object> result = service.build(TODAY, 2);

        String za1 = (String) sequence(result, "a1").get("suggestedZone");
        String za2 = (String) sequence(result, "a2").get("suggestedZone");
        assertEquals(za1, za2,
                "two same-material sequences must consolidate into one zone to minimise distinct refs");
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Translate the SequenceDto/SerieDto fixtures into the IMPORTED candidate
     * source (rows + serie entities) the service now reads, and supply a minimal
     * two-zone charge context (no sequences live there anymore).
     */
    private void stubCandidates(LiveChargeDto.SequenceDto... seqs) {
        List<Object[]> rows = new ArrayList<>();
        List<CuttingRequestSerieData> series = new ArrayList<>();
        for (LiveChargeDto.SequenceDto s : seqs) {
            rows.add(new Object[]{s.getSequence(), s.getEffectiveZone(), s.getEffectiveZone(),
                    s.getDueDate(), "2", null});
            for (LiveChargeDto.SerieDto se : s.getSeries()) {
                series.add(serieEntity(s.getSequence(), se.getSerie(), se.getMachine(),
                        se.getRefTissus(), se.getStatusCoupe(), se.getTableLengthRequired(),
                        se.getTempsDeCoupe()));
            }
        }
        when(cuttingRequestRepository.findImportedDueOnOrBeforeLight(any(), any())).thenReturn(rows);
        when(cuttingRequestSerieDataRepository.findBySequencesArr(any())).thenReturn(series);

        LiveChargeDto.MachineTypeChargeDto mt = new LiveChargeDto.MachineTypeChargeDto(
                "Lectra", "Coupe", 1, 480, 90.0, 432.0, 0.0, 100.0, 100.0, 23.0);
        LiveChargeDto.ZoneChargeDto z1 = new LiveChargeDto.ZoneChargeDto(
                "Z1", "STRICT", Collections.singletonList(mt),
                Collections.emptyList(), Collections.emptyList(), 100.0, 432.0, 23.0);
        LiveChargeDto.ZoneChargeDto z2 = new LiveChargeDto.ZoneChargeDto(
                "Z2", "STRICT", Collections.singletonList(mt),
                Collections.emptyList(), Collections.emptyList(), 100.0, 432.0, 23.0);
        when(liveChargeService.compute()).thenReturn(new LiveChargeDto(
                LocalDateTime.now(), TODAY, 2, 480,
                new LiveChargeDto.TotalsDto(0, 0, 0, 0, 200.0, 864.0),
                Arrays.asList(z1, z2), Collections.emptyList()));
    }

    private Zone zone(String nom, String locations) {
        Zone z = new Zone();
        z.setNom(nom);
        z.setCategory(Zone.Category.STRICT);
        z.setRollLocations(locations);
        z.setActive(true);
        return z;
    }

    private Object[] rack(String serialId, String reftissu, String emplacement, double metrage) {
        // projection order: serialId, reftissu, quantite, emplacement, lot, metrage
        return new Object[]{serialId, reftissu, 0.0, emplacement, "lot", metrage};
    }

    private SerieRouleauTemp onTable(String idRouleau, String reftissu, double estimationRest) {
        SerieRouleauTemp t = new SerieRouleauTemp();
        t.setTableMatelassage("T-" + idRouleau);
        t.setIdRouleau(idRouleau);
        t.setReftissu(reftissu);
        t.setEstimationRest(estimationRest);
        return t;
    }

    private CuttingRequestSerieData serieEntity(String sequence, String serie, String machine,
                                                String material, String statusCoupe,
                                                double meters, Double tempsDeCoupe) {
        CuttingRequestSerieData s = new CuttingRequestSerieData();
        s.setSequence(sequence);
        s.setSerie(serie);
        s.setMachine(machine);
        s.setPartNumberMaterial(material);
        s.setLongueur(meters);
        s.setNbrCouche(1);
        s.setStatusCoupe(statusCoupe);
        s.setStatusMatelassage(statusCoupe);
        s.setTempsDeCoupe(tempsDeCoupe);
        return s;
    }

    private LiveChargeDto.SerieDto serie(String serieId, String material, String targetZone, double meters) {
        return new LiveChargeDto.SerieDto(
                serieId, "Lectra", 20.0, 20.0, "TEMPS_DE_COUPE",
                "Waiting", "Waiting", null, null,
                null, null, null, null,
                0.0, meters, targetZone, "STRICT",
                material, null, meters);
    }

    private LiveChargeDto.SequenceDto seq(String id, String zone, LiveChargeDto.SerieDto serie) {
        return seqDue(id, zone, TODAY, serie);
    }

    private LiveChargeDto.SequenceDto seqDue(String id, String zone, LocalDate due, LiveChargeDto.SerieDto serie) {
        return new LiveChargeDto.SequenceDto(
                id, zone, zone, zone, "DISPATCHED",
                false, false, null, null, null, null,
                false, "ACCEPTED", serie.getTableLengthRequired(),
                Collections.singletonList(serie),
                due, 0.0, null);
    }

    private Map<String, Object> sequence(Map<String, Object> result, String id) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sequences = (List<Map<String, Object>>) result.get("sequences");
        return sequences.stream().filter(s -> id.equals(s.get("sequence"))).findFirst().orElseThrow();
    }

    private Map<String, Object> material(Map<String, Object> result, String ref) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> materials = (List<Map<String, Object>>) result.get("materials");
        return materials.stream().filter(m -> ref.equals(m.get("refTissus"))).findFirst().orElseThrow();
    }
}
