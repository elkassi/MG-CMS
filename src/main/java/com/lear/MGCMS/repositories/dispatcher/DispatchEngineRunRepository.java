package com.lear.MGCMS.repositories.dispatcher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lear.MGCMS.domain.dispatcher.DispatchEngineRun;

/**
 * Plain CRUD over the {@code dispatch_engine_run} table (Flyway V13_07).
 */
public interface DispatchEngineRunRepository extends JpaRepository<DispatchEngineRun, Long> {

    Page<DispatchEngineRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
