package com.lear.MGCMS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.ProjetReftissu;
import com.lear.MGCMS.repositories.ProjetReftissuRepository;

@Service
public class ProjetReftissuService {

	@Autowired
	private ProjetReftissuRepository repo;
	
	public Page<ProjetReftissu> findAll(String projet, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection,evalSort).ignoreCase());
        if(projet != null) projet += "%";
		return repo.findByFilter(projet,PageRequest.of(page, size, sortOrderIgnoreCase));
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

	public ProjetReftissu findByObjId(Long id) {
		// TODO Auto-generated method stub
		return repo.findByObjId(id);
	}

	public ProjetReftissu save( ProjetReftissu obj) {
		// TODO Auto-generated method stub
		return repo.save(obj);
	}
	
}
