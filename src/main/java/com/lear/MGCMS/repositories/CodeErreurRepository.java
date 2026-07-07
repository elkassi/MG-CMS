package com.lear.MGCMS.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CodeErreur;

public interface CodeErreurRepository extends CrudRepository<CodeErreur, Long> {

	@Query("SELECT t from CodeErreur t where t.code like :code and (:of is not null)")
	Page<CodeErreur> findByFilter(String code, PageRequest of);
	@Query("SELECT t from CodeErreur t where t.id = :id")
	CodeErreur findByObjId(Long id);
	
}
