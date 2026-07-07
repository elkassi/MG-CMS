package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.GammeTechniqueImprimer;
import com.lear.cms.domain.ProduitFinit;
import org.springframework.data.repository.query.Param;

public interface ProduitFinitRepository extends CrudRepository<ProduitFinit, Long> {
	
	@Query("from ProduitFinit where nSequence like :sequence")
	List<ProduitFinit> findBySequencelike(String sequence);
	
	@Query("from ProduitFinit where nSequence = :sequence")
	List<ProduitFinit> findBySequence(String sequence);
	
	@Query("Select p.nSequence from ProduitFinit p where nSequence like :sequence group by nSequence")
	List<String> findAllSequenceLike(String sequence);

	@Query("Select cast(p.noff as long) from ProduitFinit p where nSequence = :sequence")
	List<Long> findWOBySequence(@Param("sequence") String sequence);

	@Query("Select max(p.id) from ProduitFinit p")
    Long getMaxId();
}
