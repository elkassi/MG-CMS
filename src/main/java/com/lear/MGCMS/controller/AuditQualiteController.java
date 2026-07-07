package com.lear.MGCMS.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lear.MGCMS.domain.FirstCheck;
import com.lear.MGCMS.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.AuditQualite;
import com.lear.MGCMS.services.AuditQualiteService;

@RestController
@RequestMapping("/api/auditQualite")
public class AuditQualiteController {
	
	@Autowired 
	private AuditQualiteService service;
	
	@PostMapping
	private ResponseEntity<?> save(@RequestBody AuditQualite obj) {
		return new ResponseEntity<AuditQualite> (service.save(obj), HttpStatus.CREATED);
	}
	
	@GetMapping
	private ResponseEntity<?> findBetween(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = true) String shift,
			@RequestParam(value = "tableControle", required = true) String tableControle
			) {
		return new ResponseEntity<List<AuditQualite>> (service.findList(date, shift, tableControle), HttpStatus.OK);
	}
	
	@GetMapping("/all")
	public Page<AuditQualite> findAll(
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

	@GetMapping("/filtre")
	private ResponseEntity<?> getStat(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = false) String shift
	) {
		return new ResponseEntity<List<AuditQualite>> (service.getList(date, shift), HttpStatus.CREATED);
	}


	@PostMapping("/validateAction")
	private ResponseEntity<?> validateAction(@RequestBody AuditQualite obj, Authentication authentication) {
		AuditQualite oldObj = service.findById(obj.getId());
		if(oldObj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		if(oldObj.getActionBy() != null) {
			return new ResponseEntity<String>("Action already validated", HttpStatus.BAD_REQUEST);
		}
		if(obj.getAction() == null || obj.getAction().isEmpty()) {
			return new ResponseEntity<String>("Action is required", HttpStatus.BAD_REQUEST);
		}
		UserDetailsImpl user = (UserDetailsImpl)authentication.getPrincipal();
		obj.setActionBy(user.getLastName() + " " + user.getFirstName() + " (" + user.getMatricule() + ")");
		obj.setActionDate(LocalDateTime.now());
		return new ResponseEntity<AuditQualite> (service.save(obj), HttpStatus.CREATED);
	}


}
