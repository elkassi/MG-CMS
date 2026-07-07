package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CncPsSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CncPsSessionRepository extends JpaRepository<CncPsSession, Long>, JpaSpecificationExecutor<CncPsSession> {

    List<CncPsSession> findByOperatorOrderByCreatedAtDesc(String operator);

    List<CncPsSession> findByOperatorAndCreatedAtAfterOrderByCreatedAtDesc(String operator, LocalDateTime since);

    List<CncPsSession> findAllByOrderByCreatedAtDesc();

    List<CncPsSession> findByBoxId(String boxId);

    List<CncPsSession> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    List<CncPsSession> findByCompletedAndCreatedAtBetweenOrderByCreatedAtDesc(Boolean completed, LocalDateTime start, LocalDateTime end);
}
