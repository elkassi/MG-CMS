package com.lear.MGCMS.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Machine;
import com.lear.MGCMS.domain.MachineType;
import com.lear.MGCMS.services.MachineTypeService;

@RestController
@RequestMapping("/api/machineType")
public class MachineTypeController {

	@Autowired
	private MachineTypeService service;
	
	@GetMapping("/list")
	public List<MachineType> findList() {
		return service.findList();
	}
	
	
}
