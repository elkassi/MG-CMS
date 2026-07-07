package com.lear.MGCMS.repositories.CuttingPlan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanCombination;

public interface CuttingPlanCombinationRepository extends JpaRepository<CuttingPlanCombination, Long>, JpaSpecificationExecutor<CuttingPlanCombination> {

}
