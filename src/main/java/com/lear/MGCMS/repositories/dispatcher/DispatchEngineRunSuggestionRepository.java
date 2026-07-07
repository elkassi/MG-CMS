package com.lear.MGCMS.repositories.dispatcher;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.dispatcher.DispatchEngineRunSuggestion;

/**
 * Plain CRUD over the {@code dispatch_engine_run_suggestion} table (Flyway V13_07).
 */
public interface DispatchEngineRunSuggestionRepository
        extends JpaRepository<DispatchEngineRunSuggestion, DispatchEngineRunSuggestion.Pk> {

    @Query("SELECT s FROM DispatchEngineRunSuggestion s WHERE s.id.runId = :runId")
    List<DispatchEngineRunSuggestion> findByRunId(@Param("runId") Long runId);
}
