package com.lear.MGCMS.repositories.CuttingPlan;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanCombinationPartNumber;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanCombinationPartNumberId;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumber;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberId;

public interface CuttingPlanCombinationPartNumberRepository extends CrudRepository<CuttingPlanCombinationPartNumber, CuttingPlanCombinationPartNumberId> {

	@Modifying
	@Transactional
	@Query("delete from CuttingPlanCombinationPartNumber where cuttingPlanCombination.id = :id")
	void deleteByCuttingPlanId(Long id);

}
