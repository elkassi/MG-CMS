package com.lear.MGCMS.controller.pls;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.pls.DemandeHistoryService;
import com.lear.pls.domain.DemandeHistory;


@RestController
@RequestMapping("/api/plsDemandeHistory")
public class DemandeHistoryController {
	
	@Autowired
	private DemandeHistoryService service;
	
	@Autowired
	private UserService userService;
	
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
		return new ResponseEntity<Page<DemandeHistory>>(service.findAll(filters, page,size,sortBy), HttpStatus.OK);
	}


}
