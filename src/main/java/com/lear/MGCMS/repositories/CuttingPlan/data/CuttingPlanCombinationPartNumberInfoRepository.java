package com.lear.MGCMS.repositories.CuttingPlan.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.controller.CuttingPlan.data.CuttingPlanCombinationPartNumberInfoId;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacementInfo;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacementInfoId;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanCombinationPartNumberInfo;

public interface CuttingPlanCombinationPartNumberInfoRepository extends JpaRepository<CuttingPlanCombinationPartNumberInfo, CuttingPlanCombinationPartNumberInfoId>, JpaSpecificationExecutor<CuttingPlanMaterialPlacementInfo> {



}
