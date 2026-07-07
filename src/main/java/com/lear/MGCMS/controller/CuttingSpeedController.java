package com.lear.MGCMS.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingSpeed;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.services.CuttingSpeedService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/cuttingSpeed")
public class CuttingSpeedController {
	
	@Autowired
	private CuttingSpeedService service;
	
	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private UserService userService;
	
	@GetMapping("/all")
	public Page<CuttingSpeed> findAll(
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
	public List<CuttingSpeed> findList() {
		return service.findList();
	}
	
	
	@PostMapping
	public ResponseEntity<?> save(@RequestBody CuttingSpeed obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;

		CuttingSpeed newObj = service.save(obj);
		return new ResponseEntity<CuttingSpeed>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@RequestBody CuttingSpeed obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;

		service.delete(obj);
		return new ResponseEntity<String>("Deleted", HttpStatus.CREATED);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		CuttingSpeed obj = service.findById(id);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<CuttingSpeed>(obj, HttpStatus.OK);
	}


}
