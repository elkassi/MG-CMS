package com.lear.MGCMS.controller.CuttingPlan;

import com.lear.MGCMS.services.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.CuttingPlan.CuttingPlanRapportDrillService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanRapportModelService;
import com.lear.MGCMS.services.CuttingPlan.CuttingPlanRapportPlacementService;
@RestController
@RequestMapping("/api/cuttingPlanRapportFunctions")
public class CuttingPlanRapportFunctionsController {
	
	@Autowired
	private CuttingPlanRapportPlacementService cuttingPlanRapportPlacementService;
	@Autowired
	private CuttingPlanRapportModelService cuttingPlanRapportModelService;
	@Autowired
	private CuttingPlanRapportDrillService cuttingPlanRapportDrillService;
	@Autowired
	private QueryService queryService;
	@PostMapping("/rapport-placement/cp/{cpid}")
	@PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
	public ResponseEntity<?> deletecpRapport1ById(@PathVariable String cpid) {
		System.out.println(cpid);
		cuttingPlanRapportPlacementService.deleteByCuttingPlanId(Long.parseLong(cpid));
//		queryService.deleteRapportPlacementByCuttingPlanId(Long.parseLong(cpid));
		return new ResponseEntity<String> ("Deleted", HttpStatus.OK);
	}

	@PostMapping("/rapport-modele/cp/{cpid}")
	@PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
	public ResponseEntity<?> deletecpRapport2ById(@PathVariable String cpid) {
		int deleted = cuttingPlanRapportModelService.deleteByCuttingPlanId(Long.parseLong(cpid));
		return new ResponseEntity<String> (deleted +" deleted", HttpStatus.OK);
	}
	@PostMapping("/rapport-drill/cp/{cpid}")
	@PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
	public ResponseEntity<?> deletecpRapport3ById(@PathVariable String cpid) {
		int deleted = cuttingPlanRapportDrillService.deleteByCuttingPlanId(Long.parseLong(cpid));
		return new ResponseEntity<String> (deleted +" deleted", HttpStatus.OK);
	}

	@PostMapping("/rapport-placement/{id}")
	@PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
	public ResponseEntity<?> deleteRapport1ById(@PathVariable String id) {
		int deleted = cuttingPlanRapportPlacementService.deleteById(Long.parseLong(id));
		if(deleted != 0) {
			return new ResponseEntity<String> (deleted +" deleted", HttpStatus.OK);
		} else {
			return new ResponseEntity<String> ("Nothing deleted", HttpStatus.BAD_REQUEST);
		}
	}



	@PostMapping("/rapport-modele/{id}")
	@PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
	public ResponseEntity<?> deleteRapport2ById(@PathVariable String id) {
		int deleted = cuttingPlanRapportModelService.deleteById(Long.parseLong(id));
		if(deleted != 0) {
			return new ResponseEntity<String> (deleted +" deleted", HttpStatus.OK);
		} else {
			return new ResponseEntity<String> ("Nothing deleted", HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/rapport-drill/{id}")
	@PreAuthorize("hasRole('CAD') or hasRole('CAD_FOAM')")
	private ResponseEntity<?> deleteRapport3ById(@PathVariable String id) {
		int deleted = cuttingPlanRapportDrillService.deleteById(Long.parseLong(id));
		if(deleted != 0) {
			return new ResponseEntity<String> (deleted +" deleted", HttpStatus.OK);
		} else {
			return new ResponseEntity<String> ("Nothing deleted", HttpStatus.BAD_REQUEST);
		}
	}

}
