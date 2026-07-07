package com.lear.MGCMS.repositories.dispatcher;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.UnassignableSerie;

/**
 * Plain CRUD over the {@code unassignable_serie} audit table. Phase 11
 * schedules a cron that calls {@link #deleteOlderThan} for 7-day retention.
 */
public interface UnassignableSerieRepository extends JpaRepository<UnassignableSerie, Long> {

    /** Most-recent-first audit trail for a specific serie. */
    @Query("SELECT u FROM UnassignableSerie u "
         + "WHERE u.serieId = :serieId "
         + "ORDER BY u.createdAt DESC")
    List<UnassignableSerie> findBySerieIdOrderByCreatedAtDesc(String serieId);

    /** All rows created after a cutoff — surfaced on the Process page. */
    @Query("SELECT u FROM UnassignableSerie u "
         + "WHERE u.createdAt >= :cutoff "
         + "ORDER BY u.createdAt DESC")
    List<UnassignableSerie> findSince(LocalDateTime cutoff);

    /** Retention prune — Phase 11 runs this on a 7-day cron. */
    @Modifying
    @Transactional
    @Query("DELETE FROM UnassignableSerie u WHERE u.createdAt < :cutoff")
    int deleteOlderThan(LocalDateTime cutoff);
}
