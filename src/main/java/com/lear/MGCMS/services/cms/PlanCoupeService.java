package com.lear.MGCMS.services.cms;

import java.time.LocalDate;
import java.util.*;

import org.hibernate.jdbc.Expectations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import com.lear.cms.domain.PlanCoupe;
import com.lear.cms.domain.SpreadingCuttingPlanCoupe;
import com.lear.cms.repositories.DrillPlanCoupeRepository;
import com.lear.cms.repositories.PartNumberPlanCoupeRepository;
import com.lear.cms.repositories.PlanCoupeRepository;
import com.lear.cms.repositories.SpreadingCuttingPlanCoupeRepository;

@Service
public class PlanCoupeService {

	@Autowired
	private PlanCoupeRepository repo;
	
	@Autowired
	private PartNumberPlanCoupeRepository partNumberPlanCoupeRepository;
	
	@Autowired
	private SpreadingCuttingPlanCoupeRepository spreadingCuttingPlanCoupeRepository;
	
	@Autowired
	private DrillPlanCoupeRepository drillPlanCoupeRepository;

	public List<PlanCoupe> findAllLight() {
		// sort by idPlanCoupe desc (List<PlanCoupe>) repo.findAll()
		List<PlanCoupe> arr = (List<PlanCoupe>) repo.findAll();
		Collections.sort(arr, Comparator.comparingLong(PlanCoupe::getIdPlanCoupe).reversed());
		return arr;
	}

	public PlanCoupe findById(Long id) {
		// TODO Auto-generated method stub
		Optional<PlanCoupe> objOpt = repo.findById(id);
		if(!objOpt.isPresent()) {
			return null;
		}
		
		PlanCoupe obj = objOpt.get();
		obj.setPartNumberPlanCoupes(partNumberPlanCoupeRepository.findByIdPartNumberPlanForeignPlanCoupe(id));
		
		List<SpreadingCuttingPlanCoupe> arrSc = new ArrayList<SpreadingCuttingPlanCoupe>();
		for(SpreadingCuttingPlanCoupe sc : spreadingCuttingPlanCoupeRepository.findByIdSpreadingPlanForeignPlanCoupe(id)) {
			sc.setDrillPlanCoupes(drillPlanCoupeRepository.findByIdCuttingForeignPlanCoupe(sc.getIdSpreadingCuttingPlanCoupe()));
			arrSc.add(sc);
		}
		obj.setSpreadingCuttingPlanCoupes(arrSc);
		
		return obj;
	}

	public PlanCoupe findByIdLight(Long id) {
		// TODO Auto-generated method stub
		Optional<PlanCoupe> objOpt = repo.findById(id);
		if(!objOpt.isPresent()) {
			return null;
		}

		PlanCoupe obj = objOpt.get();
		return obj;
	}


	public Long maxId() {
		// TODO Auto-generated method stub
		Long max = repo.findMaxId();
		if(max == null) return 0L;
		return max;
	}


	public List<PlanCoupe> findAllLightBetween(LocalDate date1, LocalDate date2) {
		// TODO Auto-generated method stub
		return repo.findAllBetween(date1, date2);
	}

	public PlanCoupe save(PlanCoupe pc) {
		// TODO Auto-generated method stub
		return repo.save(pc);
	}

    public List<PlanCoupe> findPlanWithNoTimingModel() {
		// TODO Auto-generated method stub
		return repo.findPlanWithNoTimingModel();
    }
}
