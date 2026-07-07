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

import com.lear.MGCMS.domain.ProjetVersion;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.ProjetVersionService;

@RestController
@RequestMapping("/api/projetVersion")
public class ProjetVersionController {

	@Autowired
	private ProjetVersionService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@GetMapping("/all")
	public Page<ProjetVersion> findAll(
			@RequestParam(value = "projet", defaultValue = "", required = false) String projet,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "id,desc", required = false) String sortBy
			) {
		return service.findAll(projet, page,size,sortBy);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		ProjetVersion obj = service.findByObjId(Long.parseLong(id));
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<ProjetVersion>(obj, HttpStatus.OK);
	}
	
	@GetMapping("/projet/{projet}")
	public List<ProjetVersion> findByProjet(@PathVariable String projet) {
		return service.findByProjet(projet);
	}
	
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody ProjetVersion obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		ProjetVersion newObj = service.save(obj);
		return new ResponseEntity<ProjetVersion>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@RequestBody ProjetVersion obj) {
		ProjetVersion oldObj = service.findByObjId(obj.getId());
		service.delete(oldObj);
		return new ResponseEntity<String>("Done", HttpStatus.OK);
	}
	
}
