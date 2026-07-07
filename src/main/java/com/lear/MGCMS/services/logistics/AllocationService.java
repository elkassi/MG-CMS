package com.lear.MGCMS.services.logistics;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.logistics.LogisticsAllocation;
import com.lear.MGCMS.domain.logistics.LogisticsAllocation.Status;
import com.lear.MGCMS.repositories.logistics.LogisticsAllocationRepository;

/**
 * Write side of the logistics allocation / reservation ledger.
 *
 * <p>The keystone that makes rack stock TRUE: reservations are recorded as
 * {@link Status#ADVISED} rows, promoted to {@link Status#RELEASED} when a
 * picklist is released, then {@link Status#CONSUMED} when the roll is cut.
 * {@link #reservedMetersByMaterialZone()} is the deduction other code
 * subtracts from rack availability so two zones are never told to use the same
 * roll. Soft/advisory — it records intent, it does not lock physical stock.</p>
 */
@Service
public class AllocationService {

    /** Statuses that still hold stock against rack availability. */
    private static final List<Status> ACTIVE_STATUSES =
            Arrays.asList(Status.ADVISED, Status.RELEASED);

    @Autowired
    private LogisticsAllocationRepository allocationRepository;

    /** Persist a batch of reservations as {@link Status#ADVISED}. */
    @Transactional
    public List<LogisticsAllocation> reserve(List<LogisticsAllocation> allocations) {
        if (allocations == null || allocations.isEmpty()) return Collections.emptyList();
        for (LogisticsAllocation a : allocations) {
            if (a.getStatus() == null) a.setStatus(Status.ADVISED);
        }
        return allocationRepository.saveAll(allocations);
    }

    /** Mark every active reservation of a roll within a serie as CONSUMED. */
    @Transactional
    public int markConsumed(String serialId, String serie) {
        return transition(allocationRepository.findBySerialId(serialId), serie, Status.CONSUMED);
    }

    /** Mark every active reservation of a roll within a serie as RETURNED. */
    @Transactional
    public int markReturned(String serialId, String serie) {
        return transition(allocationRepository.findBySerialId(serialId), serie, Status.RETURNED);
    }

    /** Cancel every active reservation of a sequence (e.g. picklist scrapped before release). */
    @Transactional
    public int cancel(String sequence) {
        return transition(allocationRepository.findBySequence(sequence), null, Status.CANCELLED);
    }

    /**
     * Reserved meters per material/zone, keyed {@code "MATERIAL|ZONE"}, summed
     * over {@code (ADVISED, RELEASED)}. This is the deduction other code
     * subtracts from rack availability.
     */
    @Transactional(readOnly = true)
    public Map<String, Double> reservedMetersByMaterialZone() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Object[] row : allocationRepository.sumMetersByMaterialAndZone(ACTIVE_STATUSES)) {
            if (row == null || row.length < 3) continue;
            String material = row[0] != null ? String.valueOf(row[0]) : null;
            String zone = row[1] != null ? String.valueOf(row[1]) : null;
            double meters = row[2] instanceof Number ? ((Number) row[2]).doubleValue() : 0.0;
            if (material == null || zone == null) continue;
            out.merge(material + "|" + zone, meters, Double::sum);
        }
        return out;
    }

    /**
     * Move the active ({@code ADVISED} / {@code RELEASED}) rows of {@code rows}
     * to {@code target}. When {@code serie} is non-null only rows of that serie
     * are moved.
     */
    private int transition(List<LogisticsAllocation> rows, String serie, Status target) {
        if (rows == null || rows.isEmpty()) return 0;
        int updated = 0;
        LocalDateTime now = LocalDateTime.now();
        for (LogisticsAllocation a : rows) {
            if (serie != null && !serie.equals(a.getSerie())) continue;
            if (a.getStatus() == Status.ADVISED || a.getStatus() == Status.RELEASED) {
                a.setStatus(target);
                a.setUpdatedAt(now);
                updated++;
            }
        }
        return updated;
    }
}
