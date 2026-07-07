package com.lear.cms.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.GammeTechnique;
import com.lear.cms.domain.GammeTechniqueImprimer;

import java.util.List;

public interface GammeTechniqueCMSRepository extends CrudRepository<GammeTechnique, Long> {
	
	GammeTechnique findFirstByPartNumber(String partNumber);
	@Query("from GammeTechnique where partNumber in (:partNumbers)")
	List<GammeTechnique> getByPartNumber(List<String> partNumbers);
}
