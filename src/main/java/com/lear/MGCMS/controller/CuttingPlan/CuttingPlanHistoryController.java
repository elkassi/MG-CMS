package com.lear.MGCMS.controller.CuttingPlan;

import java.util.List;

import javax.persistence.Entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanHistory;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanHistoryService;

@RestController
@RequestMapping("/api/cuttingPlanHistory")
public class CuttingPlanHistoryController {

	@Autowired
	private CuttingPlanHistoryService service;
	
	@GetMapping("/cuttingPlan/{cuttingPlan}")
	public List<CuttingPlanHistory> findByCuttingPlan(@PathVariable String cuttingPlan) {
		return service.findByCuttingPlan(Long.parseLong(cuttingPlan));
	}
	
}
