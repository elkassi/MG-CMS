package com.lear.MGCMS.controller.pls;

import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.pls.ReftissuAlertPlsService;
import com.lear.pls.domain.ReftissuAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/plsReftissuAlert")
public class ReftissuAlertPlsController {

	@Autowired
	private MapValidationErrorService mapValidationErrorService;

	@Autowired
	private ReftissuAlertPlsService service;

	@GetMapping("/all")
	public Iterable<ReftissuAlert> findAll() {
		return service.findAll();
	}

	@GetMapping("/list")
	public Iterable<ReftissuAlert> findList() {
		return service.findAll();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> save(@Valid @RequestBody ReftissuAlert obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;
		ReftissuAlert newObj = service.save(obj);
		return new ResponseEntity<>(newObj, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@PathVariable String id) {
		ReftissuAlert obj = service.findById(id);
		return new ResponseEntity<>(obj, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> delete(@PathVariable String id) {
		service.deleteById(id);
		return new ResponseEntity<>("Deleted", HttpStatus.OK);
	}
}
