package com.lear.MGCMS.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Planning;
import com.lear.MGCMS.domain.PlanningDetails;
import com.lear.MGCMS.services.PlanningService;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.payload.StatsInfo2;
import com.lear.MGCMS.payload.StatsInfoDTO;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/planning")
public class PlanningController {

	@Autowired
	private PlanningService service;
	
	@Autowired
	private UserService userService; 
	
	@GetMapping("/{date}/{shift}")
	public List<PlanningDetails> findAll(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PathVariable String shift){
		return service.findAll(date, shift);
	}
	
	@PostMapping("/planned")
	public ResponseEntity<?> savePlanned(@RequestBody List<PlanningDetails> arr, Authentication authentication) {
		LocalDateTime now = LocalDateTime.now();
		User user = userService.findByUsername(authentication.getName());
		for(PlanningDetails obj: arr) {
			obj.setPlannedDate(now);
			obj.setPlannedBy(user);
		}
		return new ResponseEntity<List<PlanningDetails>>(service.saveAll(arr), HttpStatus.OK);
	}
	
	@GetMapping("/stats/imported/{date}/{shift}")
	public List<StatsInfoDTO> getStatImported(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PathVariable String shift) {
		return service.getStatImported(date, shift);
	}
	
	@GetMapping("/stats/not-imported/{date}/{shift}")
	public List<StatsInfoDTO> getStatNotImported(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PathVariable String shift) {
		return service.getStatNotImported(date, shift);
	}
	
	@GetMapping("/stats/{date}/{shift}")
	public List<StatsInfoDTO> getStat(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PathVariable String shift) {
		return service.getStat(date, shift);
	}
}
