package com.lear.MGCMS.controller.scanCoupe;

import com.lear.MGCMS.domain.scanCoupe.Config;
import com.lear.MGCMS.domain.scanCoupe.UserOp;
import com.lear.MGCMS.services.scanCoupe.ConfigService;
import com.lear.MGCMS.services.scanCoupe.UserOpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/userOp")
public class UserOpController {

	@Autowired
	private UserOpService service;
	
	@Autowired
	private ConfigService configService;
	
	@PostMapping
	public ResponseEntity<?> save(@RequestBody UserOp obj, BindingResult result) {
		return new ResponseEntity<UserOp>(service.save(obj),HttpStatus.OK);  
	}
	
	@GetMapping
	public List<UserOp> findAll() {
		return service.findAll();
	}
	
	@GetMapping("/{matricule}/{code}")
	public ResponseEntity<?> resetPsw(@PathVariable Integer matricule, @PathVariable String code) {
		Config config2 = configService.findByParam("resetCode");
		if(!code.equals(config2.getValue())) {
			return new ResponseEntity<String>("BAD CODE",HttpStatus.BAD_REQUEST);  
		} 
		UserOp user = service.findByMatricule(matricule);
		if(user == null) {
			return new ResponseEntity<String>("Aucun utilisateur avec cette matricule",HttpStatus.BAD_REQUEST);
		}
		user.setPassword(null);
		service.save(user);
		return new ResponseEntity<String>(matricule + " est débloqué",HttpStatus.OK);
	}
	
}
