package com.lear.MGCMS.services.CuttingPlan;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberInfo;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanPartNumberInfoRepository;

@Service
public class CuttingPlanPartNumberInfoService {

	@Autowired
	private CuttingPlanPartNumberInfoRepository repo;
	
	public List<CuttingPlanPartNumberInfo> findAllToWork(LocalDateTime date, List<String> arr) {
		return repo.findAllToWork(date, arr);
	}
	
}
