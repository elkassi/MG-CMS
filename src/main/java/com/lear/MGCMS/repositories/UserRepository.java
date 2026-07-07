package com.lear.MGCMS.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.lear.MGCMS.domain.User;

@Repository
public interface UserRepository extends CrudRepository<User, String> {
	
	User findByUsername(String username);
	
	User findByMatricule(String matricule);
	
	Iterable<User> findAllByActiveTrue();
		
	@Query("SELECT t from User t WHERE t.firstName + ' ' + t.lastName = :name")
	User findFirstByFullName(String name);

	@Query("SELECT t from User t")
	Page<User> findAll(PageRequest of);

}
