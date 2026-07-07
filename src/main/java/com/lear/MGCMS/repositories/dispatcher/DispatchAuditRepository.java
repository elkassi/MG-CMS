package com.lear.MGCMS.repositories.dispatcher;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.DispatchAudit;

/**
 * Plain CRUD over the {@code dispatch_audit} table (Flyway V12_01).
 * Read by the dispatcher audit panel (most-recent-first), pruned by
 * {@code RetentionCronService}.
 */
public interface DispatchAuditRepository extends JpaRepository<DispatchAudit, Long> {

    /** Audit trail for a single sequence — used by the row drill-down. */
    @Query("SELECT a FROM DispatchAudit a "
         + "WHERE a.sequence = :sequence "
         + "ORDER BY a.createdAt DESC")
    List<DispatchAudit> findBySequenceOrderByCreatedAtDesc(String sequence);

    /** Latest N rows across all sequences — feeds the audit panel. */
    @Query("SELECT a FROM DispatchAudit a ORDER BY a.createdAt DESC")
    List<DispatchAudit> findLatest(Pageable page);

    /** Rolling-window read since {@code cutoff}. */
    @Query("SELECT a FROM DispatchAudit a "
         + "WHERE a.createdAt >= :cutoff "
         + "ORDER BY a.createdAt DESC")
    List<DispatchAudit> findSince(LocalDateTime cutoff);

    /** Retention prune — RetentionCronService schedules this. */
    @Modifying
    @Transactional
    @Query("DELETE FROM DispatchAudit a WHERE a.createdAt < :cutoff")
    int deleteOlderThan(LocalDateTime cutoff);
}
