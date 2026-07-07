package com.lear.MGCMS.controller;

import javax.validation.Valid;

import com.lear.cms.domain.GammeTechniqueImprimer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Machine;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.MachineService;
import com.lear.MGCMS.services.MapValidationErrorService;

import java.util.List;

@RestController
@RequestMapping("/api/machine")
public class MachineController {
	
	@Autowired
	private MachineService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@GetMapping("/all")
	public Page<Machine> findAll(
			@RequestParam(value = "code", defaultValue = "", required = false) String code,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
			) {
		return service.findAll(code, page,size,sortBy);
	}

	@GetMapping("/list")
	public List<Machine> findList() {
		return service.findList();
	}
	
	@GetMapping("/{code}")
	public ResponseEntity<?> findByCode(@PathVariable String code)  {
		Machine obj = service.findByCode(code);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<Machine>(obj, HttpStatus.OK);
	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody Machine obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		Machine newObj = service.save(obj);
		return new ResponseEntity<Machine>(newObj, HttpStatus.CREATED);
	}

}
