package com.lear.MGCMS.services.CuttingRequest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestBoxInfo;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository;

@Service
public class CuttingRequestBoxInfoService {
	
	@Autowired
	private  CuttingRequestBoxInfoRepository repo;
	
	public List<CuttingRequestBoxInfo> findAll(LocalDate date, String shift) {
		// TODO Auto-generated method stub
		return repo.findAll(date, shift);
	}

}
