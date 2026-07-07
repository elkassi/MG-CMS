package com.lear.MGCMS.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.Planning;
import com.lear.MGCMS.domain.PlanningDetails;
import com.lear.MGCMS.payload.StatsInfo2;
import com.lear.MGCMS.payload.StatsInfoDTO;
import com.lear.MGCMS.repositories.PlanningDetailsRepository;
import com.lear.MGCMS.repositories.PlanningRepository;

@Service
public class PlanningService {

	@Autowired
	private PlanningRepository repo;
	
	@Autowired
	private PlanningDetailsRepository repo2;

	public List<PlanningDetails> findAll(LocalDate date, String shift) {
		// TODO Auto-generated method stub
		return repo.findAll(date, shift);
	}

	public List<PlanningDetails> saveAll(List<PlanningDetails> arr) {
		// TODO Auto-generated method stub
		return (List<PlanningDetails>) repo2.saveAll(arr);
	}

	public List<StatsInfoDTO> getStatImported(LocalDate date, String shift) {
		// TODO Auto-generated method stub
		return repo2.getStatImported(date, shift);
	}
	
	public List<StatsInfoDTO> getStatNotImported(LocalDate date, String shift) {
		// TODO Auto-generated method stub
		return repo2.getStatNotImported(date, shift);
	}

	public List<StatsInfoDTO> getStat(LocalDate date, String shift) {
		// TODO Auto-generated method stub
		return repo2.getStat(date, shift);
	}

	public Planning findFirstByItem(String item) {
		// TODO Auto-generated method stub
		return repo.findFirstByItem(item);
		
	}
	
}
