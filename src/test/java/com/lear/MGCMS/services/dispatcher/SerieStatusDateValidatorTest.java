package com.lear.MGCMS.services.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestPartNumberDataRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SerieStatusDateValidatorTest {

    @Mock private CuttingRequestSerieDataRepository serieDataRepository;
    @Mock private CuttingRequestPartNumberDataRepository partNumberDataRepository;
    @Mock private CuttingRequestRepository cuttingRequestRepository;
    @Mock private CuttingTimeCalculator cuttingTimeCalculator;
    @Mock private DataSource cmsDataSource;

    @InjectMocks private SerieStatusDateValidator validator;

    @Test
    void normalizeCoupePassThroughClosesPreviousStartedSerie() {
        LocalDateTime nextStart = LocalDateTime.of(2026, 5, 14, 8, 45);
        when(serieDataRepository.findCoupePassThroughClosures())
                .thenReturn(java.util.Collections.singletonList(new Object[] {"S1", nextStart}));
        when(serieDataRepository.closeCoupePassThrough("S1", nextStart)).thenReturn(1);

        int patched = validator.normalizeCoupePassThrough();

        assertEquals(1, patched);
        verify(serieDataRepository).closeCoupePassThrough("S1", nextStart);
    }

    @Test
    void normalizeMatelassagePassThroughClosesPreviousStartedSerie() {
        LocalDateTime nextStart = LocalDateTime.of(2026, 5, 14, 9, 30);
        when(serieDataRepository.findMatelassagePassThroughClosures())
                .thenReturn(java.util.Collections.singletonList(new Object[] {"S1", nextStart}));
        when(serieDataRepository.closeMatelassagePassThrough("S1", nextStart)).thenReturn(1);

        int patched = validator.normalizeMatelassagePassThrough();

        assertEquals(1, patched);
        verify(serieDataRepository).closeMatelassagePassThrough("S1", nextStart);
    }

    @Test
    void autoCompleteSequencesFromSeriesMarksParentCompleted() {
        CuttingRequest request = new CuttingRequest();
        request.setSequence("SEQ1");
        request.setSequenceStatus("ACTIVE");
        when(cuttingRequestRepository.findNonCompletedSequences()).thenReturn(List.of("SEQ1"));
        when(serieDataRepository.findSequencesWhereAllCoupeComplete(List.of("SEQ1"))).thenReturn(List.of("SEQ1"));
        when(cuttingRequestRepository.findById("SEQ1")).thenReturn(Optional.of(request));

        int patched = validator.autoCompleteSequencesFromSeries();

        assertEquals(1, patched);
        assertEquals("COMPLETED", request.getSequenceStatus());
        verify(cuttingRequestRepository).saveAll(any(Iterable.class));
    }
}
