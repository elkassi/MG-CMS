package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ZoneRepository;

@Service
public class ZoneService {

	@Autowired
	private ZoneRepository repo;
	
	public Page<Zone> findAll(String nom, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection,evalSort).ignoreCase());
        
		return repo.findByFilter(nom+"%",PageRequest.of(page, size, sortOrderIgnoreCase));
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

	public Zone findByObjId(String id) {
		// TODO Auto-generated method stub
		return repo.findByObjId(id);
	}

	public Zone save( Zone obj) {
		// TODO Auto-generated method stub
		return repo.save(obj);
	}

	public List<Zone> findAll() {
		// TODO Auto-generated method stub
		return (List<Zone>) repo.findAll();
	}

	public void delete(Zone oldObj) {
		repo.delete(oldObj);
	}
	
}
