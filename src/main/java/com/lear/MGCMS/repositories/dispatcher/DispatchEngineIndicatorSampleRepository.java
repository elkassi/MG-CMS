package com.lear.MGCMS.repositories.dispatcher;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.dispatcher.DispatchEngineIndicatorSample;

/**
 * Plain CRUD over the {@code dispatch_engine_indicator_sample} table (Flyway V13_07).
 */
public interface DispatchEngineIndicatorSampleRepository
        extends JpaRepository<DispatchEngineIndicatorSample, DispatchEngineIndicatorSample.Pk> {

    @Query("SELECT s FROM DispatchEngineIndicatorSample s WHERE s.id.runId = :runId ORDER BY s.id.iteration ASC")
    List<DispatchEngineIndicatorSample> findByRunId(@Param("runId") Long runId);
}
