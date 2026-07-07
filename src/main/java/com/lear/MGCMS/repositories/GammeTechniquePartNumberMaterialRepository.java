package com.lear.MGCMS.repositories;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.GammeTechniquePartNumberMaterial;
import com.lear.MGCMS.domain.GammeTechniquePartNumberMaterialId;

public interface GammeTechniquePartNumberMaterialRepository extends CrudRepository<GammeTechniquePartNumberMaterial, GammeTechniquePartNumberMaterialId> {

	@Modifying
	@Transactional
	@Query("delete from GammeTechniquePartNumberMaterial where gammeTechnique.partNumber = :partNumber")
	void deleteByGammeTechniquePartNumber(String partNumber);
	
}
