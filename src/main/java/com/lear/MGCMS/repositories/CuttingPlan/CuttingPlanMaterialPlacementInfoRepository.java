package com.lear.MGCMS.repositories.CuttingPlan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacementInfo;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacementInfoId;

public interface CuttingPlanMaterialPlacementInfoRepository extends JpaRepository<CuttingPlanMaterialPlacementInfo, CuttingPlanMaterialPlacementInfoId>, JpaSpecificationExecutor<CuttingPlanMaterialPlacementInfo> {

	@Query("select cp from CuttingPlanMaterialPlacementInfo cp "
			+ "where cp.cuttingPlanMaterial IN (:reftissus) "
			+ "and (:projet is null or exists(select c from CuttingPlanLight2 c where c.id = cp.cuttingPlan and c.projet = :projet)) "
			+ "order by cp.cuttingPlan")
	List<CuttingPlanMaterialPlacementInfo> findByRefTissus(List<String> reftissus, String projet);

	List<CuttingPlanMaterialPlacementInfo> findByCuttingPlanAndCuttingPlanMaterial(Long cuttingPlan,
			String cuttingPlanMaterial);


}
