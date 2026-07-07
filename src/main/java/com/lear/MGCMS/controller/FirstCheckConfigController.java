package com.lear.MGCMS.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.lear.MGCMS.domain.Zone;
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

import com.lear.MGCMS.domain.CoupeMachineHistory;
import com.lear.MGCMS.domain.FirstCheckConfig;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.services.FirstCheckConfigService;
import com.lear.MGCMS.services.FirstCheckService;
import com.lear.MGCMS.services.MapValidationErrorService;

@RestController
@RequestMapping("/api/firstCheckConfig")
public class FirstCheckConfigController {

	@Autowired 
	private FirstCheckConfigService service;
	@Autowired
    private MapValidationErrorService mapValidationErrorService;

	@GetMapping("/all")
	public Page<FirstCheckConfig> findAll(
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
	public List<FirstCheckConfig> findList() {
		return service.findList();
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		FirstCheckConfig obj = service.findById(Long.parseLong(id));
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<FirstCheckConfig>(obj, HttpStatus.OK);
	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody FirstCheckConfig obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
    	FirstCheckConfig newObj = service.save(obj);
		return new ResponseEntity<FirstCheckConfig>(newObj, HttpStatus.CREATED);
	}


	
	@GetMapping("/filter")
	public List<FirstCheckConfig> findList(
			@RequestParam(value = "shift", required = false) String category
			) {
		return service.findList(category);
	}

	@PostMapping("/delete")
	public void delete(@RequestBody FirstCheckConfig obj) {
			service.delete(obj);
	}

}
