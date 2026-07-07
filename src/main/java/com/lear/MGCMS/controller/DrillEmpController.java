package com.lear.MGCMS.controller;

import java.time.LocalDateTime;
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

import com.lear.MGCMS.domain.DrillEmp;
import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.DrillEmpService;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/drillEmp")
public class DrillEmpController {

	@Autowired
	private DrillEmpService service;
	
	@Autowired
	private UserService userService;

	@Autowired
    private MapValidationErrorService mapValidationErrorService;

	
	
	@GetMapping("/all")
	private ResponseEntity<?> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy,
            Authentication authentication
            ) {
//		User user = userService.findByUsername(authentication.getName());
//		boolean authorized = false;
//		for(Role role : user.getRoles()) {
//			if(role.getName().equals("ROLE_CAD")) {
//				authorized = true; break;
//			}
//		}
//		
//		if(!authorized) {
//			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
//		}
		if (filters == null) {
            filters = new HashMap<>();
        }
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return new ResponseEntity<Page<DrillEmp>>(service.findAll(filters, page,size,sortBy), HttpStatus.OK);

	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody DrillEmp obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		User user = userService.findByUsername(authentication.getName());
		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_CAD")) {
				authorized = true; break;
			}
			if(role.getName().equals("ROLE_CAD_FOAM")) {
				authorized = true; break;
			}
		}
		
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		
		DrillEmp newObj = service.save(obj);
		return new ResponseEntity<DrillEmp>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/list")
	public List<DrillEmp> getList(@RequestBody List<String> patterns) {
		return service.getList(patterns);
	}
	
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@PathVariable String id, Authentication authentication) {
		// TODO Auto-generated method stub
		User user = userService.findByUsername(authentication.getName());
		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_CAD")) {
				authorized = true; break;
			}
			if(role.getName().equals("ROLE_CAD_FOAM")) {
				authorized = true; break;
			}
		}
		
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		return new ResponseEntity<DrillEmp>(service.findById(id), HttpStatus.OK);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@Valid @RequestBody DrillEmp obj, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());
		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_CAD")) {
				authorized = true; break;
			}
			if(role.getName().equals("ROLE_CAD_FOAM")) {
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
