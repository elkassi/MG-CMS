package com.lear.MGCMS.controller.cms;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.cms.CoupeService;
import com.lear.cms.domain.Coupe;

import java.util.List;

@RestController
@RequestMapping("/api/cms/coupe")
public class CoupeController {

	@Autowired
	private CoupeService service;
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@PostMapping
	public ResponseEntity<?> saveCoupe(@Valid @RequestBody Coupe site, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);

        if (errorMap != null) return errorMap;
        
        Coupe newCoupe = service.save(site);
        return new ResponseEntity<Coupe>(newCoupe, HttpStatus.CREATED);
	}
	
	
	@GetMapping("/all")
	public Iterable<Coupe> findAll() {
		return service.findAll();
	}
	@GetMapping("/{id}")
	public ResponseEntity<?> getFileById(@PathVariable String id) {
		Coupe planCoupe = service.findById(Long.parseLong(id));
		return new ResponseEntity<Coupe>(planCoupe,HttpStatus.OK);
	}
	
	
	@PostMapping("/delete/{id}")
	public ResponseEntity<?> deleteById(@PathVariable String id) {
		service.deletebyId(Long.parseLong(id));
		return new ResponseEntity<String>("Coupe with id: '"+id+"' is deleted", HttpStatus.OK);
	}

	@GetMapping("/sequence/{nof}")
	public List<Coupe> findCoupeByNof(@PathVariable String nof) {
		return (List<Coupe>) service.findByNof(nof);
	}
	
	@GetMapping("/filter/{nof}/{nserie}")
	public ResponseEntity<?> findCoupeByNofAndNserie(@PathVariable String nof, @PathVariable String nserie) {
	
		Coupe planCoupe = service.findByNofAndNserie(nof.trim(), nserie.trim());
		if(planCoupe == null) {
			return new ResponseEntity<String>("Coupe not found",HttpStatus.BAD_REQUEST);

		}
		return new ResponseEntity<Coupe>(planCoupe,HttpStatus.OK);

	}
	
	
}
