package com.lear.MGCMS.controller.pls;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.pls.ScrapRapportPlsService;
import com.lear.pls.domain.DemandeHistory;
import com.lear.pls.domain.ScrapRapport;
import com.lear.pls.domain.SubDemande;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/plsScrapRapport")
public class ScrapRapportPlsController {

	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	@Autowired
	private UserService userService;

	@Autowired
	private ScrapRapportPlsService service;

	@GetMapping("/all")
	public ResponseEntity<?> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
			@RequestParam(value = "size", defaultValue = "20", required = false) int size,
			@RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy) {
		if (filters == null) {
			filters = new HashMap<>();
		}
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return new ResponseEntity<>(service.findAll(filters, page, size, sortBy), HttpStatus.OK);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> save(@Valid @RequestBody ScrapRapport obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;
		ScrapRapport newObj = service.save(obj);
		return new ResponseEntity<>(newObj, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@PathVariable Long id) {
		ScrapRapport obj = service.findById(id);
		return new ResponseEntity<>(obj, HttpStatus.OK);
	}

	@PostMapping("/delete")
	ResponseEntity<?> delete(@Valid @RequestBody ScrapRapport obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if(errorMap != null) return errorMap;
		User user = userService.findByUsername(authentication.getName());
		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_PLS_ADMIN")) {
				authorized = true; break;
			}
		}

		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		service.delete(obj);
		return new ResponseEntity<String>("Deleted", HttpStatus.CREATED);
	}

}
