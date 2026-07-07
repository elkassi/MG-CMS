package com.lear.MGCMS.services.CuttingPlan;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanHistory;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanHistoryRepository;

@Service
public class CuttingPlanHistoryService {

	@Autowired
	private CuttingPlanHistoryRepository repo;
	
	public List<CuttingPlanHistory> findByCuttingPlan(Long cuttingPlan) {
		return repo.findByCuttingPlan(cuttingPlan);
	}
	
}
