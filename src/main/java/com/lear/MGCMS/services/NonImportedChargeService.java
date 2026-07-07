package com.lear.MGCMS.services;

import com.lear.cms.domain.OrderSchedule;
import com.lear.cms.repositories.OrderScheduleRepository;
import com.lear.cms.repositories.TimingModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.*;

/**
 * Computes "non-imported charge" — estimated cutting time for work orders
 * that exist in Order_Schedule (status = 'F') but have not yet been imported
 * into the CMS as CuttingRequests.
 *
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Query Order_Schedule where status = 'F' for the given date/shift.</li>
 *   <li>For each row, look up Timing_Model by item number (PartNumber_Demande).</li>
 *   <li>If not found, fallback to Timing_STD_Model (same DB, updated by quality team).</li>
 *   <li>If still not found: fallback = 1.0 min per piece × quantiteDemande.</li>
 * </ol>
 */
@Service
public class NonImportedChargeService {

    @Autowired
    private OrderScheduleRepository orderScheduleRepository;

    @Autowired
    private TimingModelRepository timingModelRepository;

    @Autowired
    private ApplicationContext applicationContext;

    /** Fallback cutting time per piece when no Timing_Model entry exists. */
    private static final double FALLBACK_MINUTES_PER_PIECE = 1.0;

    private static final String SOURCE_TIMING_MODEL = "Timing_Model";
    private static final String SOURCE_TIMING_STD_MODEL = "Timing_STD_Model";
    private static final String SOURCE_FALLBACK = "Fallback";

    /**
     * Compute total non-imported charge (minutes) for a date/shift.
     *
     * @return Map with "totalMinutes", "count", and "details" list
     */
    public Map<String, Object> computeCharge(LocalDate date, String shift) {
        List<OrderSchedule> orders = orderScheduleRepository.findByDateAndShift(date, shift);

        double totalMinutes = 0.0;
        int count = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (OrderSchedule os : orders) {
            if (!"F".equalsIgnoreCase(os.getStatusDemande())) continue;
            if (os.getQuantiteDemande() == null || os.getQuantiteDemande() <= 0) continue;

            String partNumber = os.getPartNumberDemande();
            int qty = os.getQuantiteDemande();

            TimeResolution resolution = resolveMinutesPerPiece(partNumber);
            double minutesPerPiece = resolution.minutesPerPiece;
            double estimatedMinutes = minutesPerPiece * qty;

            totalMinutes += estimatedMinutes;
            count++;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("idDemande", os.getIdDemande());
            detail.put("partNumber", partNumber);
            detail.put("description", os.getDescriptionPNDemande());
            detail.put("quantity", qty);
            detail.put("minutesPerPiece", minutesPerPiece);
            detail.put("estimatedMinutes", estimatedMinutes);
            detail.put("source", resolution.source);
            details.add(detail);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMinutes", totalMinutes);
        result.put("count", count);
        result.put("details", details);
        return result;
    }

    /**
     * Resolve cutting time per piece for a part number.
     * Looks up Timing_Model by itemNumber, then Timing_STD_Model, then falls back to 1.0 min/piece.
     */
    private TimeResolution resolveMinutesPerPiece(String partNumber) {
        if (partNumber == null || partNumber.isEmpty()) {
            return new TimeResolution(FALLBACK_MINUTES_PER_PIECE, SOURCE_FALLBACK);
        }

        // 1. Try Timing_Model first
        List<Double> times = timingModelRepository.findCuttingTimesByItemNumber(partNumber);
        if (times != null && !times.isEmpty()) {
            Double t = times.get(0);
            if (t != null && t > 0) {
                return new TimeResolution(t, SOURCE_TIMING_MODEL);
            }
        }

        // 2. Fallback to Timing_STD_Model (quality team updates this table)
        Double stdTime = findCuttingTimeInTimingSTDModel(partNumber);
        if (stdTime != null && stdTime > 0) {
            return new TimeResolution(stdTime, SOURCE_TIMING_STD_MODEL);
        }

        return new TimeResolution(FALLBACK_MINUTES_PER_PIECE, SOURCE_FALLBACK);
    }

    /**
     * Query Timing_STD_Model for the minimum positive Cutting_STD_Time by item number.
     */
    private Double findCuttingTimeInTimingSTDModel(String partNumber) {
        try {
            DataSource cmsDataSource = (DataSource) applicationContext.getBean("cmsDataSource");
            JdbcTemplate jdbcTemplate = new JdbcTemplate(cmsDataSource);
            String sql = "SELECT TOP 1 [Cutting_STD_Time] " +
                         "FROM [dbo].[Timing_STD_Model] " +
                         "WHERE [ItemNumber_STD_Time] = ? AND [Cutting_STD_Time] > 0 " +
                         "ORDER BY [Cutting_STD_Time] ASC";
            List<Double> results = jdbcTemplate.queryForList(sql, Double.class, partNumber);
            if (!results.isEmpty()) {
                return results.get(0);
            }
        } catch (Exception e) {
            // Log but don't fail — fallback to 1.0 min/piece
            System.err.println("Error querying Timing_STD_Model for partNumber=" + partNumber + ": " + e.getMessage());
        }
        return null;
    }

    private static class TimeResolution {
        final double minutesPerPiece;
        final String source;

        TimeResolution(double minutesPerPiece, String source) {
            this.minutesPerPiece = minutesPerPiece;
            this.source = source;
        }
    }
}
