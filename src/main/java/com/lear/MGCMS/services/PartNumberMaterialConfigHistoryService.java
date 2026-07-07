package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PartNumberMaterialConfigHistory;
import com.lear.MGCMS.repositories.PartNumberMaterialConfigHistoryRepository;

@Service
public class PartNumberMaterialConfigHistoryService {
	
	@Autowired
	private PartNumberMaterialConfigHistoryRepository repo;
	
	public List<PartNumberMaterialConfigHistory> findByPartNumberMaterial(String partNumberMaterial) {
		return repo.findByPartNumberMaterial(partNumberMaterial);
	}

}
