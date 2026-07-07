package com.lear.MGCMS.services.logistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.StockStatusReport;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.logistics.LogisticsPicklistRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.StockStatusReportService;
import com.lear.MGCMS.services.dispatcher.LiveChargeService;
import com.lear.cms.repositories.SuiviPlanningRepository;

/**
 * Focused unit tests for {@link LogisticsReleaseService#recap(List)} — the
 * material balance (rack + on-going vs committed + new) and the magasin fallback.
 */
class LogisticsReleaseRecapTest {

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
    @Mock private CuttingRequestBoxInfoRepository boxInfoRepository;
    @Mock private StockStatusReportService stockStatusReportService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private LogisticsReleaseService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(zoneRepository.findAllActive()).thenReturn(Collections.emptyList());
        when(serieRouleauTempService.getAll()).thenReturn(Collections.emptyList());
        when(cuttingRequestSerieDataRepository.sumCommittedWaitingMetersByMaterial())
                .thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("OK: rack covers committed + new, magasin is never consulted")
    @SuppressWarnings("unchecked")
    void recap_ok_whenRackCovers() {
        // need = 20 (AA), rack = 50 (PAA), committed = 0 -> remaining = 30 >= 0.
        when(cuttingRequestSerieDataRepository.findBySequencesArr(any())).thenReturn(Collections.singletonList(
                serie("seq-1", "ser-1", "AA", 20.0)));
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.singletonList(
                rack("roll-aa", "PAA", 50.0)));

        Map<String, Object> result = service.recap(Collections.singletonList("seq-1"));

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals(Boolean.TRUE, result.get("canConfirm"));
        assertEquals(1, result.get("selectedCount"));

        Map<String, Object> aa = findMaterial(result, "AA");
        assertEquals("OK", aa.get("status"));
        assertEquals(20.0, ((Number) aa.get("newMeters")).doubleValue(), 0.001);
        assertEquals(0.0, ((Number) aa.get("committedMeters")).doubleValue(), 0.001);
        assertEquals(50.0, ((Number) aa.get("availableMeters")).doubleValue(), 0.001);
        assertEquals(30.0, ((Number) aa.get("remainingMeters")).doubleValue(), 0.001);
        assertTrue(((List<?>) aa.get("magasinPull")).isEmpty());

        Map<String, Object> totals = (Map<String, Object>) result.get("totals");
        assertEquals(1, totals.get("materialsOk"));
        assertEquals(0, totals.get("materialsShort"));

