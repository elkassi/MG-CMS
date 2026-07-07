package com.lear.MGCMS.controller.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.lear.MGCMS.services.dispatcher.TableFeedDto;
import com.lear.MGCMS.services.dispatcher.TableFeedRankingService;
import com.lear.MGCMS.services.scheduling.ShiftClock;

class PublicRecommendationControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void nextSeries_usesTableFeedRankingContract() {
        TableFeedRankingService rankingService = mock(TableFeedRankingService.class);
        ShiftClock shiftClock = mock(ShiftClock.class);
        LocalDate date = LocalDate.of(2026, 6, 1);

        when(shiftClock.currentSlot()).thenReturn(new ShiftClock.ShiftSlot(date, 2));
        when(rankingService.recommendForMachine(eq(date), eq(2), eq("AA2"), eq(3)))
                .thenReturn(Arrays.asList(new TableFeedDto.CandidateDto(
                        "S1", "SEQ1", "FAB-A", "Lectra", "Z1",
                        "Waiting", "Waiting", "RELEASED", false,
                        true, true, false, true,
                        12.0, 10, 120.0, date, 25.0, 190.0,
                        Arrays.asList("Série Waiting / Waiting", "Matière sur rack dans la zone"))));

        PublicRecommendationController controller = new PublicRecommendationController();
        ReflectionTestUtils.setField(controller, "tableFeedRankingService", rankingService);
        ReflectionTestUtils.setField(controller, "shiftClock", shiftClock);

        ResponseEntity<List<Map<String, Object>>> response = controller.getNextSeries("AA2", 3);

        assertEquals(200, response.getStatusCodeValue());
        List<Map<String, Object>> body = response.getBody();
        assertEquals(1, body.size());
        Map<String, Object> item = body.get(0);
        assertEquals("S1", item.get("serie"));
        assertEquals("SEQ1", item.get("sequence"));
        assertEquals("AA2", item.get("machine"));
        assertEquals("MATELASSAGE", item.get("phase"));
        assertEquals("Waiting", item.get("statusCoupe"));
        assertEquals("Waiting", item.get("statusMatelassage"));
        assertEquals("RELEASED", item.get("sequenceStatus"));
        assertTrue(((List<String>) item.get("reasons")).contains("Série Waiting / Waiting"));
        verify(rankingService).recommendForMachine(date, 2, "AA2", 3);
    }
}
