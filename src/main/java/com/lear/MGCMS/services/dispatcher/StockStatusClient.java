package com.lear.MGCMS.services.dispatcher;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.StockStatusReport;
import com.lear.MGCMS.services.StockStatusReportService;

/**
 * Adapter for the central-stock check used by {@link MaterialAvailabilityChecker}.
 *
 * <p>Originally an HTTP self-call (Spring app calling its own REST endpoint
 * on a hardcoded port without JWT) — every call failed silently and every
 * fabric was treated as MISSING. Replaced with a direct service injection
 * so the engine's hot path stays in-process.</p>
 */
@Service
public class StockStatusClient {

    @Autowired
    private StockStatusReportService stockStatusReportService;

    /** @return the number of {@link StockStatusReport} rows for these refTissus; {@code 0} on null/empty. */
    public int getCurrentStockCount(List<String> refTissus) {
        if (refTissus == null || refTissus.isEmpty()) return 0;
        try {
            List<StockStatusReport> rows = stockStatusReportService.getCurrentStock(refTissus);
            return rows == null ? 0 : rows.size();
        } catch (Exception e) {
            return 0;
        }
    }
}
