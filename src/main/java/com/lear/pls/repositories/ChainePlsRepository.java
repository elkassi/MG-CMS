package com.lear.pls.repositories;

import com.lear.pls.domain.Chaine;
import com.lear.pls.domain.Chaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ChainePlsRepository extends JpaRepository<Chaine, Long>, JpaSpecificationExecutor<Chaine> {

	Chaine getChainePlsById(Long id);

	Chaine findByNom(String nom);
}
