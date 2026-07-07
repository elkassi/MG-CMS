package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.dispatcher.UnassignableSerie;
import com.lear.MGCMS.domain.dispatcher.UnassignableSerie.ReasonCode;
import com.lear.MGCMS.repositories.dispatcher.UnassignableSerieRepository;

/**
 * Unit tests for {@link SchedulableSerieFilter}: verifies both the
 * partitioning contract and the audit-write side effect.
 */
class SchedulableSerieFilterTest {

    @Mock private SerieZoneResolver serieZoneResolver;
    @Mock private UnassignableSerieRepository unassignableRepository;

    @InjectMocks private SchedulableSerieFilter filter;

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 24);

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    private static Zone zone(String nom, Zone.Category cat) {
        Zone z = new Zone();
        z.setNom(nom);
        z.setCategory(cat);
        z.setActive(true);
        return z;
    }

    private static SerieDispatchInfo serie(String id, String machine) {
        return new SerieDispatchInfo(id, "1", machine, 5.0, 10, "P1");
    }

    @Test
    @DisplayName("empty input → empty output, no writes")
    void emptyInput() {
        SchedulableSerieFilter.FilterResult r = filter.filter(Collections.emptyList(), TODAY, 1);
        assertTrue(r.getSchedulable().isEmpty());
        assertTrue(r.getRejected().isEmpty());
        verify(unassignableRepository, never()).save(any());
    }

    @Test
    @DisplayName("mixed batch — schedulable and rejected partitioned, audit rows written")
    void mixedBatch() {
        SerieDispatchInfo ok1 = serie("S1", "Lectra");
        SerieDispatchInfo ok2 = serie("S2", "Lectra");
        SerieDispatchInfo ko1 = serie("S3", "LASER-DXF");
        SerieDispatchInfo ko2 = serie("S4", "FOO");

        Zone fa = zone("FirstArticle", Zone.Category.STRICT);

        when(serieZoneResolver.resolve(ok1, TODAY, 1))
                .thenReturn(SerieZoneResolver.Resolution.accepted(fa));
        when(serieZoneResolver.resolve(ok2, TODAY, 1))
                .thenReturn(SerieZoneResolver.Resolution.accepted(fa));
        when(serieZoneResolver.resolve(ko1, TODAY, 1))
                .thenReturn(SerieZoneResolver.Resolution.rejected(
                        SerieZoneResolver.FailureReason.ALL_ZONES_CLOSED_FOR_SHIFT));
        when(serieZoneResolver.resolve(ko2, TODAY, 1))
                .thenReturn(SerieZoneResolver.Resolution.rejected(
                        SerieZoneResolver.FailureReason.NO_ZONE_ACCEPTING_TYPE));

        SchedulableSerieFilter.FilterResult r =
                filter.filter(Arrays.asList(ok1, ok2, ko1, ko2), TODAY, 1);

        assertEquals(2, r.getSchedulable().size());
        assertEquals(2, r.getRejected().size());

        assertEquals(fa, r.getSchedulable().get(ok1));
        assertEquals(fa, r.getSchedulable().get(ok2));

        ArgumentCaptor<UnassignableSerie> cap = ArgumentCaptor.forClass(UnassignableSerie.class);
        verify(unassignableRepository, times(2)).save(cap.capture());
        List<UnassignableSerie> saved = cap.getAllValues();

        assertEquals("S3", saved.get(0).getSerieId());
        assertEquals(ReasonCode.ALL_ZONES_CLOSED_FOR_SHIFT, saved.get(0).getReasonCode());
        assertTrue(saved.get(0).getReasonDetail().contains("LASER-DXF"));

        assertEquals("S4", saved.get(1).getSerieId());
        assertEquals(ReasonCode.NO_ZONE_ACCEPTING_TYPE, saved.get(1).getReasonCode());
    }

    @Test
    @DisplayName("acceptance scenario from guide: LASER-DXF rejected → ALL_ZONES_CLOSED_FOR_SHIFT")
    void acceptanceScenario() {
        SerieDispatchInfo dxf = serie("LASR-1", "LASER-DXF");
        when(serieZoneResolver.resolve(dxf, TODAY, 1))
                .thenReturn(SerieZoneResolver.Resolution.rejected(
                        SerieZoneResolver.FailureReason.ALL_ZONES_CLOSED_FOR_SHIFT));

        SchedulableSerieFilter.FilterResult r =
                filter.filter(Collections.singletonList(dxf), TODAY, 1);

        assertEquals(1, r.getRejected().size());
        assertEquals(ReasonCode.ALL_ZONES_CLOSED_FOR_SHIFT,
                     r.getRejected().get(0).getReasonCode());
        verify(unassignableRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("null entries in input list are skipped")
    void nullEntriesSkipped() {
        SchedulableSerieFilter.FilterResult r =
                filter.filter(Arrays.asList((SerieDispatchInfo) null), TODAY, 1);
        assertTrue(r.getSchedulable().isEmpty());
        assertTrue(r.getRejected().isEmpty());
        verify(unassignableRepository, never()).save(any());
    }
}
