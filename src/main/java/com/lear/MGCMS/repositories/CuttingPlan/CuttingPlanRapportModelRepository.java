package com.lear.MGCMS.repositories.CuttingPlan;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanRapportModel;

public interface CuttingPlanRapportModelRepository extends CrudRepository<CuttingPlanRapportModel, Long>{

	@Modifying
	@Transactional
	@Query("delete from CuttingPlanRapportModel where cuttingPlan.id = :id")
	int deleteByCuttingPlanId(Long id);
	@Modifying
	@Transactional
	@Query("delete from CuttingPlanRapportModel where id = :id")
	int deleteID(Long id);
	
}
