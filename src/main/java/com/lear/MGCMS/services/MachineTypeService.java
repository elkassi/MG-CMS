package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.Machine;
import com.lear.MGCMS.domain.MachineType;
import com.lear.MGCMS.repositories.MachineTypeRepository;

@Service
public class MachineTypeService {

	@Autowired
	private MachineTypeRepository repo;

	public List<MachineType> findList() {
		// TODO Auto-generated method stub
		return (List<MachineType>) repo.findAll();
	}
	
}
