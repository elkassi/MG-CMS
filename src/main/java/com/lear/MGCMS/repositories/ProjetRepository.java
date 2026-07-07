package com.lear.MGCMS.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.Projet;
import com.lear.MGCMS.domain.Projet;

public interface ProjetRepository extends CrudRepository<Projet, String> {
	
	@Query("SELECT t from Projet t where t.nom like :nom and (:of is not null)")
	Page<Projet> findByFilter(String nom, PageRequest of);
	@Query("SELECT t from Projet t where t.nom = :nom")
	Projet findByObjId(String nom);

}