        // R100.prn parse must not run when there is no deficit.
        verify(stockStatusReportService, never()).getCurrentStock(anyList());
    }

    @Test
    @DisplayName("COVERED: rack short, magasin makes up the deficit and FIFO pull is produced")
    @SuppressWarnings("unchecked")
    void recap_covered_whenMagasinFillsDeficit() {
        // need = 20 (AA), rack = 10 -> deficit 10; magasin has 6 + 9 = 15 >= 10.
        when(cuttingRequestSerieDataRepository.findBySequencesArr(any())).thenReturn(Collections.singletonList(
                serie("seq-1", "ser-1", "AA", 20.0)));
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.singletonList(
                rack("roll-aa", "PAA", 10.0)));
        when(stockStatusReportService.getCurrentStock(anyList())).thenReturn(Arrays.asList(
                magasin("T01-A", "AA", 9.0, LocalDateTime.of(2026, 5, 2, 8, 0)),
                magasin("T01-B", "AA", 6.0, LocalDateTime.of(2026, 5, 1, 8, 0))));

        Map<String, Object> result = service.recap(Collections.singletonList("seq-1"));

        assertEquals(Boolean.TRUE, result.get("canConfirm"));
        Map<String, Object> aa = findMaterial(result, "AA");
        assertEquals("COVERED", aa.get("status"));
        assertEquals(-10.0, ((Number) aa.get("remainingMeters")).doubleValue(), 0.001);
        assertEquals(15.0, ((Number) aa.get("magasinMeters")).doubleValue(), 0.001);

        // FIFO: oldest (May 1, T01-B) first; covers 6 then 9 -> 15 >= 10 (both rows).
        List<Map<String, Object>> pull = (List<Map<String, Object>>) aa.get("magasinPull");
        assertEquals(2, pull.size());
        assertEquals("T01-B", pull.get(0).get("location"));
        assertEquals("T01-A", pull.get(1).get("location"));

        Map<String, Object> totals = (Map<String, Object>) result.get("totals");
        assertEquals(1, totals.get("materialsCovered"));
        assertEquals(0, totals.get("materialsShort"));

        // Both the normalized ref and the P-prefixed ref are offered to the R100 lookup.
        verify(stockStatusReportService).getCurrentStock(Arrays.asList("AA", "PAA"));
    }

    @Test
    @DisplayName("SHORTAGE: neither rack nor magasin covers the demand, batch cannot be confirmed")
    @SuppressWarnings("unchecked")
    void recap_shortage_whenNeitherCovers() {
        // need = 20 (AA), rack = 5 -> deficit 15; magasin empty -> shortage.
        when(cuttingRequestSerieDataRepository.findBySequencesArr(any())).thenReturn(Collections.singletonList(
                serie("seq-1", "ser-1", "AA", 20.0)));
        when(scanRouleauRepository.findAllLight()).thenReturn(Collections.singletonList(
                rack("roll-aa", "PAA", 5.0)));
        when(stockStatusReportService.getCurrentStock(anyList())).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.recap(Collections.singletonList("seq-1"));

        assertEquals(Boolean.FALSE, result.get("canConfirm"));
        Map<String, Object> aa = findMaterial(result, "AA");
        assertEquals("SHORTAGE", aa.get("status"));
        assertEquals(0.0, ((Number) aa.get("magasinMeters")).doubleValue(), 0.001);
        assertTrue(((List<?>) aa.get("magasinPull")).isEmpty());

        Map<String, Object> totals = (Map<String, Object>) result.get("totals");
        assertEquals(1, totals.get("materialsShort"));
    }

    @Test
    @DisplayName("empty selection is rejected before any lookup")
    void recap_emptySelection() {
        Map<String, Object> result = service.recap(Collections.emptyList());

        assertEquals(Boolean.FALSE, result.get("success"));
        assertEquals("Sélection vide", result.get("error"));
        assertTrue(((List<?>) result.get("materials")).isEmpty());
        verify(stockStatusReportService, never()).getCurrentStock(anyList());
    }

    // ------------------------------------------------------------------ helpers

    @SuppressWarnings("unchecked")
    private Map<String, Object> findMaterial(Map<String, Object> result, String ref) {
        List<Map<String, Object>> materials = (List<Map<String, Object>>) result.get("materials");
        return materials.stream()
                .filter(m -> ref.equals(m.get("refTissus")))
                .findFirst()
                .orElseThrow();
    }

    private CuttingRequestSerieData serie(String sequence, String serie, String material, double longueur) {
        CuttingRequestSerieData s = new CuttingRequestSerieData();
        s.setSequence(sequence);
        s.setSerie(serie);
        s.setPartNumberMaterial(material);
        s.setLongueur(longueur);
        s.setNbrCouche(1);
        s.setStatusCoupe("Waiting");
        s.setStatusMatelassage("Waiting");
        return s;
    }

    private Object[] rack(String serialId, String reftissu, double metrage) {
        // projection order: serialId, reftissu, quantite, emplacement, lot, metrage
        return new Object[]{serialId, reftissu, 0.0, "T0R1", "lot", metrage};
    }

    private StockStatusReport magasin(String location, String material, double qty, LocalDateTime lastUpdated) {
        // R100 semantics: itemNumber = material, ref = roll serial, location = rack.
        StockStatusReport report = new StockStatusReport();
        report.setLocation(location);
        report.setItemNumber(material);
        report.setRef("ROLL-" + location);
        report.setQtyOnHand(qty);
        report.setLastUpdated(lastUpdated);
        return report;
    }
}
