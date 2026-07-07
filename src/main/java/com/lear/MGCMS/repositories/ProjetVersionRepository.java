package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.ProjetVersion;
import com.lear.MGCMS.domain.ProjetVersion;

public interface ProjetVersionRepository extends CrudRepository<ProjetVersion, Long> {

	@Query("SELECT t from ProjetVersion t where t.projet.nom like :projet and (:of is not null)")
	Page<ProjetVersion> findByFilter(String projet, PageRequest of);
	@Query("SELECT t from ProjetVersion t where t.id = :id")
	ProjetVersion findByObjId(Long id);
	@Query("SELECT t from ProjetVersion t where t.projet.nom = :projet")
	List<ProjetVersion> findByProjet(String projet);

	@Query("SELECT t.version from ProjetVersion t where t.projet.nom = :nom")
    List<String> findVersionByProjetNom(String nom);
}
