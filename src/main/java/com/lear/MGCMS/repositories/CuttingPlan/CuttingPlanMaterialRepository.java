package com.lear.MGCMS.repositories.CuttingPlan;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterial;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialId;

public interface CuttingPlanMaterialRepository extends CrudRepository<CuttingPlanMaterial, CuttingPlanMaterialId> {

	@Modifying
	@Transactional
	@Query("delete from CuttingPlanMaterial where cuttingPlan.id = :id")
	void deleteByCuttingPlanId(Long id);
	
	@Modifying
	@Transactional
	@Query("delete from CuttingPlanMaterial where cuttingPlan.id = :id and partNumberMaterial = :partNumberMaterial")
	void deleteByCuttingPlanIdAndPartNumberMaterial(Long id, String partNumberMaterial);
	
	@Modifying
	@Transactional
	@Query(value = "Update CuttingPlanMaterial "
			+ "SET vitesse = :vitesse "
			+ ", rotation = :rotation "
			+ ", tauxScrap = :tauxScrap "
			+ ", matelassageEndroit = :matelassageEndroit "		
			+ "where partNumberMaterial = :partNumberMaterial ", nativeQuery = true)
	void update(String partNumberMaterial, Integer vitesse , String rotation,
			String tauxScrap, String matelassageEndroit);


}
