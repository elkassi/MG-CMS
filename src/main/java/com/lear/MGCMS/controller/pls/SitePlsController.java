package com.lear.MGCMS.controller.pls;

import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.pls.SitePlsService;
import com.lear.pls.domain.Site;
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
@RequestMapping("/api/plsSite")
public class SitePlsController {

	@Autowired
	private MapValidationErrorService mapValidationErrorService;

	@Autowired
	private SitePlsService service;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> save(@Valid @RequestBody Site obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;
		Site newObj = service.save(obj);
		return new ResponseEntity<>(newObj, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@PathVariable String id) {
		Site obj = service.findById(Long.parseLong(id));
		return new ResponseEntity<>(obj, HttpStatus.OK);
	}

	@GetMapping("/all")
	public ResponseEntity<?> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
			@RequestParam(value = "size", defaultValue = "20", required = false) int size,
			@RequestParam(value = "sort", defaultValue = "id,desc", required = false) String sortBy
	) {
		if (filters == null) {
			filters = new HashMap<>();
		}
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return new ResponseEntity<Page<Site>>(service.findAll(filters, page,size,sortBy), HttpStatus.OK);
	}

	@GetMapping("/list")
	public List<Site> findList() {
		return service.findList();
	}

	@PostMapping("/delete/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteById(@PathVariable String id) {
		service.deleteById(Long.parseLong(id));
		return new ResponseEntity<>("Site with id: '" + id + "' is deleted", HttpStatus.OK);
	}
}
