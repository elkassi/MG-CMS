package com.lear.MGCMS.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.ZoneService;

@RestController
@RequestMapping("/api/zone")
public class ZoneController {

	@Autowired 
	private ZoneService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@GetMapping("/all")
	public Page<Zone> findAll(
			@RequestParam(value = "nom", defaultValue = "", required = false) String nom,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
			) {
		return service.findAll(nom, page,size,sortBy);
	}
	
	@GetMapping("/list")
	public List<Zone> findAll() {
		return service.findAll();
	}
	
	@GetMapping("/{nom}")
	public ResponseEntity<?> findByCode(@PathVariable String nom)  {
		Zone obj = service.findByObjId(nom);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<Zone>(obj, HttpStatus.OK);
	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody Zone obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		Zone newObj = service.save(obj);
		return new ResponseEntity<Zone>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/delete")
	public void delete(@RequestBody Zone obj) {
		if(obj.getNom() != null) {
			Zone oldObj = service.findByObjId(obj.getNom());
			service.delete(oldObj);

		}
	}
	
}
