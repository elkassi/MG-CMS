package com.lear.MGCMS.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.AuditQualiteConfig;
import com.lear.MGCMS.services.AuditQualiteConfigService;
import com.lear.MGCMS.services.MapValidationErrorService;

@RestController
@RequestMapping("/api/auditQualiteConfig")
public class AuditQualiteConfigController {
	
	@Autowired 
	private AuditQualiteConfigService service;
	@Autowired
    private MapValidationErrorService mapValidationErrorService;



	@GetMapping("/all")
	public Page<AuditQualiteConfig> findAll(
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
		AuditQualiteConfig obj = service.findById(Long.parseLong(id));
		if(obj == null) {
			return new ResponseEntity<String>("Not Found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<AuditQualiteConfig>(obj, HttpStatus.OK);
	}


	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody AuditQualiteConfig obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
    	AuditQualiteConfig newObj = service.save(obj);
		return new ResponseEntity<AuditQualiteConfig>(newObj, HttpStatus.CREATED);
	}

	@GetMapping("/list")
	public List<AuditQualiteConfig> findList(
	) {
		return service.findList();
	}
	
	@GetMapping("/filter")
	public List<AuditQualiteConfig> findFilter(
			) {
		return service.findList();
	}

}
