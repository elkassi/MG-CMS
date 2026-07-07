package com.lear.MGCMS.repositories.CuttingPlan;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumber;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberId;

public interface CuttingPlanPartNumberRepository extends CrudRepository<CuttingPlanPartNumber, CuttingPlanPartNumberId> {

	@Modifying
	@Transactional
	@Query("delete from CuttingPlanPartNumber where cuttingPlan.id = :id")
	void deleteByCuttingPlanId(Long id);

}
