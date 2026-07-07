package com.lear.MGCMS.services.logistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.SequenceStatus;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository;
import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.dispatcher.WorkbenchCacheService;
import com.lear.cms.repositories.SuiviPlanningRepository;

/**
 * Reconciles {@code CuttingRequest.sequenceStatus} from the legacy
 * {@code suiviplanning.Statu} every 20 minutes. {@code suiviplanning} (cms
 * datasource) is the cross-app source of truth — the external CMS desktop app,
 * scanCoupe and CMS-Prod all write it — so this job mirrors its four lifecycle
 * statuses back onto the primary-datasource CuttingRequest:
 *
 * <pre>
 *   Non demarre -&gt; IMPORTED
 *   Released    -&gt; RELEASED
 *   En cours    -&gt; STARTED
 *   Complet     -&gt; COMPLETED
 * </pre>
 *
 * <p>The mirror is allowed to move a sequence backward (if suiviplanning
 * regresses) but never overwrites the two MG-CMS-internal exception states
 * {@code MATERIAL_MISSING} / {@code INCOMPLETE}, which suiviplanning has no
 * representation for — that guard lives in
 * {@link CuttingRequestDataRepository#reconcileSequenceStatus}. Every sequence
 * present in {@code suiviplanning} is reconciled (it is the rolling planning set,
 * a few hundred sequences — not the full completed history), so a status change
 * is mirrored regardless of how old the sequence's {@code date_suivi} is.</p>
 */
@Service
public class SuiviPlanningStatusSyncService {

    private static final Logger log = LoggerFactory.getLogger(SuiviPlanningStatusSyncService.class);

    /** Max sequences per bulk UPDATE — keeps the IN (...) clause under SQL Server's 2100-parameter cap. */
    private static final int BATCH_SIZE = 1000;

    @Value("${mgcms.suiviSync.enabled:true}")
    private boolean enabled;

    @Autowired private SuiviPlanningRepository suiviPlanningRepository;
    @Autowired private CuttingRequestDataRepository requestDataRepository;
    @Autowired(required = false) private WorkbenchCacheService workbenchCacheService;
    @Autowired(required = false) private OrdonnancementService ordonnancementService;

    /**
     * Scheduled entry point — runs every 20 minutes (after a 2-minute initial
     * delay so it never fires mid-startup). No-op when the master flag is off.
     */
    @Scheduled(fixedRate = 1000L * 60 * 20, initialDelay = 1000L * 60 * 2)
    public void sync() {
        if (!enabled) {
            return;
        }
        try {
            int changed = reconcile();
            if (changed > 0) {
                log.info("SuiviPlanningStatusSync: updated {} CuttingRequest sequenceStatus row(s)", changed);
                invalidateDerivedCaches();
            }
        } catch (RuntimeException ex) {
            // Never let a bad run kill the scheduler thread; the next tick retries.
            log.error("SuiviPlanningStatusSync failed", ex);
        }
    }

    /**
     * One reconciliation pass. Reads every (sequence, Statu) pair from
     * suiviplanning, maps each to a CuttingRequest status, de-dupes to one target
     * per sequence (furthest-along when a sequence's rows disagree), then issues
     * guarded bulk updates per target (chunked to stay under the parameter cap).
     *
     * @return number of CuttingRequest rows updated. Package-visible for tests and
     *         a manual ops trigger.
     */
    int reconcile() {
        List<Object[]> rows = suiviPlanningRepository.findAllStatu();
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        // One target status per sequence — keep the furthest-along when a sequence
        // has rows in different states (it is STARTED if any of its matelas started).
        Map<String, String> targetBySequence = new HashMap<>();
        for (Object[] row : rows) {
            String sequence = (String) row[0];
            String target = mapStatu((String) row[1]);
            if (sequence == null || target == null) {
                continue;
            }
            String current = targetBySequence.get(sequence);
            if (current == null || rank(target) > rank(current)) {
                targetBySequence.put(sequence, target);
            }
        }
        if (targetBySequence.isEmpty()) {
            return 0;
        }

        // Group sequences by target, then one guarded bulk update per target.
        Map<String, List<String>> sequencesByTarget = new HashMap<>();
        for (Map.Entry<String, String> entry : targetBySequence.entrySet()) {
            sequencesByTarget.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        int changed = 0;
        for (Map.Entry<String, List<String>> entry : sequencesByTarget.entrySet()) {
            String target = entry.getKey();
            List<String> seqs = entry.getValue();
            for (int i = 0; i < seqs.size(); i += BATCH_SIZE) {
                List<String> chunk = seqs.subList(i, Math.min(i + BATCH_SIZE, seqs.size()));
                changed += requestDataRepository.reconcileSequenceStatus(target, chunk);
            }
        }
        return changed;
    }

    /** suiviplanning Statu -&gt; CuttingRequest sequenceStatus. Unmapped values are skipped. */
    private static String mapStatu(String statu) {
        if (statu == null) {
            return null;
        }
        switch (statu.trim().toLowerCase()) {
            case "non demarre": return SequenceStatus.IMPORTED;
            case "released":    return SequenceStatus.RELEASED;
            case "en cours":    return SequenceStatus.STARTED;
            case "complet":     return SequenceStatus.COMPLETED;
            default:            return null;
        }
    }

    /** Lifecycle order used only to pick the furthest-along status of a sequence. */
    private static int rank(String target) {
        if (SequenceStatus.IMPORTED.equals(target))  return 0;
        if (SequenceStatus.RELEASED.equals(target))  return 1;
        if (SequenceStatus.STARTED.equals(target))   return 2;
        if (SequenceStatus.COMPLETED.equals(target)) return 3;
        return -1;
    }

    /**
     * Mirror of {@code SequenceStatusService.afterProductionDataChange()} — refresh
     * the derived views that key off sequenceStatus. Failures are swallowed; the
     * views rebuild on their own next poll.
     */
    private void invalidateDerivedCaches() {
        try {
            if (ordonnancementService != null) {
                ordonnancementService.invalidateTimelineCache();
            }
            if (workbenchCacheService != null) {
                workbenchCacheService.invalidateAll();
            }
        } catch (Exception ignored) {
            // Derived views refresh on the next poll if this eager refresh fails.
        }
    }
}
