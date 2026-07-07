package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CncMachineReportPiece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CncMachineReportPieceRepository extends JpaRepository<CncMachineReportPiece, Long>, JpaSpecificationExecutor<CncMachineReportPiece> {

    List<CncMachineReportPiece> findByReportId(Long reportId);
}
