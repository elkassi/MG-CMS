package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.ControlTable;
import com.lear.MGCMS.services.ControlTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@RestController
@RequestMapping("/api/controlTable")
public class ControlTableController {

	private static final Logger log = LoggerFactory.getLogger(ControlTableController.class);

	@Autowired
	private ControlTableService service;
	
	@GetMapping("/pc-mine")
	public ResponseEntity<?> findByPcname() {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			ControlTable obj = service.findByName(addr.getHostName());
			if(obj != null) {
				return new ResponseEntity<ControlTable>(obj, HttpStatus.OK);
			} 
			return new ResponseEntity<String>(addr.getHostName() + " not found", HttpStatus.BAD_REQUEST);
		} catch (UnknownHostException e) {
			log.error("ControlTableController PC name lookup failed", e);
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/list")
	public List<ControlTable> findAll() {
		return service.findAll();
	}
	
	
	
}
