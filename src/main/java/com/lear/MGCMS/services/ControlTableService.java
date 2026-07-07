package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.ControlTable;
import com.lear.MGCMS.repositories.ControlTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ControlTableService {
	
	@Autowired
	private ControlTableRepository repo;

	public ControlTable findByName(String hostName) {
		// TODO Auto-generated method stub
		return repo.findFirstByPcName(hostName);
	}

	public List<ControlTable> findAll() {
		// TODO Auto-generated method stub
		return (List<ControlTable>) repo.findAll();
	}
}
