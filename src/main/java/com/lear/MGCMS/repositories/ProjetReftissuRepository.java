package com.lear.MGCMS.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.ProjetReftissu;
import com.lear.MGCMS.domain.ProjetReftissu;

public interface ProjetReftissuRepository extends CrudRepository<ProjetReftissu, Long> {
	
	@Query("SELECT t from ProjetReftissu t where (:projet is null or t.projet.nom like :projet) and (:of is not null)")
	Page<ProjetReftissu> findByFilter(String projet, PageRequest of);
	@Query("SELECT t from ProjetReftissu t where t.id = :id")
	ProjetReftissu findByObjId(Long id);

}
