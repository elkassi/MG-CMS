package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;

/**
 * Phase 11 — stub self-heal sweep.
 *
 * <p>Scans for {@link CuttingRequest} rows whose
 * {@code zoneAcceptanceStatus=PENDING} older than
 * {@link SelfHealProperties#getStuckPendingMinutes()}, and
 * <em>logs a warning</em>. Non-destructive on purpose — this first
 * iteration exists so ops can see the stuck count trend on a dashboard
 * before anyone asks the scheduler to auto-rewrite state.</p>
 *
 * <p>Future iterations may:
 * <ul>
 *   <li>Re-run the dispatcher for stuck rows to move them out of limbo.</li>
 *   <li>Detect {@link CuttingRequest}s with no {@code scheduledDate} and
 *       hand them back to the engine.</li>
 *   <li>Detect MachineQueue heads whose {@code assignedAt} is older than
 *       a shift and flag the machine as stuck in the kiosk banner.</li>
 * </ul>
 * Each of these will land behind its own sub-flag under
 * {@code mgcms.engine.selfHeal.*}.</p>
 */
@Service
public class SelfHealService {

    private static final Logger log = LoggerFactory.getLogger(SelfHealService.class);

    @Autowired private SelfHealProperties properties;
    @Autowired private CuttingRequestRepository cuttingRequestRepository;

    /**
     * Cron-driven entry point. No-op when the flag is off. Safe to call by
     * hand from tests via {@link #sweep()}.
     */
    @Scheduled(cron = "${mgcms.engine.selfHeal.cron:0 */10 6-22 * * *}")
    public void scheduledSweep() {
        if (!properties.isEnabled()) return;
        try {
            sweep();
        } catch (RuntimeException ex) {
            log.error("SelfHealService sweep failed", ex);
        }
    }

    /** Visible for tests + ops triggering a sweep on demand. Returns stuck count. */
    public int sweep() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(properties.getStuckPendingMinutes());
        // The status + cutoff filter is pushed into the DB query so we never
        // load the whole cutting_request table into memory (review C2). The
        // age signal is now {@code dispatchedAt} (the timestamp stamped on
        // publish/force/rebalance), not {@code createdAt}.
        List<CuttingRequest> stuckRows = cuttingRequestRepository.findStuckPending(cutoff);
        for (CuttingRequest cr : stuckRows) {
            log.warn("SelfHeal: sequence={} dispatchedZone={} stuck PENDING since {}",
                     cr.getSequence(), cr.getDispatchedZone(), cr.getDispatchedAt());
        }
        int stuck = stuckRows.size();
        if (stuck > 0) {
            log.warn("SelfHeal: {} CuttingRequest(s) stuck in PENDING > {} min",
                     stuck, properties.getStuckPendingMinutes());
        }
        return stuck;
    }
}
