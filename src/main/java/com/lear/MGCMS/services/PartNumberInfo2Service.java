package com.lear.MGCMS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PartNumberInfo;
import com.lear.MGCMS.domain.PartNumberInfo2;
import com.lear.MGCMS.repositories.PartNumberInfo2Repository;
import com.lear.MGCMS.repositories.PartNumberInfoRepository;

@Service
public class PartNumberInfo2Service {

	@Autowired
	private PartNumberInfo2Repository repo;
	
	public PartNumberInfo2 findByPartNumber(String partNumber) {
		return repo.findByPartNumber(partNumber);
	}
	
	public PartNumberInfo2 save(PartNumberInfo2 obj) {
		return repo.save(obj);
	}
	
}
