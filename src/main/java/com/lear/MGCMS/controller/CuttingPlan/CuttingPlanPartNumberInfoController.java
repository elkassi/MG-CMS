package com.lear.MGCMS.controller.CuttingPlan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberInfo;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanLight2Repository;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanPartNumberInfoService;

@RestController
@RequestMapping("/api/cuttingPlanPartNumberInfo")
public class CuttingPlanPartNumberInfoController {
	
	@Autowired
	private CuttingPlanPartNumberInfoService service;
	
	@Autowired
	private CuttingPlanLight2Repository cuttingPlanLight2Repository;
	
	@PostMapping("/to-work")
	public List<CuttingPlanPartNumberInfo> findAllToWork(@RequestBody List<String> arr) {
		arr.removeIf(Objects::isNull);
		System.out.println("arr size : " + arr.size());
		List<CuttingPlanPartNumberInfo> data = service.findAllToWork(LocalDateTime.now(), arr);
		for(CuttingPlanPartNumberInfo obj : data) {
			obj.setCmsId(cuttingPlanLight2Repository.findCmsIdById(obj.getCuttingPlan()));
		}
		return data;
	}

	
	

}
