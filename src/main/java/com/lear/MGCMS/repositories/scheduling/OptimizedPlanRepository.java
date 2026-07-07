package com.lear.MGCMS.repositories.scheduling;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lear.MGCMS.domain.scheduling.OptimizedPlan;

@Repository
public interface OptimizedPlanRepository extends JpaRepository<OptimizedPlan, Long> {

    Optional<OptimizedPlan> findByPlanId(String planId);

    List<OptimizedPlan> findByZoneName(String zoneName);

    List<OptimizedPlan> findByZoneNameAndIsActiveTrue(String zoneName);

    List<OptimizedPlan> findByStatus(String status);

    List<OptimizedPlan> findByCreatedByMatriculeOrderByCreatedAtDesc(String matricule);

    @Query("SELECT p FROM OptimizedPlan p WHERE p.zoneName = :zoneName AND p.isActive = true ORDER BY p.updatedAt DESC")
    Optional<OptimizedPlan> findActiveByZone(@Param("zoneName") String zoneName);

    @Query("SELECT p FROM OptimizedPlan p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<OptimizedPlan> findRecentPlans(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM OptimizedPlan p WHERE p.status = 'RUNNING'")
    List<OptimizedPlan> findRunningOptimizations();

    @Query("SELECT p FROM OptimizedPlan p WHERE p.zoneName IN :zoneNames AND p.isActive = true")
    List<OptimizedPlan> findActiveByZones(@Param("zoneNames") List<String> zoneNames);

    @Query("SELECT DISTINCT p.zoneName FROM OptimizedPlan p WHERE p.isActive = true")
    List<String> findZonesWithActivePlans();

    void deleteByPlanId(String planId);
}
