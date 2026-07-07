package com.lear.MGCMS.repositories.CuttingPlan;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberId;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberInfo;

public interface CuttingPlanPartNumberInfoRepository extends JpaRepository<CuttingPlanPartNumberInfo, CuttingPlanPartNumberId>, JpaSpecificationExecutor<CuttingPlanPartNumberInfo>  {

//	@Query("SELECT t from CuttingPlanPartNumberInfo t where "
//			+ "partNumber in (:arr) "
//			+ "and not exists (SELECT t from CuttingPlanPartNumberInfo t2 where t2.cuttingPlan = t.cuttingPlan and t2.partNumber not in (:arr))"
//			+ "and exists (SELECT cp from CuttingPlanLight cp where cp.id = t.cuttingPlan and (cp.enabled = 1 "
//			+ " or ((cp.startDate is null or cp.startDate <= :currentTime) and (cp.endDate is null or cp.endDate >= :currentTime))"
//			+ "))")
//	List<CuttingPlanPartNumberInfo> findAllToWork(LocalDateTime currentTime, List<String> arr);
@Query("SELECT t FROM CuttingPlanPartNumberInfo t "
		+ "JOIN CuttingPlanLight cp ON cp.id = t.cuttingPlan "
		+ "WHERE cp.enabled = 1 "
		+ "AND t.partNumber IN (:arr) "
		+ "AND NOT EXISTS (SELECT t2 FROM CuttingPlanPartNumberInfo t2 WHERE t2.cuttingPlan = t.cuttingPlan AND t2.partNumber NOT IN (:arr)) "
		+ "AND (cp.startDate IS NULL OR cp.startDate <= :currentTime) "
		+ "AND (cp.endDate IS NULL OR cp.endDate >= :currentTime)")
List<CuttingPlanPartNumberInfo> findAllToWork(LocalDateTime currentTime, List<String> arr);


}
