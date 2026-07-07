package com.lear.MGCMS.controller.cms;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanLight2Repository;
import com.lear.MGCMS.services.cms.PlanCoupeService;
import com.lear.cms.domain.PlanCoupe;

@RestController
@RequestMapping("/api/cms/planCoupe")
public class PlanCoupeController {
	
	@Autowired
	private PlanCoupeService service;
	
	@Autowired
	private CuttingPlanLight2Repository  cuttingPlanLight2Repository;
	
	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@PathVariable String id) {
		try{
			PlanCoupe obj = service.findById(Long.parseLong(id));
			if(obj == null) {
				return new ResponseEntity<String> ("Plan de coupe "+ id + " n'est trouvé dans CMS", HttpStatus.BAD_REQUEST);
			}
			CuttingPlanLight2 cp = cuttingPlanLight2Repository.findByCmsId(Long.parseLong(id));
			if(cp != null) {
				return new ResponseEntity<String> ("Plan de coupe "+ id + " est utilisé ici avec l'id " + cp.getId(), HttpStatus.BAD_REQUEST);
			}
			return new ResponseEntity<PlanCoupe> (obj, HttpStatus.OK);
		}catch(Exception e) {
			System.out.println(e.getMessage());
			return new ResponseEntity<String> ("Plan de coupe "+ id + " n'est trouvé dans CMS", HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/light/{id}")
	public ResponseEntity<?> findByIdLight(@PathVariable String id) {
		PlanCoupe obj = service.findByIdLight(Long.parseLong(id));
		if(obj == null) {
			return new ResponseEntity<String> ("Plan de coupe "+ id + " n'est trouvé dans CMS", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<PlanCoupe> (obj, HttpStatus.OK);
	}


	@GetMapping("/{id}/{cuttingPlanId}")
	public ResponseEntity<?> findById(@PathVariable String id,@PathVariable String cuttingPlanId) {
		try{
			PlanCoupe obj = service.findById(Long.parseLong(id));
			if(obj == null) {
				return new ResponseEntity<String> ("Plan de coupe "+ id + " n'est trouvé dans CMS", HttpStatus.BAD_REQUEST);
			}
			CuttingPlanLight2 cp = cuttingPlanLight2Repository.findByCmsId(Long.parseLong(id));
			if(cp != null && !cp.getId().equals(Long.parseLong(cuttingPlanId))) {
				return new ResponseEntity<String> ("Plan de coupe "+ id + " est utilisé ici avec l'id " + cp.getId(), HttpStatus.BAD_REQUEST);
			}
			return new ResponseEntity<PlanCoupe> (obj, HttpStatus.OK);
		}catch(Exception e) {
			System.out.println(e.getMessage());
			return new ResponseEntity<String> ("Plan de coupe "+ id + " n'est trouvé dans CMS", HttpStatus.BAD_REQUEST);
		}
	}

}
