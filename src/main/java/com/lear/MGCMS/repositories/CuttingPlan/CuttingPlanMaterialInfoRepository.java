package com.lear.MGCMS.repositories.CuttingPlan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialId;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialInfo;

public interface CuttingPlanMaterialInfoRepository extends JpaRepository<CuttingPlanMaterialInfo, CuttingPlanMaterialId>, JpaSpecificationExecutor<CuttingPlanMaterialInfo> {

	@Query("select cpm from CuttingPlanMaterialInfo cpm where cpm.cuttingPlan = :cuttingPlan and cpm.partNumberMaterial = :partNumberMaterial")
	CuttingPlanMaterialInfo findByCuttingPlanAndPartNumberMaterial(Long cuttingPlan, String partNumberMaterial);

}
