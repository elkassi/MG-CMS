package com.lear.MGCMS.services.pls;

import com.lear.pls.domain.ReftissuAlert;
import com.lear.pls.repositories.ReftissuAlertPlsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReftissuAlertPlsService {

	@Autowired
	private ReftissuAlertPlsRepository repo;

	public ReftissuAlert save(ReftissuAlert obj) {
		return repo.save(obj);
	}

	public ReftissuAlert findById(String id) {
		return repo.findById(id).orElse(null);
	}

	public Iterable<ReftissuAlert> findAll() {
		return repo.findAll();
	}

	public List<ReftissuAlert> findList() {
		return (List<ReftissuAlert>) repo.findAll();
	}

	public void deleteById(String id) {
		repo.deleteById(id);
	}
}
