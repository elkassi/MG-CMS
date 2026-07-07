package com.lear.MGCMS.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.PartNumberCuttingTimeService;

@RestController
@RequestMapping("/api/partNumberCuttingTime")
public class PartNumberCuttingTimeController {

	@Autowired
	private PartNumberCuttingTimeService cuttingTimeService;

	// Calculate cutting time per PN for a project within a period
	@PostMapping("/calculate")
	public ResponseEntity<?> calculate(@RequestBody Map<String, String> body) {
		String project = body.get("project");
		if (project == null || project.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Le projet est requis"));
		}

		LocalDate startDate = null;
		LocalDate endDate = null;
		try {
			if (body.get("startDate") != null) {
				startDate = LocalDate.parse(body.get("startDate"));
			}
			if (body.get("endDate") != null) {
				endDate = LocalDate.parse(body.get("endDate"));
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("message", "Format de date invalide"));
		}

		List<Map<String, Object>> results = cuttingTimeService.calculateCuttingTimeByProject(project, startDate, endDate);
		return ResponseEntity.ok(results);
	}
}
