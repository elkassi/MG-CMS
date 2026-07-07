package com.lear.MGCMS.repositories.CuttingPlan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;

public interface CuttingPlanLight2Repository extends JpaRepository<CuttingPlanLight2, Long>, JpaSpecificationExecutor<CuttingPlanLight2> {

	CuttingPlanLight2 findByCmsId(long parseLong);
	@Query("select cmsId from CuttingPlanLight2 cp where cp.id = :id")
	Long findCmsIdById(long id);

	@Query("from CuttingPlanLight2 cp where cp.cmsId = :cmsId and cp.enabled = true")
	CuttingPlanLight2 findByCmsIdAndEnable(Long cmsId);
}
