package com.lear.MGCMS.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.lear.MGCMS.domain.PartNumberMaterialConfig;
import com.lear.MGCMS.domain.PlieConfig;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.PlieConfigService;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/plieConfig")
public class PlieConfigController {
	
	@Autowired
	private PlieConfigService service;
	
	@Autowired
	private UserService userService;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@GetMapping("/all")
	private Page<PlieConfig> findAll(
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
	@GetMapping("/projet/{projet}")
	public List<PlieConfig> findByProjetNom(@PathVariable String projet) {
		return service.findByProjetNom(projet);
	}
	
	@GetMapping("/{id}")
	private PlieConfig findById(@PathVariable String id) {
		return service.findById(Long.parseLong(id));
	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody PlieConfig obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
    	
		PlieConfig newObj = service.save(obj);
		return new ResponseEntity<PlieConfig>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@RequestBody PlieConfig obj) {
		service.delete(obj);
		return new ResponseEntity<String>("deleted", HttpStatus.CREATED);
	}

}
