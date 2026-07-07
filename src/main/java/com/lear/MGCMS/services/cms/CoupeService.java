package com.lear.MGCMS.services.cms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.cms.domain.Coupe;
import com.lear.cms.repositories.CoupeRepository;

import java.util.List;

@Service
public class CoupeService {

	@Autowired
	private CoupeRepository repository;
	
	public Coupe save(Coupe obj) {
		return repository.save(obj);
	}
	
	public Coupe findById(Long id) {
		return repository.getCoupeById(id);
	}
	
	public Iterable<Coupe> findAll() {
		return repository.findAll();
	}
	
	public void deletebyId(Long id) {
		repository.deleteById(id);
	}
	
	public Coupe findByNofAndNserie(String nof, String nserie) {
		return repository.findFirstByNofAndNserie(nof, Long.parseLong(nserie));
	}


    public List<Coupe> findByNof(String nof) {
		return repository.findByNof(nof);
    }
}
