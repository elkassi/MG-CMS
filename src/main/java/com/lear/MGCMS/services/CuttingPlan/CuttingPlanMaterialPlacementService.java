package com.lear.MGCMS.services.CuttingPlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanMaterialPlacementRepository;

@Service
public class CuttingPlanMaterialPlacementService {

	@Autowired
	private CuttingPlanMaterialPlacementRepository repo;

	public void deleteByCuttingPlanId(Long id) {
		repo.deleteByCuttingPlanId(id);
	}
	
}
