package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.repositories.dispatcher.AdmissionBlockedAuditRepository;
import com.lear.MGCMS.repositories.dispatcher.UnassignableSerieRepository;

/**
 * Phase 11 — nightly retention sweep for the two dispatcher audit tables.
 *
 * <p>Deletes rows in {@code unassignable_serie} and
 * {@code admission_blocked_audit} whose {@code created_at} is older than
 * {@code mgcms.retention.days} (default 7). The job is gated by
 * {@code mgcms.retention.enabled} — off by default so it never fires
 * silently in an environment that hasn't confirmed both tables exist.</p>
 *
 * <p>The cron expression defaults to {@code 0 30 2 * * *} (02:30 every
 * night). Pick an off-hours slot to avoid contending with the engine
 * tick (Phase 7) which runs on its own cadence.</p>
 */
@Service
public class RetentionCronService {

    private static final Logger log = LoggerFactory.getLogger(RetentionCronService.class);

    @Autowired private RetentionProperties properties;
    @Autowired private UnassignableSerieRepository unassignableRepository;
    @Autowired private AdmissionBlockedAuditRepository admissionAuditRepository;

    /**
     * Cron entry point — runs once per day at the configured time. No-op
     * when the master flag is off or {@code days <= 0}.
     */
    @Scheduled(cron = "${mgcms.retention.cron:0 30 2 * * *}")
    public void purgeAudits() {
        if (!properties.isEnabled()) return;
        int days = properties.getDays();
        if (days <= 0) {
            log.warn("RetentionCronService: enabled but days={} — skipping (misconfig?)", days);
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        try {
            int unassignable = unassignableRepository.deleteOlderThan(cutoff);
            int admission    = admissionAuditRepository.deleteOlderThan(cutoff);
            log.info("RetentionCronService: pruned unassignable={} admissionAudit={} (cutoff={})",
                     unassignable, admission, cutoff);
        } catch (RuntimeException ex) {
            // Don't let a bad sweep kill the scheduler thread — next night's
            // run should get another shot.
            log.error("RetentionCronService failed mid-sweep", ex);
        }
    }

    /** Exposed for unit tests + manual ops invocation. */
    public int purgeNow(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(1, days));
        int a = unassignableRepository.deleteOlderThan(cutoff);
        int b = admissionAuditRepository.deleteOlderThan(cutoff);
        return a + b;
    }
}
