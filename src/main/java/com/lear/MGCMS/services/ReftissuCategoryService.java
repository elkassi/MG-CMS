package com.lear.MGCMS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.ReftissuCategory;
import com.lear.MGCMS.repositories.ReftissuCategoryRepository;

@Service
public class ReftissuCategoryService {
	
	@Autowired
	private ReftissuCategoryRepository repo;

}
