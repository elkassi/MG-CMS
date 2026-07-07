package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.DrillEmp;

public interface DrillEmpRepository extends JpaRepository<DrillEmp, Long>, JpaSpecificationExecutor<DrillEmp> {

	DrillEmp findDrillEmpByPattern(String pattern);

	void deleteByPattern(String id);
	
	@Query("from DrillEmp where pattern in (:patterns)")
	List<DrillEmp> getList(List<String> patterns);

}
