package com.lear.MGCMS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.repositories.ReftissuMachineRepository;

@Service
public class ReftissuMachineService {

	@Autowired
	private ReftissuMachineRepository repo;
	
}
