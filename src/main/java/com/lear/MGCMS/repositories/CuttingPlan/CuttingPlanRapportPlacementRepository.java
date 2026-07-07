package com.lear.MGCMS.repositories.CuttingPlan;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanRapportPlacement;

public interface CuttingPlanRapportPlacementRepository extends CrudRepository<CuttingPlanRapportPlacement, Long>{

	@Modifying
	@Transactional
	@Query("delete from CuttingPlanRapportPlacement where cuttingPlan.id = :id")
	void deleteByCuttingPlanId(Long id);

	@Modifying
	@Transactional
	@Query("delete from CuttingPlanRapportPlacement where id = :id")
	int deleteID(Long id);

}
