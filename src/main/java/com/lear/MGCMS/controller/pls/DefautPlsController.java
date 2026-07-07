package com.lear.MGCMS.controller.pls;

import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.pls.DefautPlsService;
import com.lear.pls.domain.Defaut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plsDefaut")
public class DefautPlsController {

	@Autowired
	private MapValidationErrorService mapValidationErrorService;

	@Autowired
	private DefautPlsService service;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> save(@Valid @RequestBody Defaut obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;
		Defaut newObj = service.save(obj);
		return new ResponseEntity<>(newObj, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@PathVariable String id) {
		Defaut obj = service.findById(id);
		return new ResponseEntity<>(obj, HttpStatus.OK);
	}

	@GetMapping("/all")
	public ResponseEntity<?> findAll(
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
		return new ResponseEntity<Page<Defaut>>(service.findAll(filters, page,size,sortBy), HttpStatus.OK);
	}

	@GetMapping("/list")
	public List<Defaut> findList() {
		return service.findList();
	}

	@PostMapping("/delete/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteById(@PathVariable String id) {
		service.deleteById(id);
		return new ResponseEntity<>("Defaut with code: '" + id + "' is deleted", HttpStatus.OK);
	}
}
