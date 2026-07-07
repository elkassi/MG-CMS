package com.lear.pls.repositories;

import com.lear.pls.domain.Projet;
import org.springframework.data.repository.CrudRepository;

public interface ProjetPlsRepository extends CrudRepository<Projet, Long> {
	
	Projet getProjetPlsById(Long id);
		
	void deleteById(Long id);

	Iterable<Projet> findAll();

	Projet findByNom(String nom);
	
}
