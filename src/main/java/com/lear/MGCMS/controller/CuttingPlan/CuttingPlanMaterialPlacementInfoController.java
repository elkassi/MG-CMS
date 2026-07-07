package com.lear.MGCMS.controller.CuttingPlan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacementInfo;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanMaterialPlacementInfoService;

@RestController
@RequestMapping("/api/cuttingPlanMaterialPlacementInfo")
public class CuttingPlanMaterialPlacementInfoController {
	
	@Autowired
	private CuttingPlanMaterialPlacementInfoService service;
	
	@GetMapping("/all")
	public Page<CuttingPlanMaterialPlacementInfo> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
			) {
		if (filters == null) {
            filters = new HashMap<>();
        }
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return service.findAll(filters, page,size,sortBy);
	}
	
	@GetMapping("/reftissus/{reftissus}")
	public List<CuttingPlanMaterialPlacementInfo> findByRefTissus(@PathVariable List<String> reftissus,
			 @RequestParam(value = "projet", required = false) String projet) {
		return service.findByRefTissus(reftissus, projet);
	}

}
