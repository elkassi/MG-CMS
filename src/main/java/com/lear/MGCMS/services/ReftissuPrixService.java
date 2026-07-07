package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.ReftissuPrix;
import com.lear.MGCMS.repositories.ReftissuPrixRepository;

@Service
public class ReftissuPrixService {

	@Autowired
	private ReftissuPrixRepository repo;
	
	public ReftissuPrix save(ReftissuPrix obj) {
		return repo.save(obj);
	}
	
	public List<ReftissuPrix> findArr(List<String> arr) {
		return repo.findList(arr);
	}
	
}
