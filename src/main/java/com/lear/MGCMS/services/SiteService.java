package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.Site;
import com.lear.MGCMS.repositories.SiteRepository;

@Service
public class SiteService {

	@Autowired
	private SiteRepository repo;

	public List<Site> findAll() {
		// TODO Auto-generated method stub
		return (List<Site>) repo.findAll();
	}
	
	
}
