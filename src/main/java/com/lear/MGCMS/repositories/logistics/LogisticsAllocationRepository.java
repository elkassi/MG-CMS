package com.lear.MGCMS.repositories.logistics;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.logistics.LogisticsAllocation;
import com.lear.MGCMS.domain.logistics.LogisticsAllocation.Status;

/**
 * Plain CRUD over the {@code logistics_allocation} ledger (Flyway V16_02).
 *
 * <p>{@link #sumMetersByMaterialAndZone} is the deduction that downstream code
 * subtracts from rack availability: per {@code (refTissus, targetZone)}, the
 * total meters reserved by rows whose status is still in the active set.</p>
 */
public interface LogisticsAllocationRepository extends JpaRepository<LogisticsAllocation, Long> {

    List<LogisticsAllocation> findByStatusIn(List<Status> statuses);

    List<LogisticsAllocation> findBySerialId(String serialId);

    List<LogisticsAllocation> findBySequence(String sequence);

    /**
     * Sum of {@code allocatedMeters} grouped by {@code (refTissus, targetZone)}
     * for the given active-status set. Each row is
     * {@code [refTissus, targetZone, sum(allocatedMeters)]}.
     */
    @Query("SELECT a.refTissus, a.targetZone, SUM(a.allocatedMeters) "
            + "FROM LogisticsAllocation a "
            + "WHERE a.status IN :statuses "
            + "GROUP BY a.refTissus, a.targetZone")
    List<Object[]> sumMetersByMaterialAndZone(@Param("statuses") List<Status> statuses);
}
