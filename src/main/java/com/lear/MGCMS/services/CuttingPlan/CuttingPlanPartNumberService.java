package com.lear.MGCMS.services.CuttingPlan;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumber;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanPartNumberRepository;

@Service
public class CuttingPlanPartNumberService {

	@Autowired
	private CuttingPlanPartNumberRepository repo;

	public void deleteAll(List<CuttingPlanPartNumber> cuttingPlanPartNumbers) {
		repo.deleteAll(cuttingPlanPartNumbers);
	}

	public void deleteByCuttingPlanId(Long id) {
		repo.deleteByCuttingPlanId(id);
	}
	
}
