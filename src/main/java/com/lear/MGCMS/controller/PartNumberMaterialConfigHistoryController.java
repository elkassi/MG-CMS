package com.lear.MGCMS.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.PartNumberMaterialConfigHistory;
import com.lear.MGCMS.services.PartNumberMaterialConfigHistoryService;

@RestController
@RequestMapping("/api/partNumberMaterialConfigHistory")
public class PartNumberMaterialConfigHistoryController {
	
	@Autowired
	private PartNumberMaterialConfigHistoryService service;
	
	@GetMapping("/material/{material}")
	public List<PartNumberMaterialConfigHistory> findByMaterial(@PathVariable String material) {
		return service.findByPartNumberMaterial(material);
	}

}
