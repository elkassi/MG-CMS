package com.lear.MGCMS.controller.pls;

import com.lear.MGCMS.services.pls.ProjetPlsService;
import com.lear.pls.domain.Projet;
import com.lear.MGCMS.services.MapValidationErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/plsProjet")
public class ProjetPlsController {
	
	@Autowired
    private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private ProjetPlsService service;
	
	@PostMapping
	public ResponseEntity<?> saveProjet(@Valid @RequestBody Projet obj, BindingResult result, Authentication authentication) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);

        if (errorMap != null) return errorMap;
        Projet dbProjet = service.findByNom(obj.getNom());
        if(dbProjet != null) {
            return new ResponseEntity<String>("ce projet existe déjà", HttpStatus.BAD_REQUEST);

        }
        Projet newProjet = service.save(obj);
        return new ResponseEntity<Projet>(newProjet, HttpStatus.CREATED);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<?> getProjetById(@PathVariable String id) {
		Projet site = service.findById(Long.parseLong(id));
		return new ResponseEntity<Projet>(site,HttpStatus.OK);
	}
	
	@GetMapping("/all")
	public Iterable<Projet> findAll() {
		return service.findAll();
	}


	@GetMapping("/list")
	public List<Projet> findList() {
		return service.findList();
	}

	@PostMapping("/delete/{id}")
	public ResponseEntity<?> deleteById(@PathVariable String id) {
		service.deletebyId(Long.parseLong(id));
		return new ResponseEntity<String>("projet with id: '"+id+"' is deleted", HttpStatus.OK);
	}

}
