package com.lear.pls.repositories;

import com.lear.pls.domain.Demande;
import com.lear.pls.domain.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface SitePlsRepository extends JpaRepository<Site, Long>, JpaSpecificationExecutor<Site> {

	Site getSitePlsById(Long id);

	Site findByNom(String nom);
}
