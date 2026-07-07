package com.lear.MGCMS.controller;

import java.util.List;

import javax.persistence.Entity;
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

import com.lear.MGCMS.domain.CodeErreur;
import com.lear.MGCMS.domain.CodeScrap;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.CodeErreurService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/codeErreur")
public class CodeErreurController {
	
	@Autowired 
	private CodeErreurService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	@Autowired
	private UserService userService;

	@GetMapping("/all")
	public Page<CodeErreur> findAll(
			@RequestParam(value = "code", defaultValue = "", required = false) String code,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
			) {
		return service.findAll(code, page,size,sortBy);
	}
	
	@GetMapping("/list")
	public List<CodeErreur> findAll() {
		return service.findAll();
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		CodeErreur obj = service.findByObjId(Long.parseLong(id));
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<CodeErreur>(obj, HttpStatus.OK);
	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody CodeErreur obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		CodeErreur newObj = service.save(obj);
		return new ResponseEntity<CodeErreur>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@Valid @RequestBody CodeErreur obj, Authentication authentication) {
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
