package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.lear.MGCMS.services.OrdonnancementService;

/**
 * Legacy ordering: dueDate → dueShift → serie.
 *
 * <p>In practice the DTO only carries {@code planningDate} (which maps to
 * dueDate) and does not expose dueShift, so the legacy sort falls back to
 * planningDate → sequence → serie.  For ready-to-cut series the tie-breaker
 * is {@code dateFinMatelassage} (oldest first) so a serie that finished
 * spreading earlier gets the cutting table sooner.</p>
 */
@Component
public class LegacySeriesOrderingStrategy implements SeriesOrderingStrategy {

    @Override
    public void sortReadyToCut(List<OrdonnancementService.SerieDTO> series, Context ctx) {
        series.sort(Comparator
                .comparing((OrdonnancementService.SerieDTO s) ->
                        s.dateFinMatelassage != null ? s.dateFinMatelassage : LocalDateTime.MAX)
                .thenComparing(s -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparing(s -> s.planningDate != null ? s.planningDate : LocalDate.MAX)
                .thenComparing(s -> s.sequence != null ? s.sequence : "")
                .thenComparing(s -> s.serie != null ? s.serie : ""));
    }

    @Override
    public void sortWaiting(List<OrdonnancementService.SerieDTO> series, Context ctx) {
        series.sort(Comparator
                .comparing((OrdonnancementService.SerieDTO s) -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparing(s -> s.planningDate != null ? s.planningDate : LocalDate.MAX)
                .thenComparing(s -> s.sequence != null ? s.sequence : "")
                .thenComparing(s -> s.serie != null ? s.serie : ""));
    }
}
