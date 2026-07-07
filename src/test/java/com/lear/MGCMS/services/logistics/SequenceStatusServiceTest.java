package com.lear.MGCMS.services.logistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.lear.MGCMS.domain.CuttingRequest.ReleaseZoneSource;
import com.lear.MGCMS.domain.CuttingRequest.SequenceStatus;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.cms.repositories.SuiviPlanningRepository;

class SequenceStatusServiceTest {

    @Mock private CuttingRequestDataRepository requestDataRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private SuiviPlanningRepository suiviPlanningRepository;

    @InjectMocks private SequenceStatusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // @Value default is true in the Spring context; mirror it here.
        ReflectionTestUtils.setField(service, "rectifyEnabled", true);
    }

    private CuttingRequestData row(String status) {
        CuttingRequestData d = new CuttingRequestData();
        d.setSequence("SEQ1");
        d.setSequenceStatus(status);
        return d;
    }

    @Test
    void released_fromImported_persistsStatusAndZone() {
        CuttingRequestData d = row(SequenceStatus.IMPORTED);
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.transition("SEQ1", SequenceStatus.RELEASED, "Z1");

        assertTrue((Boolean) r.get("success"));
        assertEquals(SequenceStatus.RELEASED, d.getSequenceStatus());
        assertEquals("Z1", d.getReleaseZone());
        assertEquals("Z1", r.get("releaseZone"));
        assertEquals(ReleaseZoneSource.LOGISTICS, d.getReleaseZoneSource());
        verify(requestDataRepository).save(d);
    }

    @Test
    void released_withoutZone_isRejected() {
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(row(SequenceStatus.IMPORTED));

        Map<String, Object> r = service.transition("SEQ1", SequenceStatus.RELEASED, null);

        assertFalse((Boolean) r.get("success"));
        verify(requestDataRepository, never()).save(any());
    }

    @Test
    void started_fromReleased_isAllowed() {
        CuttingRequestData d = row(SequenceStatus.RELEASED);
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.transition("SEQ1", SequenceStatus.STARTED, null);

        assertTrue((Boolean) r.get("success"));
        assertEquals(SequenceStatus.STARTED, d.getSequenceStatus());
        assertNull(d.getReleaseZone());
    }

    @Test
    void completed_fromImported_isRejected() {
        // IMPORTED is pre-release; it cannot jump straight to COMPLETED.
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(row(SequenceStatus.IMPORTED));

        Map<String, Object> r = service.transition("SEQ1", SequenceStatus.COMPLETED, null);

        assertFalse((Boolean) r.get("success"));
        verify(requestDataRepository, never()).save(any());
    }

    @Test
    void legacyNullStatus_canTransitionToMaterialMissing() {
        CuttingRequestData d = row(null);
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.transition("SEQ1", SequenceStatus.MATERIAL_MISSING, null);

        assertTrue((Boolean) r.get("success"));
        assertEquals(SequenceStatus.MATERIAL_MISSING, d.getSequenceStatus());
    }

    @Test
    void unknownStatus_isRejected() {
        Map<String, Object> r = service.transition("SEQ1", "BOGUS", null);

        assertFalse((Boolean) r.get("success"));
        verify(requestDataRepository, never()).findBySequence(any());
    }

    @Test
    void missingSequence_isRejected() {
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(null);

        Map<String, Object> r = service.transition("SEQ1", SequenceStatus.STARTED, null);

        assertFalse((Boolean) r.get("success"));
        verify(requestDataRepository, never()).save(any());
    }

    /* ---------- rectify (chef override) ---------- */

    @Test
    void rectify_bypassesStateMachine_andWritesThroughToSuiviplanning() {
        // COMPLETED -> STARTED is forbidden for transition(); rectify allows it.
        CuttingRequestData d = row(SequenceStatus.COMPLETED);
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.rectify("SEQ1", SequenceStatus.STARTED, null, "chef1");

        assertTrue((Boolean) r.get("success"));
        assertEquals(SequenceStatus.STARTED, d.getSequenceStatus());
        verify(requestDataRepository).save(d);
        verify(suiviPlanningRepository).updateStatuBySequence("SEQ1", "En cours");
    }

    @Test
    void rectify_completed_writesCompletToSuiviplanning() {
        CuttingRequestData d = row(SequenceStatus.STARTED);
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.rectify("SEQ1", SequenceStatus.COMPLETED, null, "chef1");

        assertTrue((Boolean) r.get("success"));
        verify(suiviPlanningRepository).updateStatuBySequence("SEQ1", "Complet");
    }

    @Test
    void rectify_materialMissing_doesNotTouchSuiviplanning() {
        // suiviplanning has no MATERIAL_MISSING; the sync already preserves it.
        CuttingRequestData d = row(SequenceStatus.STARTED);
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.rectify("SEQ1", SequenceStatus.MATERIAL_MISSING, null, "chef1");

        assertTrue((Boolean) r.get("success"));
        verify(suiviPlanningRepository, never()).updateStatuBySequence(anyString(), anyString());
    }

    @Test
    void rectify_whenDisabled_isRejected() {
        ReflectionTestUtils.setField(service, "rectifyEnabled", false);

        Map<String, Object> r = service.rectify("SEQ1", SequenceStatus.COMPLETED, null, "chef1");

        assertFalse((Boolean) r.get("success"));
        verify(requestDataRepository, never()).save(any());
    }

    @Test
    void rectify_toReleased_requiresAZoneOnRecordOrInRequest() {
        CuttingRequestData d = row(SequenceStatus.STARTED); // no releaseZone
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.rectify("SEQ1", SequenceStatus.RELEASED, null, "chef1");

        assertFalse((Boolean) r.get("success"));
        verify(requestDataRepository, never()).save(any());
    }

    @Test
    void rectify_toReleased_keepsExistingZone() {
        CuttingRequestData d = row(SequenceStatus.STARTED);
        d.setReleaseZone("Z1");
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.rectify("SEQ1", SequenceStatus.RELEASED, null, "chef1");

        assertTrue((Boolean) r.get("success"));
        assertEquals("Z1", d.getReleaseZone());
        verify(suiviPlanningRepository).updateStatuBySequence("SEQ1", "Released");
    }

    @Test
    void rectify_toImported_clearsZone_andWritesNonDemarre() {
        CuttingRequestData d = row(SequenceStatus.RELEASED);
        d.setReleaseZone("Z1");
        d.setReleaseZoneSource(ReleaseZoneSource.LOGISTICS);
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);

        Map<String, Object> r = service.rectify("SEQ1", SequenceStatus.IMPORTED, null, "chef1");

        assertTrue((Boolean) r.get("success"));
        assertNull(d.getReleaseZone());
        assertNull(d.getReleaseZoneSource());
        assertEquals(SequenceStatus.IMPORTED, d.getSequenceStatus());
        verify(suiviPlanningRepository).updateStatuBySequence("SEQ1", "Non demarre");
    }

    @Test
    void rectify_zoneOnly_movesTheSequence() {
        CuttingRequestData d = row(SequenceStatus.COMPLETED);
        d.setReleaseZone("Z1");
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);
        when(zoneRepository.findById("Z2")).thenReturn(Optional.of(new Zone()));

        Map<String, Object> r = service.rectify("SEQ1", null, "Z2", "chef1");

        assertTrue((Boolean) r.get("success"));
        assertEquals("Z2", d.getReleaseZone());
        assertEquals(ReleaseZoneSource.CHEF, d.getReleaseZoneSource());
        assertEquals(SequenceStatus.COMPLETED, d.getSequenceStatus());
        verify(suiviPlanningRepository, never()).updateStatuBySequence(anyString(), anyString());
    }

    @Test
    void rectifyBulk_reportsPerSequenceOutcome() {
        CuttingRequestData d = row(SequenceStatus.STARTED);
        when(requestDataRepository.findBySequence("SEQ1")).thenReturn(d);
        when(requestDataRepository.findBySequence("SEQ2")).thenReturn(null);

        Map<String, Object> r = service.rectifyBulk(
                Arrays.asList("SEQ1", "SEQ2"), SequenceStatus.COMPLETED, "chef1");

        assertTrue((Boolean) r.get("success"));
        assertEquals(1, r.get("updated"));
        assertEquals(1, r.get("failed"));
        verify(suiviPlanningRepository).updateStatuBySequence("SEQ1", "Complet");
    }
}
