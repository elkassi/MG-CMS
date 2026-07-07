package com.lear.MGCMS.services.logistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.CuttingRequest.SequenceStatus;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository;
import com.lear.cms.repositories.SuiviPlanningRepository;

class SuiviPlanningStatusSyncServiceTest {

    @Mock private SuiviPlanningRepository suiviPlanningRepository;
    @Mock private CuttingRequestDataRepository requestDataRepository;

    @InjectMocks private SuiviPlanningStatusSyncService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static Object[] row(String sequence, String statu) {
        return new Object[] { sequence, statu };
    }

    @Test
    void mapsEachStatuToItsSequenceStatusTarget() {
        when(suiviPlanningRepository.findAllStatu()).thenReturn(Arrays.asList(
                row("S_IMP", "Non demarre"),
                row("S_REL", "Released"),
                row("S_STA", "En cours"),
                row("S_COM", "Complet")));
        when(requestDataRepository.reconcileSequenceStatus(anyString(), anyList())).thenReturn(1);

        int changed = service.reconcile();

        assertEquals(4, changed);
        verify(requestDataRepository).reconcileSequenceStatus(eq(SequenceStatus.IMPORTED), eq(List.of("S_IMP")));
        verify(requestDataRepository).reconcileSequenceStatus(eq(SequenceStatus.RELEASED), eq(List.of("S_REL")));
        verify(requestDataRepository).reconcileSequenceStatus(eq(SequenceStatus.STARTED), eq(List.of("S_STA")));
        verify(requestDataRepository).reconcileSequenceStatus(eq(SequenceStatus.COMPLETED), eq(List.of("S_COM")));
    }

    @Test
    void unmappedOrNullStatuIsSkipped() {
        when(suiviPlanningRepository.findAllStatu()).thenReturn(Arrays.asList(
                row("S1", "Bloque"),
                row("S2", null)));

        int changed = service.reconcile();

        assertEquals(0, changed);
        verify(requestDataRepository, never()).reconcileSequenceStatus(anyString(), anyList());
    }

    @Test
    void dedupesToFurthestAlongStatusPerSequence() {
        // One sequence with mixed rows — the most-advanced status wins.
        when(suiviPlanningRepository.findAllStatu()).thenReturn(Arrays.asList(
                row("S1", "Non demarre"),
                row("S1", "En cours"),
                row("S1", "Non demarre")));
        when(requestDataRepository.reconcileSequenceStatus(anyString(), anyList())).thenReturn(1);

        int changed = service.reconcile();

        assertEquals(1, changed);
        ArgumentCaptor<String> target = ArgumentCaptor.forClass(String.class);
        verify(requestDataRepository).reconcileSequenceStatus(target.capture(), eq(List.of("S1")));
        assertEquals(SequenceStatus.STARTED, target.getValue());
    }

    @Test
    void emptySourceDoesNothing() {
        when(suiviPlanningRepository.findAllStatu()).thenReturn(new ArrayList<>());

        int changed = service.reconcile();

        assertEquals(0, changed);
        verify(requestDataRepository, never()).reconcileSequenceStatus(anyString(), anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void chunksLargeBatchesUnderTheParameterCap() {
        // 1500 same-status sequences must split into two bulk updates (1000 + 500)
        // so the IN (...) clause never exceeds SQL Server's 2100-parameter cap.
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            rows.add(row("S" + i, "Non demarre"));
        }
        when(suiviPlanningRepository.findAllStatu()).thenReturn(rows);

        service.reconcile();

        ArgumentCaptor<List<String>> chunks = ArgumentCaptor.forClass(List.class);
        verify(requestDataRepository, times(2)).reconcileSequenceStatus(eq(SequenceStatus.IMPORTED), chunks.capture());
        assertEquals(1000, chunks.getAllValues().get(0).size());
        assertEquals(500, chunks.getAllValues().get(1).size());
    }

    @Test
    void repositoryReconcileQueryIncludesNullStatusesButKeepsInternalExceptions() throws Exception {
        Method method = CuttingRequestDataRepository.class
                .getMethod("reconcileSequenceStatus", String.class, List.class);
        String query = method.getAnnotation(Query.class).value();

        assertTrue(query.contains("c.sequenceStatus IS NULL"));
        assertTrue(query.contains("c.sequenceStatus <> :target"));
        assertTrue(query.contains("MATERIAL_MISSING"));
        assertTrue(query.contains("INCOMPLETE"));
    }
}
