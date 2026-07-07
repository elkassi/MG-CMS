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

import com.lear.MGCMS.domain.Qn;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.QnService;

@RestController
@RequestMapping("/api/qn")
public class QnController {
	
	@Autowired
	private QnService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@GetMapping("/all")
	public Page<Qn> findAll(
			@RequestParam(value = "numeroQn", defaultValue = "", required = false) String numeroQn,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "id,desc", required = false) String sortBy
			) {
		return service.findAll(numeroQn, page,size,sortBy);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findByCode(@PathVariable String id)  {
		Qn obj = service.findByObjId(id);
		if(obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<Qn>(obj, HttpStatus.OK);
	}
		
	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody Qn obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
    	if(errorMap != null) return errorMap;
		Qn newObj = service.save(obj);
		return new ResponseEntity<Qn>(newObj, HttpStatus.CREATED);
	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@RequestBody Qn obj) {
		service.delete(obj);
		return new ResponseEntity<String>("Done", HttpStatus.OK);
	}
	
	@GetMapping("/reftissu")
	public List<Qn> findByReftissu(
			//reftissus as a list of String RequestParam
			@RequestParam(value = "reftissus", required = false) List<String> reftissus
			)  {
		return service.findByReftissu(reftissus);
	}


}
