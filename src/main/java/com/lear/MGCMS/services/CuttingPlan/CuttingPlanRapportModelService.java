package com.lear.MGCMS.services.CuttingPlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRapportModelRepository;

@Service
public class CuttingPlanRapportModelService {

	@Autowired
	private CuttingPlanRapportModelRepository repo;

	public int deleteById(Long id) {
		return repo.deleteID(id);
	}

	public int deleteByCuttingPlanId(Long id) {
		return repo.deleteByCuttingPlanId(id);
	}
	
	
	
}
