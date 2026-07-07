package com.lear.MGCMS.services.CuttingPlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRapportPlacementRepository;

@Service
public class CuttingPlanRapportPlacementService {

	@Autowired
	private CuttingPlanRapportPlacementRepository repo;

	public void deleteByCuttingPlanId(Long id) {
		repo.deleteByCuttingPlanId(id);
	}

	public int deleteById(Long id) {
		return repo.deleteID(id);
	}
	
	
	
	
}
