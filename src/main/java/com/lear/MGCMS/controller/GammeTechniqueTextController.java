package com.lear.MGCMS.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.GammeTechniqueText;
import com.lear.MGCMS.payload.GammeTechniqueTextBulkRequest;
import com.lear.MGCMS.services.GammeTechniqueTextService;

@RestController
@RequestMapping("/api/gammeTechniqueText")
public class GammeTechniqueTextController {

	@Autowired
	private GammeTechniqueTextService service;

	@GetMapping("/resolve")
	public List<GammeTechniqueText> resolve(
			@RequestParam String partNumber,
			@RequestParam(value = "patterns", required = false) List<String> patterns) {
		if (patterns == null) {
			patterns = new ArrayList<String>();
		}
		return service.resolve(partNumber, patterns);
	}

	@PostMapping("/bulk")
	@PreAuthorize("hasRole('ENGINEERING') or hasRole('CUTTING_CUIR') or hasRole('QUALITE')")
	public ResponseEntity<?> saveBulk(@RequestBody GammeTechniqueTextBulkRequest request, Authentication authentication) {
		if (request == null || request.getPartNumber() == null || request.getPartNumber().trim().isEmpty()) {
			return new ResponseEntity<String>("part number ne peut pas être vide", HttpStatus.BAD_REQUEST);
		}
		String username = authentication != null ? authentication.getName() : null;
		List<GammeTechniqueText> saved = service.replaceForPartNumber(
			request.getPartNumber().trim(),
			request.getTexts(),
			request.getDeletedIds(),
			username
		);
		return new ResponseEntity<List<GammeTechniqueText>>(saved, HttpStatus.CREATED);
	}
}
