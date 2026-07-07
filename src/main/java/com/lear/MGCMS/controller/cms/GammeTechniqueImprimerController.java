package com.lear.MGCMS.controller.cms;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.ctc.GammeTechniqueImprimerService;
import com.lear.cms.domain.GammeTechniqueImprimer;

@RestController
@RequestMapping("/api/gammeTechniqueImprimer")
public class GammeTechniqueImprimerController {

	@Autowired
	private GammeTechniqueImprimerService service;
	@Autowired
	private UserService userService;

	@GetMapping("/sequnce/{sequence}")
	public List<GammeTechniqueImprimer> findBySequence(@PathVariable String sequence) {
		return service.findBySequence(sequence);
	}

	@GetMapping("/serie/{serie}")
	public ResponseEntity<?> findBySerie(@PathVariable String serie) {
		List<GammeTechniqueImprimer> results = service.findByNSerieGammeImp(serie);
		if (results == null || results.isEmpty()) {
			return new ResponseEntity<String>(serie + " n'est pas trouvé dans la table des gammes CMS", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(results.get(0), HttpStatus.OK);
	}

	@GetMapping("/list")
	public List<GammeTechniqueImprimer> findBySequence() {
		return service.findList();
	}
	
	@PostMapping("/update")
	@PreAuthorize("hasRole('ADMIN') or hasRole('IMPORTER')")
	public ResponseEntity<?> update(@RequestBody GammeTechniqueImprimer obj, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());

		GammeTechniqueImprimer newObj = service.save(obj, user);
		return new ResponseEntity<GammeTechniqueImprimer> (newObj, HttpStatus.OK);
	}
	
}
