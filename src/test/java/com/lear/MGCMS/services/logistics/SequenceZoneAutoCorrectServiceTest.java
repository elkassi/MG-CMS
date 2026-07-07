package com.lear.MGCMS.services.logistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.lear.MGCMS.domain.CuttingRequest.ReleaseZoneSource;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;

class SequenceZoneAutoCorrectServiceTest {

    @Mock private CuttingRequestDataRepository requestDataRepository;
    @Mock private CuttingRequestSerieDataRepository serieDataRepository;
    @Mock private ProductionTableRepository productionTableRepository;

    @InjectMocks private SequenceZoneAutoCorrectService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(service, "windowDays", 14);
    }

    private static Object[] candidate(String sequence, String zone, String source) {
        return new Object[] { sequence, zone, source };
    }

    private static Object[] serieRow(String sequence, String serie, String statusCoupe,
                                     String table, LocalDateTime debut) {
        return new Object[] { sequence, serie, statusCoupe, table, debut };
    }

    private static Object[] tableRow(String table, String zone, Zone.Category category) {
        return new Object[] { table, zone, category };
    }

    @Test
    void infersZone_fromStrictTable_andApplies() {
        when(requestDataRepository.findZoneAutofixCandidates(any(LocalDate.class)))
                .thenReturn(Collections.singletonList(candidate("SEQ1", null, null)));
        when(serieDataRepository.findLockInputsBySequences(anyList()))
                .thenReturn(Collections.singletonList(
                        serieRow("SEQ1", "S1", "Complete", "LECTRA1", LocalDateTime.now())));
        when(productionTableRepository.findZoneInfoByTableNoms(anyList()))
                .thenReturn(Collections.singletonList(tableRow("LECTRA1", "NEJMA", Zone.Category.STRICT)));
        when(requestDataRepository.applyAutoZone("SEQ1", "NEJMA")).thenReturn(1);

        Map<String, Object> r = service.runOnce();

        verify(requestDataRepository).applyAutoZone("SEQ1", "NEJMA");
        assertEquals(1, r.get("corrected"));
        assertEquals(1, r.get("examined"));
        assertEquals(0, r.get("locked"));
    }

    @Test
    void sharedTable_givesNoSignal() {
        when(requestDataRepository.findZoneAutofixCandidates(any(LocalDate.class)))
                .thenReturn(Collections.singletonList(candidate("SEQ1", null, null)));
        when(serieDataRepository.findLockInputsBySequences(anyList()))
                .thenReturn(Collections.singletonList(
                        serieRow("SEQ1", "S1", "In progress", "PRESSE1", LocalDateTime.now())));
        when(productionTableRepository.findZoneInfoByTableNoms(anyList()))
                .thenReturn(Collections.singletonList(tableRow("PRESSE1", "C/CAP", Zone.Category.SHARED)));

        Map<String, Object> r = service.runOnce();

        verify(requestDataRepository, never()).applyAutoZone(anyString(), anyString());
        assertEquals(0, r.get("corrected"));
        assertEquals(1, r.get("noSignal"));
    }

    @Test
    void logisticsAndChefZones_areLocked() {
        when(requestDataRepository.findZoneAutofixCandidates(any(LocalDate.class)))
                .thenReturn(Arrays.asList(
                        candidate("SEQ1", "Z1", ReleaseZoneSource.LOGISTICS),
                        candidate("SEQ2", "Z2", ReleaseZoneSource.CHEF)));

        Map<String, Object> r = service.runOnce();

        verify(requestDataRepository, never()).applyAutoZone(anyString(), anyString());
        assertEquals(2, r.get("locked"));
        assertEquals(0, r.get("examined"));
    }

    @Test
    void alreadyCorrectZone_isNotRewritten() {
        when(requestDataRepository.findZoneAutofixCandidates(any(LocalDate.class)))
                .thenReturn(Collections.singletonList(candidate("SEQ1", "NEJMA", ReleaseZoneSource.AUTO)));
        when(serieDataRepository.findLockInputsBySequences(anyList()))
                .thenReturn(Collections.singletonList(
                        serieRow("SEQ1", "S1", "Complete", "LECTRA1", LocalDateTime.now())));
        when(productionTableRepository.findZoneInfoByTableNoms(anyList()))
                .thenReturn(Collections.singletonList(tableRow("LECTRA1", "NEJMA", Zone.Category.STRICT)));

        Map<String, Object> r = service.runOnce();

        verify(requestDataRepository, never()).applyAutoZone(anyString(), anyString());
        assertEquals(0, r.get("corrected"));
    }

    @Test
    void lastWorkedSerie_winsAcrossStrictZones() {
        LocalDateTime older = LocalDateTime.now().minusHours(5);
        LocalDateTime newer = LocalDateTime.now();
        when(requestDataRepository.findZoneAutofixCandidates(any(LocalDate.class)))
                .thenReturn(Collections.singletonList(candidate("SEQ1", null, null)));
        when(serieDataRepository.findLockInputsBySequences(anyList()))
                .thenReturn(Arrays.asList(
                        serieRow("SEQ1", "S1", "Complete", "LECTRA1", older),
                        serieRow("SEQ1", "S2", "In progress", "LECTRA2", newer)));
        when(productionTableRepository.findZoneInfoByTableNoms(anyList()))
                .thenReturn(Arrays.asList(
                        tableRow("LECTRA1", "NEJMA", Zone.Category.STRICT),
                        tableRow("LECTRA2", "SPARTEL", Zone.Category.STRICT)));
        when(requestDataRepository.applyAutoZone("SEQ1", "SPARTEL")).thenReturn(1);

        Map<String, Object> r = service.runOnce();

        verify(requestDataRepository).applyAutoZone("SEQ1", "SPARTEL");
        assertEquals(1, r.get("corrected"));
    }
}
