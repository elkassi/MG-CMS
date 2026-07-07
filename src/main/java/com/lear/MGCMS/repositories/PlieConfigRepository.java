package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.PlieConfig;
import org.springframework.data.jpa.repository.Query;


public interface PlieConfigRepository extends JpaRepository<PlieConfig, Long>, JpaSpecificationExecutor<PlieConfig>  {

	PlieConfig getPlieConfigById(Long id);
	@Query("from PlieConfig p where p.projet.nom = ?1 or p.projet is null")
	List<PlieConfig> findByProjetNom(String projet);
    List<PlieConfig> findByPartNumberMaterial(String partNumberMaterial);
}
