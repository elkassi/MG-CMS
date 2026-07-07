package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CncMachineReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CncMachineReportRepository extends JpaRepository<CncMachineReport, Long>, JpaSpecificationExecutor<CncMachineReport> {

    Optional<CncMachineReport> findBySourceSessionId(Long sourceSessionId);
}
