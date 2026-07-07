package com.lear.MGCMS.services.CuttingPlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRapportDrillRepository;

@Service
public class CuttingPlanRapportDrillService {

	@Autowired
	private CuttingPlanRapportDrillRepository repo;

	public int deleteById(Long id) {
		return repo.deleteID(id);
	}

	public int deleteByCuttingPlanId(Long id) {
		return repo.deleteByCuttingPlanId(id);
	}
	
}
