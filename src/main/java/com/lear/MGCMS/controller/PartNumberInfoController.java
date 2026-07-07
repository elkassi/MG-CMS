package com.lear.MGCMS.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.PartNumberInfo;
import com.lear.MGCMS.repositories.PartNumberInfoRepository;
import com.lear.MGCMS.services.PartNumberInfoService;

@RestController
@RequestMapping("/api/partNumberInfo")
public class PartNumberInfoController {

	@Autowired
	private PartNumberInfoService partNumberInfoService;

	@Autowired
	private PartNumberInfoRepository partNumberInfoRepository;

	// Read-only: accessible to all authenticated users
	@GetMapping("/list")
	public List<PartNumberInfo> findAll() {
		List<PartNumberInfo> result = new ArrayList<>();
		partNumberInfoRepository.findAll().forEach(result::add);
		return result;
	}

	// Read-only: accessible to all authenticated users
	@GetMapping("/{partNumber}")
	public ResponseEntity<?> findByPartNumber(@PathVariable String partNumber) {
		PartNumberInfo info = partNumberInfoService.findByPartNumber(partNumber);
		if (info == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(info);
	}

	// Write: only ROLE_ADMIN or ROLE_PROCESS
	@PreAuthorize("hasRole('ADMIN') or hasRole('PROCESS')")
	@PostMapping
	public ResponseEntity<PartNumberInfo> save(@RequestBody PartNumberInfo partNumberInfo) {
		PartNumberInfo saved = partNumberInfoService.save(partNumberInfo);
		return ResponseEntity.ok(saved);
	}
}
