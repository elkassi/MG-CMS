package com.lear.MGCMS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.repositories.RoleRepository;

@Service
public class RoleService {

	@Autowired
	private RoleRepository repo;
	
	public List<Role> findAll() {
		return (List<Role>) repo.findAll();
	}

    public Role save(Role role) {
		return repo.save(role);
    }

	public Role findByName(String roleName) {
		return repo.findByName(roleName);
	}
}
