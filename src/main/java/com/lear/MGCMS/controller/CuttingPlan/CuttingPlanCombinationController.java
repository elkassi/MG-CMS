package com.lear.MGCMS.controller.CuttingPlan;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanCombination;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanCombinationPartNumber;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumber;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanCombinationPartNumberService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanCombinationService;

@RestController
@RequestMapping("/api/cuttingPlanCombination")
public class CuttingPlanCombinationController {

	@Autowired
	private CuttingPlanCombinationService service;
	
	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private CuttingPlanCombinationPartNumberService cuttingPlanCombinationPartNumberService;
	
	@PostMapping
	@PreAuthorize("hasRole('CAD')")
	public ResponseEntity<?> save(@RequestBody CuttingPlanCombination obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;
		User user = userService.findByUsername(authentication.getName());
		
    	if(obj.getId() != null) {
    		cuttingPlanCombinationPartNumberService.deleteByCuttingPlanId(obj.getId());
    	}

		
    	List<String> arrPn = new ArrayList<String>();
    	List<String> arrPnQte = new ArrayList<String>();
    	for(CuttingPlanCombinationPartNumber cppn : obj.getCuttingPlanCombinationPartNumbers()) {
    		cppn.setCuttingPlanCombination(obj);
    		arrPn.add(cppn.getPartNumber());
    		arrPnQte.add(cppn.getPartNumber());
    	}
    	
    	String description = String.join("_", arrPnQte);
    	if(obj.getVersion() != null) {
    		description = obj.getVersion() + " " + description;
    	}
    	if(obj.getProjet() != null) {
    		description = obj.getProjet() + " " + description;
    	}
    	obj.setDescription(description);

    	
    	if(obj.getId() == null) {
    		if(obj.getCreatedBy() == null) obj.setCreatedBy(user);
    		if(obj.getCreatedAt() == null) obj.setCreatedAt(LocalDateTime.now());
    		
    		CuttingPlanCombination newObj = service.save(obj);
    		return new ResponseEntity<CuttingPlanCombination>(newObj, HttpStatus.CREATED);
    	}

    	CuttingPlanCombination oldObj = service.findById(obj.getId());
		if(obj.getCreatedBy() == null) obj.setCreatedBy(user);
		if(obj.getCreatedAt() == null) obj.setCreatedAt(LocalDateTime.now());
		obj.setUpdatedBy(user);obj.setUpdatedAt(LocalDateTime.now());
		if(!oldObj.getEnabled().equals(obj.getEnabled())) {
			obj.setEnabledAt(LocalDateTime.now());
			obj.setEnabledBy(user);
		}
		
		CuttingPlanCombination newObj = service.save(obj);
		return new ResponseEntity<CuttingPlanCombination>(newObj, HttpStatus.CREATED);
	}
	@GetMapping("/all")
	public Page<CuttingPlanCombination> findAll(
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
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		CuttingPlanCombination obj = service.findById(Long.parseLong(id));
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<CuttingPlanCombination>(obj, HttpStatus.OK);
	}
	
}
