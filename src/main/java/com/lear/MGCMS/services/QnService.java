package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.domain.Qn;
import com.lear.MGCMS.repositories.PartNumberBoomRepository;
import com.lear.MGCMS.repositories.QnRepository;

@Service
public class QnService {

	@Autowired
	private QnRepository repo;
	
	@Autowired
	private PartNumberBoomRepository partNumberBoomRepository;
	
	public Page<Qn> findAll(String numeroQn, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection,evalSort).ignoreCase());
        if(numeroQn != null) numeroQn += "%";
		return repo.findByFilter(numeroQn,PageRequest.of(page, size, sortOrderIgnoreCase));
	}
	
	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")){
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

	public Qn findByObjId(String id) {
		// TODO Auto-generated method stub
		return repo.findByObjId(id);
	}

	public Qn save( Qn obj) {
		// Auto-generate numeroQn if null or empty
		String numeroQn = obj.getNumeroQn();
		if(numeroQn == null || numeroQn.trim().isEmpty()) {
			String newNumeroQn = generateNextNumeroQn();
			obj.setNumeroQn(newNumeroQn);
		}
		
		if(obj.getReftissu() != null) {
			PartNumberBoom objPn = partNumberBoomRepository.findFirstByPartNumberMaterial(obj.getReftissu().trim());
			if(objPn != null) {
				obj.setDescription(objPn.getPartNumberMaterialDescription());
			}
		}
		return repo.save(obj);
	}
	
	private String generateNextNumeroQn() {
		List<String> allNumeroQns = repo.findAllNumeroQn();
		int maxNumero = 0;
		
		for(String numeroQn : allNumeroQns) {
			try {
				int numero = Integer.parseInt(numeroQn);
				if(numero > maxNumero) {
					maxNumero = numero;
				}
			} catch(NumberFormatException e) {
				// Skip non-numeric values
			}
		}
		
		int nextNumero = maxNumero + 1;
		return String.valueOf(nextNumero);
	}

	public void delete(Qn oldObj) {
		repo.delete(oldObj);
	}

	public List<Qn> findByReftissu(List<String> reftissus) {
		// TODO Auto-generated method stub
		return repo.findByReftissu(reftissus);
	}
	
}
