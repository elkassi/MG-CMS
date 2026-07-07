package com.lear.MGCMS.controller.pls;

import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.pls.ReftissuPlsService;
import com.lear.pls.domain.Reftissu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/plsReftissu")
public class ReftissuPlsController {

	@Autowired
	private MapValidationErrorService mapValidationErrorService;

	@Autowired
	private ReftissuPlsService service;

	@GetMapping("/all")
	public Iterable<Reftissu> findAll() {
		return service.findAll();
	}

	@GetMapping("/list")
	public Iterable<Reftissu> findList() {
		return service.findAll();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> save(@Valid @RequestBody Reftissu obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;
		Reftissu newObj = service.save(obj);
		return new ResponseEntity<>(newObj, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@PathVariable String id) {
		Reftissu obj = service.findById(id);
		return new ResponseEntity<>(obj, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> delete(@PathVariable String id) {
		service.deleteById(id);
		return new ResponseEntity<>("Deleted", HttpStatus.OK);
	}
}
