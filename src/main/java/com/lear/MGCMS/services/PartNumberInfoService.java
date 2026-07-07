package com.lear.MGCMS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PartNumberInfo;
import com.lear.MGCMS.repositories.PartNumberInfoRepository;

@Service
public class PartNumberInfoService {

	@Autowired
	private PartNumberInfoRepository repo;
	
	public PartNumberInfo findByPartNumber(String partNumber) {
		return repo.findByPartNumber(partNumber);
	}
	
	public PartNumberInfo save(PartNumberInfo obj) {
		return repo.save(obj);
	}
	
}
