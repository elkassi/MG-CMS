package com.lear.MGCMS.services;

import javax.persistence.Id;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.Machine;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.repositories.MachineRepository;

import java.util.List;

@Service
public class MachineService {

	@Autowired
	private MachineRepository repo;
	
	public Page<Machine> findAll(String code, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection,evalSort).ignoreCase());
        
		return repo.findByFilter(code+"%",PageRequest.of(page, size, sortOrderIgnoreCase));
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

	public Machine findByCode(String code) {
		// TODO Auto-generated method stub
		return repo.findByCode(code);
	}

	public Machine save( Machine obj) {
		// TODO Auto-generated method stub
		return repo.save(obj);
	}

    public List<Machine> findList() {
		return repo.findList();
    }
}
