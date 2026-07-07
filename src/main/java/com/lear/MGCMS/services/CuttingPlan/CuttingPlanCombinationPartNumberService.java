package com.lear.MGCMS.services.CuttingPlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanCombinationPartNumberRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanPartNumberRepository;

@Service
public class CuttingPlanCombinationPartNumberService {
	
	@Autowired
	private CuttingPlanCombinationPartNumberRepository repo;

	
	public void deleteByCuttingPlanId(Long id) {
		repo.deleteByCuttingPlanId(id);
	}


}
