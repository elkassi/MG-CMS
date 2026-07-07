package com.lear.MGCMS.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.GammeTechnique;
import com.lear.MGCMS.domain.GammeTechniqueEmp;
import com.lear.MGCMS.domain.GammeTechniquePartNumberMaterial;
import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.services.GammeTechniqueService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.sun.mail.iap.Response;

@RestController
@RequestMapping("/api/gammeTechnique")
public class GammeTechniqueController {

	@Autowired
	private GammeTechniqueService service;
	
	@Autowired
	private UserService userService;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@GetMapping("/all")
	public Page<GammeTechnique> findAll(@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
			@RequestParam(value = "size", defaultValue = "20", required = false) int size,
			@RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy) {
		if (filters == null) {
			filters = new HashMap<>();
		}
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return service.findAll(filters, page, size, sortBy);
	}
	
	@GetMapping("/{partNumber}")
	public GammeTechnique findAll(@PathVariable String partNumber) {
		return service.findByPartNumber(partNumber);
	}
	
	@PostMapping
	@PreAuthorize("hasRole('ENGINEERING') or hasRole('CUTTING_CUIR') or hasRole('QUALITE')")
	public ResponseEntity<?> save(@Valid @RequestBody GammeTechnique obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		User user = userService.findByUsername(authentication.getName());
		if(obj.getPartNumber() == null) {
			return new ResponseEntity<String>("part number ne peut pas être vide", HttpStatus.CREATED);
		}
		
		for(GammeTechniquePartNumberMaterial ctpnm : obj.getGammeTechniquePartNumberMaterials()) {
			ctpnm.setGammeTechnique(obj);
		}
		
		for(GammeTechniqueEmp gte : obj.getGammeTechniqueEmps()) {
			gte.setGammeTechnique(obj);
		}
		
		GammeTechnique oldObj = service.findByPartNumber(obj.getPartNumber());
		if(oldObj != null && oldObj.getCreatedBy() != null) {
			obj.setCreatedBy(oldObj.getCreatedBy());
			obj.setCreatedAt(oldObj.getCreatedAt());
			obj.setUpdatedBy(user);
			obj.setUpdatedAt(LocalDateTime.now());
		} else {
			obj.setCreatedBy(user);
			obj.setCreatedAt(LocalDateTime.now());
		}
		GammeTechnique newObj = service.save(obj);
		
		return new ResponseEntity<GammeTechnique>(newObj, HttpStatus.CREATED);
	}
	
	@DeleteMapping
	@RequestMapping("/{partNumber}")
	@PreAuthorize("hasRole('ENGINEERING') or hasRole('CUTTING_CUIR') or hasRole('QUALITE')")
	public void delete(@PathVariable String partNumber) {
		service.deleteByPartNumber(partNumber);
	}
	
}
