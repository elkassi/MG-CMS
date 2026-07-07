package com.lear.MGCMS.repositories.CuttingPlan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;

public interface CuttingPlanLightRepository extends JpaRepository<CuttingPlanLight, Long>, JpaSpecificationExecutor<CuttingPlanLight> {
}
