package com.lear.MGCMS.repositories;

import java.util.List;

import javax.transaction.Transactional;

import com.lear.MGCMS.domain.PartNumberMaterialConfigAll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.PartNumberMaterialConfig;

public interface PartNumberMaterialConfigRepository extends CrudRepository<PartNumberMaterialConfig, String>{

	@Query("SELECT t from PartNumberMaterialConfigAll t where t.partNumberMaterial like :partNumberMaterial and :of is not null")
	Page<PartNumberMaterialConfigAll> findAll(String partNumberMaterial, PageRequest of);
	@Query("SELECT t from PartNumberMaterialConfig t where t.partNumberMaterial = :partNumberMaterial")
	PartNumberMaterialConfig findByObjId(String partNumberMaterial);
	@Query("SELECT t from PartNumberMaterialConfigAll t where t.partNumberMaterial in :pns")
	List<PartNumberMaterialConfigAll> findByPns(List<String> pns);
	
	@Modifying
	@Transactional
	@Query(value = "DELETE FROM [dbo].[ReftissuCategory] WHERE partNumberMaterialConfig_partNumberMaterial = :partNumberMaterial " + 
			"DELETE FROM [dbo].[ReftissuMachine] WHERE partNumberMaterialConfig_partNumberMaterial = :partNumberMaterial " + 
			"DELETE FROM [dbo].[ReftissuMargin] WHERE partNumberMaterialConfig_partNumberMaterial = :partNumberMaterial ", nativeQuery = true)
	void deleteByPartNumberMaterial(String partNumberMaterial);
	
}
