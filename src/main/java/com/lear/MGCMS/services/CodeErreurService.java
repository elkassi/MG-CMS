package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CodeErreur;
import com.lear.MGCMS.repositories.CodeErreurRepository;

@Service
public class CodeErreurService {
	
	@Autowired
	private CodeErreurRepository repo;
	
	public Page<CodeErreur> findAll(String code, int page, int size, String sort) {
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

	public CodeErreur findByObjId(Long id) {
		// TODO Auto-generated method stub
		return repo.findByObjId(id);
	}

	public CodeErreur save( CodeErreur obj) {
		// TODO Auto-generated method stub
		return repo.save(obj);
	}

	public List<CodeErreur> findAll() {
		// TODO Auto-generated method stub
		return (List<CodeErreur>) repo.findAll();
	}

	public void delete(CodeErreur oldObj) {
		repo.delete(oldObj);
	}

}
