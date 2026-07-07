package com.lear.MGCMS.repositories.scheduling;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.scheduling.MaterialLogistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaterialLogisticsRepository extends JpaRepository<MaterialLogistics, Long>, JpaSpecificationExecutor<MaterialLogistics> {

    List<MaterialLogistics> findByZone(Zone zone);
    
    List<MaterialLogistics> findByPartNumberMaterial(String partNumberMaterial);
    
    List<MaterialLogistics> findByStatus(String status);
    
    List<MaterialLogistics> findByPriority(String priority);
    
    @Query("SELECT ml FROM MaterialLogistics ml WHERE ml.status = 'PENDING' ORDER BY ml.neededBy ASC")
    List<MaterialLogistics> findPendingOrderByNeededBy();
    
    @Query("SELECT ml FROM MaterialLogistics ml WHERE ml.zone = :zone AND ml.status = 'PENDING' ORDER BY ml.neededBy ASC")
    List<MaterialLogistics> findPendingByZoneOrderByNeededBy(Zone zone);
    
    @Query("SELECT ml FROM MaterialLogistics ml WHERE ml.status = 'PENDING' AND ml.neededBy <= :deadline ORDER BY ml.priority DESC, ml.neededBy ASC")
    List<MaterialLogistics> findUrgentPending(LocalDateTime deadline);
    
    @Query("SELECT ml FROM MaterialLogistics ml WHERE ml.zone.nom = :zoneName AND ml.status IN ('PENDING', 'IN_TRANSIT') ORDER BY ml.neededBy ASC")
    List<MaterialLogistics> findActiveByZone(String zoneName);
    
    @Query("SELECT ml FROM MaterialLogistics ml WHERE ml.createdAt >= :startTime AND ml.createdAt <= :endTime")
    List<MaterialLogistics> findCreatedInRange(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT DISTINCT ml.partNumberMaterial FROM MaterialLogistics ml WHERE ml.zone = :zone AND ml.status = 'PENDING'")
    List<String> findPendingMaterialsByZone(Zone zone);
}

