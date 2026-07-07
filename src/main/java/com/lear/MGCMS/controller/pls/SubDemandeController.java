package com.lear.MGCMS.controller.pls;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.pls.DemandeHistoryService;
import com.lear.MGCMS.services.pls.SubDemandeService;
import com.lear.pls.domain.Demande;
import com.lear.pls.domain.DemandeHistory;
import com.lear.pls.domain.SubDemande;


@RestController
@RequestMapping("/api/plsSubDemande")
public class SubDemandeController {

	@Autowired
	private SubDemandeService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;

	@Autowired
	private UserService userService;
	@Autowired
	private DemandeHistoryService demandeHistoryService;
	@GetMapping("/all")
	private ResponseEntity<?> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy,
            Authentication authentication
            ) {
		User user = userService.findByUsername(authentication.getName());
		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_PLS_READER") || role.getName().equals("ROLE_PLS_ADMIN")) {
				authorized = true; break;
			}
		}
		
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		if (filters == null) {
            filters = new HashMap<>();
        }
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return new ResponseEntity<Page<SubDemande>>(service.findAll(filters, page,size,sortBy), HttpStatus.OK);
	}
	
	@GetMapping("/{id}")
	private ResponseEntity<?> findById(@PathVariable String id,Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());
		boolean authorized = false;
		for(Role role : user.getRoles()) {
			if(role.getName().equals("ROLE_PLS_READER") || role.getName().equals("ROLE_PLS_ADMIN")) {
				authorized = true; break;
			}
		}
		
		if(!authorized) {
			return new ResponseEntity<String>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		}
		return new ResponseEntity<SubDemande>(service.findById(Long.parseLong(id)), HttpStatus.OK);
	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody SubDemande obj, BindingResult result, Authentication authentication) {
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
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		SubDemande newObj = service.save(obj);
		demandeHistoryService.save(new DemandeHistory(user.getLastName() + " " + user.getFirstName(), LocalDateTime.now(), "SAVE", "SubDemande", newObj.toString()));
		return new ResponseEntity<SubDemande>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/delete")
	ResponseEntity<?> delete(@Valid @RequestBody SubDemande obj, BindingResult result, Authentication authentication) {
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
		demandeHistoryService.save(new DemandeHistory(user.getLastName() + " " + user.getFirstName(), LocalDateTime.now(), "DELETE", "SubDemande", obj.toString()));
		return new ResponseEntity<String>("Deleted", HttpStatus.CREATED);
	}
	
	
}
