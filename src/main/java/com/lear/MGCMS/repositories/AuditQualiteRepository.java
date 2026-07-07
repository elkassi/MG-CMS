package com.lear.MGCMS.repositories;

import java.time.LocalDate;
import java.util.List;

import com.lear.MGCMS.domain.FirstCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.AuditQualite;

public interface AuditQualiteRepository  extends JpaRepository<AuditQualite, Long>, JpaSpecificationExecutor<AuditQualite> {

	@Query("from AuditQualite where date = :date and shift = :shift and tableControle = :tableControle")
	List<AuditQualite> findList(LocalDate date, String shift, String tableControle);

	@Query("from AuditQualite where date = :date and shift = :shift")
    List<AuditQualite> getList(LocalDate date, String shift);
}
