package com.lear.MGCMS.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.Zone;

import java.util.List;

public interface ZoneRepository extends CrudRepository<Zone, String> {
	
	@Query("SELECT t from Zone t where t.nom like :nom and (:of is not null)")
	Page<Zone> findByFilter(String nom, PageRequest of);
	@Query("SELECT t from Zone t where t.nom = :nom")
	Zone findByObjId(String nom);
	@Query("SELECT t from Zone t where t.code = :code")
	Zone findByCode(String code);
	@Query("SELECT t from Zone t")
    List<Zone> findList();

	// ===== Dispatcher (Phase 3+) =====

	@Query("SELECT t FROM Zone t WHERE t.isActive = true")
	List<Zone> findAllActive();

	@Query("SELECT t FROM Zone t WHERE t.isActive = true AND t.category = :category")
	List<Zone> findAllActiveByCategory(Zone.Category category);
}
