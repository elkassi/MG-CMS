package com.lear.MGCMS.repositories.dispatcher;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.AdmissionBlockedAudit;

/**
 * Plain CRUD over {@code admission_blocked_audit}. Phase 11 schedules a
 * cron that calls {@link #deleteOlderThan} for 7-day retention.
 */
public interface AdmissionBlockedAuditRepository extends JpaRepository<AdmissionBlockedAudit, Long> {

    @Query("SELECT a FROM AdmissionBlockedAudit a WHERE a.createdAt >= :cutoff ORDER BY a.createdAt DESC")
    List<AdmissionBlockedAudit> findSince(LocalDateTime cutoff);

    @Query("SELECT a FROM AdmissionBlockedAudit a WHERE a.serieId = :serieId ORDER BY a.createdAt DESC")
    List<AdmissionBlockedAudit> findBySerieIdOrderByCreatedAtDesc(String serieId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AdmissionBlockedAudit a WHERE a.createdAt < :cutoff")
    int deleteOlderThan(LocalDateTime cutoff);
}
