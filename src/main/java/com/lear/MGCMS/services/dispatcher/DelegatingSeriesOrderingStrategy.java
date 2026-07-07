package com.lear.MGCMS.services.dispatcher;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.lear.MGCMS.services.OrdonnancementService;

/**
 * Delegates to either the legacy or the V2 strategy based on
 * {@code cms.dispatcher.ordering}.
 *
 * <p>Marked {@code @Primary} so callers that autowire
 * {@link SeriesOrderingStrategy} always get this bean and never
 * see an ambiguous-dependency error.</p>
 */
@Component
@Primary
public class DelegatingSeriesOrderingStrategy implements SeriesOrderingStrategy {

    @Autowired
    LegacySeriesOrderingStrategy legacy;

    @Autowired
    BoxDurOptimizedOrderingStrategy v2;

    @Autowired
    OrderingProperties orderingProperties;

    @Override
    public void sortReadyToCut(List<OrdonnancementService.SerieDTO> series, Context ctx) {
        delegate().sortReadyToCut(series, ctx);
    }

    @Override
    public void sortWaiting(List<OrdonnancementService.SerieDTO> series, Context ctx) {
        delegate().sortWaiting(series, ctx);
    }

    private SeriesOrderingStrategy delegate() {
        return orderingProperties.isLegacy() ? legacy : v2;
    }
}
