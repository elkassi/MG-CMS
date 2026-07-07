package com.lear.MGCMS.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.services.RoleService;

@RestController
@RequestMapping("/api/role")
public class RoleController {

	@Autowired
	private RoleService service;
	
	@GetMapping("/all")
	public List<Role> findAll() {
		return service.findAll();
	}
	
}
