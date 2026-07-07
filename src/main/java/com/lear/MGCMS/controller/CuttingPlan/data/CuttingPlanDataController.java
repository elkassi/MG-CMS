package com.lear.MGCMS.controller.CuttingPlan.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanData;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanDataService;

@RestController
@RequestMapping("/api/cuttingPlanData")
public class CuttingPlanDataController {
	
	@Autowired
	private CuttingPlanDataService service;
	
	@GetMapping("/all")
	public Page<CuttingPlanData> findAll(
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

	@GetMapping("/list")
	public List<CuttingPlanData> findByListId(@RequestParam("listId") List<Long> listId) {
		return service.findByListId(listId);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@RequestParam("id") Long id) {
		CuttingPlanData obj = service.findById(id);
		if(obj == null) {
			return new ResponseEntity<>("Cutting Plan Data not found", HttpStatus.NOT_FOUND);
		}
		return ResponseEntity.ok(obj);
	}

}
