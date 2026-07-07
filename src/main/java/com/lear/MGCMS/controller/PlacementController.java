package com.lear.MGCMS.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Placement;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.PlacementService;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/placement")
public class PlacementController {

	@Autowired
	private PlacementService service;
	
	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private UserService userService;
	
	@GetMapping("/all")
	public Page<Placement> findAll(
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
	
}
