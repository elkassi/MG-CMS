package com.lear.MGCMS.repositories;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.GammeTechnique;

public interface GammeTechniqueRepository extends JpaRepository<GammeTechnique, String>, JpaSpecificationExecutor<GammeTechnique> {

	GammeTechnique findByPartNumber(String partNumber);

	@Modifying
	@Transactional
	@Query("delete from GammeTechnique where partNumber = :partNumber")
	void deleteByPartNumber(String partNumber);

}
