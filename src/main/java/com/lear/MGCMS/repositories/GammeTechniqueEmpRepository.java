package com.lear.MGCMS.repositories;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.GammeTechniqueEmp;
import com.lear.MGCMS.domain.GammeTechniqueEmpId;

public interface GammeTechniqueEmpRepository extends CrudRepository<GammeTechniqueEmp, GammeTechniqueEmpId> {

	@Modifying
	@Transactional
	@Query("delete from GammeTechniqueEmp where gammeTechnique.partNumber = :partNumber")
	void deleteByGammeTechniquePartNumber(String partNumber);
	
}
