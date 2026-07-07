package com.lear.MGCMS.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Projet;
import com.lear.MGCMS.domain.Site;
import com.lear.MGCMS.services.SiteService;

@RestController
@RequestMapping("/api/site")
public class SiteController {

	@Autowired
	private SiteService service;
	
	@GetMapping("/all")
	public List<Site> findAll() {
		return service.findAll();
	}
	
	@GetMapping("/list")
	public List<Site> findAll2() {
		return service.findAll();
	}
	
	
}
