package com.lear.MGCMS.services.pls;

import com.lear.pls.domain.Reftissu;
import com.lear.pls.repositories.ReftissuPlsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReftissuPlsService {

	@Autowired
	private ReftissuPlsRepository repo;

	public Reftissu save(Reftissu obj) {
		return repo.save(obj);
	}

	public Reftissu findById(String id) {
		return repo.findById(id).orElse(null);
	}

	public Iterable<Reftissu> findAll() {
		return repo.findAll();
	}

	public List<Reftissu> findList() {
		return (List<Reftissu>) repo.findAll();
	}

	public void deleteById(String id) {
		repo.deleteById(id);
	}
}
