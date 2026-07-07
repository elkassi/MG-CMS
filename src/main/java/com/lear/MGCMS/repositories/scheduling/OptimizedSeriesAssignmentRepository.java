package com.lear.MGCMS.repositories.scheduling;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lear.MGCMS.domain.scheduling.OptimizedSeriesAssignment;

@Repository
public interface OptimizedSeriesAssignmentRepository extends JpaRepository<OptimizedSeriesAssignment, Long> {

    List<OptimizedSeriesAssignment> findByOptimizedPlanId(Long planId);

    List<OptimizedSeriesAssignment> findByOptimizedPlanPlanId(String planId);

    List<OptimizedSeriesAssignment> findBySequenceId(String sequenceId);

    List<OptimizedSeriesAssignment> findBySerieId(String serieId);

    List<OptimizedSeriesAssignment> findByMachineName(String machineName);

    @Query("SELECT a FROM OptimizedSeriesAssignment a WHERE a.optimizedPlan.id = :planId ORDER BY a.machineName, a.scheduledStart")
    List<OptimizedSeriesAssignment> findByPlanIdOrderedByMachineAndTime(@Param("planId") Long planId);

    @Query("SELECT a FROM OptimizedSeriesAssignment a WHERE a.optimizedPlan.id = :planId AND a.isLocked = true")
    List<OptimizedSeriesAssignment> findLockedByPlanId(@Param("planId") Long planId);

    @Query("SELECT a FROM OptimizedSeriesAssignment a WHERE a.sequenceId = :sequenceId AND a.optimizedPlan.isActive = true")
    List<OptimizedSeriesAssignment> findActiveBySequence(@Param("sequenceId") String sequenceId);

    void deleteByOptimizedPlanId(Long planId);
}
