package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.MachineLsrRapport;
import com.lear.MGCMS.services.MachineLsrRapportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/machineLsrRapport")
public class MachineLsrRapportController {
	
	@Autowired
	private MachineLsrRapportService service;
	
	@GetMapping("/filter")
	public List<MachineLsrRapport> findByFilter(
			@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "shift", required = false) String shift,
			@RequestParam(value = "machine", required = false) String machine) {
		LocalDateTime startDate = null, endDate =null;
		if(shift.equals("2")) {
			startDate = date.atTime(05, 55);
			endDate = date.atTime(13, 55);
		} else if(shift.equals("3")) {
			startDate = date.atTime(13, 55);
			endDate = date.atTime(21, 55);
		} else {
			startDate = date.atTime(21, 55).minusDays(1);
			endDate = date.atTime(05, 55);
		}
		if(endDate.compareTo(LocalDateTime.now()) > 0 ) {
			endDate = LocalDateTime.now();
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//		System.out.println(startDate.format(formatter) + " => "+ endDate.format(formatter) + " : " + machine);

		return service.findBetween(startDate, endDate, machine);

	}

	@GetMapping("/all")
	public Page<MachineLsrRapport> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam int page,
			@RequestParam int size,
			@RequestParam String sort) {
		if (filters == null) {
			filters = new HashMap<>();
		}
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return service.findAll(filters, page, size, sort);
	}

}
