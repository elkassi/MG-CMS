package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.AuditQualiteConfig;

public interface AuditQualiteConfigRepository extends JpaRepository<AuditQualiteConfig, Long>, JpaSpecificationExecutor<AuditQualiteConfig> {

	@Query("from AuditQualiteConfig")
	List<AuditQualiteConfig> findList();
	@Query("from AuditQualiteConfig where id = :id")
	AuditQualiteConfig findObjById(Long id);

}