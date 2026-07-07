package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.Machine;

public interface MachineRepository extends CrudRepository<Machine, String> {

	@Query("SELECT t from Machine t where t.code like :code and (:of is not null)")
	Page<Machine> findByFilter(String code, PageRequest of);
	@Query("SELECT t from Machine t where t.code = :code")
	Machine findByCode(String code);
	@Query("SELECT t from Machine t")
    List<Machine> findList();
}
