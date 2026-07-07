package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.CoupeMachineHistory;
import com.lear.MGCMS.domain.CoupeMachineHistoryId;
import com.lear.MGCMS.domain.FirstCheckConfig;

public interface FirstCheckConfigRepository extends JpaRepository<FirstCheckConfig, Long>, JpaSpecificationExecutor<FirstCheckConfig> {

	@Query("from FirstCheckConfig where category = :category")
	List<FirstCheckConfig> findList(String category);
	@Query("from FirstCheckConfig where id = :id")
	FirstCheckConfig findObjById(Long id);

}
