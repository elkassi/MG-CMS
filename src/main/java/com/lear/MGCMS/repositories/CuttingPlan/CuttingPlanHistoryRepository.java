package com.lear.MGCMS.repositories.CuttingPlan;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.PartNumberMaterialConfigHistory;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanHistory;

public interface CuttingPlanHistoryRepository extends CrudRepository<CuttingPlanHistory, Long>{

	List<CuttingPlanHistory> findByCuttingPlan(Long cuttingPlan);
	
}
