package com.lear.MGCMS.services.scanCoupe;

import com.lear.MGCMS.domain.scanCoupe.UserOp;
import com.lear.MGCMS.repositories.scanCoupe.UserOpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserOpService {

	@Autowired
	private UserOpRepository repo;

	public UserOp findByMatricule(Integer matricule) {
		
		return repo.findByMatricule(matricule);
	}

	public UserOp save(UserOp user) {
		// TODO Auto-generated method stub
		return repo.save(user);
	}

	public List<UserOp> findAll() {
		// TODO Auto-generated method stub
		return (List<UserOp>) repo.findAll();
	}

	public List<UserOp> findAllByBlockTrue() {
		// TODO Auto-generated method stub
		return repo.findAllByBlockTrue();
	} 
	
}
