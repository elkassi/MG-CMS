package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CncProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CncProductionRepository extends JpaRepository<CncProduction, Long> {

    List<CncProduction> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    List<CncProduction> findBySessionIdAndStatus(Long sessionId, String status);
}
