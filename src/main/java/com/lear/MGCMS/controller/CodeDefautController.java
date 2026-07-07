package com.lear.MGCMS.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CodeDefaut;
import com.lear.MGCMS.domain.CodeErreur;
import com.lear.MGCMS.domain.CodeScrap;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.CodeDefautService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/codeDefaut")
public class CodeDefautController {

	@Autowired
	private CodeDefautService service;
	
	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private UserService userService;
	
	@PostMapping
	public ResponseEntity<?> save(@RequestBody CodeDefaut obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null) return errorMap;

		CodeDefaut newObj = service.save(obj);
		return new ResponseEntity<CodeDefaut>(newObj, HttpStatus.CREATED);
	}
	
	@GetMapping("/list")
	public List<CodeDefaut> findAll() {
		return service.findAll();
	}

	@GetMapping("/listC")
	public List<CodeDefaut> findAllC() {
		return service.findAllC();
	}


	@GetMapping("/all")
	public Page<CodeDefaut> findAll(
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
		return service.findAll(filters, page,size,sortBy);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		CodeDefaut obj = service.findById(id);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<CodeDefaut>(obj, HttpStatus.OK);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@Valid @RequestBody CodeDefaut obj, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());
		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_ADMIN")) {
				authorized = true; break;
			}
		}
		
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		service.delete(obj);
		return new ResponseEntity<String>("Deleted", HttpStatus.OK);
	}
	
}
