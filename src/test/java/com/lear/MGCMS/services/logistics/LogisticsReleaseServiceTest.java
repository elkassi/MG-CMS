package com.lear.MGCMS.services.logistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lear.MGCMS.domain.CuttingRequest.SequenceStatus;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.logistics.LogisticsAllocation;
import com.lear.MGCMS.domain.logistics.LogisticsPicklist;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.logistics.LogisticsPicklistRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.dispatcher.LiveChargeDto;
import com.lear.MGCMS.services.dispatcher.LiveChargeService;
import com.lear.cms.repositories.SuiviPlanningRepository;

class LogisticsReleaseServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    @Mock private LiveChargeService liveChargeService;
    @Mock private ScanRouleauRepository scanRouleauRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private SuiviPlanningRepository suiviPlanningRepository;
    @Mock private AllocationService allocationService;
    @Mock private SerieRouleauTempService serieRouleauTempService;
    @Mock private CuttingRequestRepository cuttingRequestRepository;
    @Mock private CuttingRequestSerieDataRepository cuttingRequestSerieDataRepository;
    @Mock private SequenceStatusService sequenceStatusService;
    @Mock private LogisticsPicklistRepository logisticsPicklistRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private LogisticsReleaseService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(allocationService.reservedMetersByMaterialZone()).thenReturn(Collections.emptyMap());
        when(allocationService.reserve(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(serieRouleauTempService.getAll()).thenReturn(Collections.emptyList());
        when(cuttingRequestRepository.findInProductionMaterials()).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("lists the IMPORTED candidates and flags out-of-zone material with a transfer roll while in-zone material is OK")
    @SuppressWarnings("unchecked")
    void build_releaseAdvice() {
        when(zoneRepository.findAllActive()).thenReturn(Arrays.asList(zone("Z1", "R1"), zone("Z2", "R2")));
        when(scanRouleauRepository.findAllLight()).thenReturn(Arrays.asList(
                rack("roll-aa", "PAA", "R2", 50.0),
                rack("roll-bb", "PBB", "R1", 40.0)));

        // Two IMPORTED candidates in Z1: one needs AA (only on a Z2 rack), one needs BB (on its own Z1 rack).
        stubCandidates(
                importedRow("seq-open", "Z1"),
                importedRow("seq-ready", "Z1"));
        when(cuttingRequestSerieDataRepository.findBySequencesArr(any())).thenReturn(Arrays.asList(
                serieEntity("seq-open", "ser-open", "AA", 20.0),
                serieEntity("seq-ready", "ser-ready", "BB", 10.0)));

        Map<String, Object> result = service.build(TODAY, 2);

        List<Map<String, Object>> sequences = (List<Map<String, Object>>) result.get("sequences");
        assertEquals(2, sequences.size());

        Map<String, Object> openAdvice = find(sequences, "seq-open");
        assertEquals("Z1", openAdvice.get("suggestedZone"));
        assertEquals("OUT_OF_ZONE", openAdvice.get("materialStatus"));

        List<Map<String, Object>> openSeries = (List<Map<String, Object>>) openAdvice.get("series");
        List<Map<String, Object>> placements = (List<Map<String, Object>>) openSeries.get(0).get("rollPlacements");
        assertFalse(placements.isEmpty(), "out-of-zone material should suggest a transfer roll");
        assertEquals("roll-aa", placements.get(0).get("serialId"));
        assertEquals(Boolean.FALSE, placements.get(0).get("inTargetZone"));
        assertEquals("Z2", placements.get(0).get("sourceZone"));

        Map<String, Object> readyAdvice = find(sequences, "seq-ready");
        assertEquals("OK", readyAdvice.get("materialStatus"));
        assertEquals("Z1", readyAdvice.get("suggestedZone"));

        List<Map<String, Object>> materials = (List<Map<String, Object>>) result.get("materials");
        Set<String> refs = new HashSet<>();
        for (Map<String, Object> m : materials) refs.add(String.valueOf(m.get("refTissus")));
        assertTrue(refs.contains("AA"), "material grouping should list AA");
        assertTrue(refs.contains("BB"), "material grouping should list BB");

        Map<String, Object> totals = (Map<String, Object>) result.get("totals");
        assertEquals(2, totals.get("candidateCount"));
    }

    @Test
    @DisplayName("commit flips suiviplanning, mirrors RELEASED, writes allocations, and stores a printable snapshot")
    @SuppressWarnings("unchecked")
    void commit_writesPicklistAndAllocations() throws Exception {
        when(zoneRepository.findAllActive()).thenReturn(Collections.singletonList(zone("Z1", "R1")));
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.singletonList(
                rack("roll-bb", "PBB", "R1", 40.0)));
        stubCandidates(importedRow("seq-ready", "Z1"));
        when(cuttingRequestSerieDataRepository.findBySequencesArr(any())).thenReturn(Collections.singletonList(
                serieEntity("seq-ready", "ser-ready", "BB", 10.0)));

        // commit re-reads suiviplanning after the flip to confirm Released.
        when(suiviPlanningRepository.findStatuBySequences(any()))
                .thenReturn(Collections.singletonList(new Object[]{"seq-ready", "Released"}));
        when(suiviPlanningRepository.releaseNonDemarreBySequences(anyList())).thenReturn(1);
        when(sequenceStatusService.transition(eq("seq-ready"), eq(SequenceStatus.RELEASED), eq("Z1")))
                .thenReturn(success("seq-ready", SequenceStatus.RELEASED));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(logisticsPicklistRepository.save(any(LogisticsPicklist.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.commit(TODAY, 2, Collections.singletonList("seq-ready"), "u1");

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals(1, result.get("suiviUpdatedRows"));
        assertEquals(1, result.get("allocationCount"));
        assertTrue(String.valueOf(result.get("picklistId")).startsWith("PL-"));

        verify(suiviPlanningRepository).releaseNonDemarreBySequences(Collections.singletonList("seq-ready"));
        verify(sequenceStatusService).transition("seq-ready", SequenceStatus.RELEASED, "Z1");

        ArgumentCaptor<List<LogisticsAllocation>> allocations = ArgumentCaptor.forClass(List.class);
        verify(allocationService).reserve(allocations.capture());
        assertEquals(1, allocations.getValue().size());
        LogisticsAllocation row = allocations.getValue().get(0);
        assertEquals("seq-ready", row.getSequence());
        assertEquals("ser-ready", row.getSerie());
        assertEquals("BB", row.getRefTissus());
        assertEquals("roll-bb", row.getSerialId());
        assertEquals("Z1", row.getTargetZone());
        assertEquals(10.0, row.getAllocatedMeters(), 0.001);
    }

    @Test
    @DisplayName("commit rejects a sequence when no serie can be spread from TRUE stock")
    void commit_blocksMaterialMissingSuggestion() {
        when(zoneRepository.findAllActive()).thenReturn(Collections.singletonList(zone("Z1", "R1")));
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.emptyList());
        stubCandidates(importedRow("seq-missing", "Z1"));
        when(cuttingRequestSerieDataRepository.findBySequencesArr(any())).thenReturn(Collections.singletonList(
                serieEntity("seq-missing", "ser-missing", "CC", 10.0)));

        Map<String, Object> result = service.commit(TODAY, 2, Collections.singletonList("seq-missing"), "u1");

        assertEquals(Boolean.FALSE, result.get("success"));
        assertTrue(String.valueOf(result.get("error")).contains("Matière insuffisante"));
        verify(suiviPlanningRepository, never()).releaseNonDemarreBySequences(anyList());
        verify(sequenceStatusService, never()).transition(any(), any(), any());
        verify(allocationService, never()).reserve(anyList());
    }

    // ------------------------------------------------------------------ helpers

    /** Mock the IMPORTED candidate rows + a minimal single-zone charge context. */
    private void stubCandidates(Object[]... rows) {
        when(cuttingRequestRepository.findImportedDueOnOrBeforeLight(any(), any()))
                .thenReturn(new ArrayList<>(Arrays.asList(rows)));

        LiveChargeDto.MachineTypeChargeDto mt = new LiveChargeDto.MachineTypeChargeDto(
                "Lectra", "Coupe", 1, 480, 90.0, 432.0, 0.0, 200.0, 200.0, 46.0);
        LiveChargeDto.ZoneChargeDto z1 = new LiveChargeDto.ZoneChargeDto(
                "Z1", "STRICT", Collections.singletonList(mt),
                Collections.emptyList(), Collections.emptyList(), 200.0, 432.0, 46.0);
        when(liveChargeService.compute()).thenReturn(new LiveChargeDto(
                LocalDateTime.now(), TODAY, 2, 480,
                new LiveChargeDto.TotalsDto(0, 0, 0, 0, 200.0, 432.0),
                Collections.singletonList(z1), Collections.emptyList()));
    }

    private Object[] importedRow(String sequence, String zone) {
        // projection: 0=sequence, 1=dispatchedZone, 2=zoneNom, 3=dueDate, 4=dueShift, 5=releaseZone
        return new Object[]{sequence, zone, zone, TODAY, "2", null};
    }

    private CuttingRequestSerieData serieEntity(String sequence, String serie, String material, double meters) {
        CuttingRequestSerieData s = new CuttingRequestSerieData();
        s.setSequence(sequence);
        s.setSerie(serie);
        s.setMachine("Lectra");
        s.setPartNumberMaterial(material);
        s.setLongueur(meters);
        s.setNbrCouche(1);
        s.setStatusCoupe("Waiting");
        s.setStatusMatelassage("Waiting");
        s.setTempsDeCoupe(20.0);
        return s;
    }

    private Map<String, Object> find(List<Map<String, Object>> list, String seq) {
        return list.stream().filter(s -> seq.equals(s.get("sequence"))).findFirst().orElseThrow();
    }

    private Map<String, Object> success(String sequence, String status) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("success", true);
        out.put("sequence", sequence);
        out.put("newStatus", status);
        return out;
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
}
