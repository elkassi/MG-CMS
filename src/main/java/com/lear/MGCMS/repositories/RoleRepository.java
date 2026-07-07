package com.lear.MGCMS.repositories;

import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.Role;


public interface RoleRepository extends CrudRepository<Role, Long> {
	
	Role findByName(String name);
	
}
