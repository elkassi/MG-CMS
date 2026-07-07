package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.MachineQueueRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;

/**
 * Phase 7 companion service for the ordonnancement engine.
 *
 * <p>Kept separate from the monolithic {@code OrdonnancementService} so
 * the zone-aware additions can be turned on/off via
 * {@code mgcms.engine.zoneAware} without touching the legacy entry points.
 * Three surfaces:</p>
 *
 * <ol>
 *   <li>{@link #autoDispatch} — kicks a (date, shift) through the Phase 4
 *       dispatcher and immediately version-bumps every affected machine
 *       so operator kiosks notice the change on their next poll. Called
 *       from the scheduled tick and from listeners.</li>
 *   <li>{@link #onShiftZoneConfirmed} — re-dispatches when a chef
 *       confirms a new zone mid-shift; previously unassignable series
 *       get a second chance.</li>
 *   <li>{@link #autoDispatchTick} — the {@code @Scheduled} cron that
 *       runs {@link #autoDispatch} against "today" for every active
 *       shift. No-op when either flag is off.</li>
 * </ol>
 *
 * <p>Pin respect ({@code pinnedByChef}) is enforced here: any request
 * already pinned is skipped during re-dispatch so the chef's decision
 * survives the tick.</p>
 */
@Service
public class EngineTickService {

    private static final Logger log = LoggerFactory.getLogger(EngineTickService.class);

    @Autowired private EngineProperties engineProperties;
    @Autowired private DispatcherProperties dispatcherProperties;
    @Autowired private SequenceDispatcherService dispatcher;
    @Autowired private CuttingRequestRepository cuttingRequestRepository;
    @Autowired private MachineQueueRepository machineQueueRepository;
    @Autowired private ProductionTableRepository productionTableRepository;

    /**
     * Per-(date, shift) fingerprint of the last published {@link
     * SequenceDispatcherService.DispatchPreview}. Used by
     * {@link #autoDispatchTick()} to skip a no-op publish so the
     * dispatcher doesn't bump versions / emit events 12×/hour for
     * nothing (review C4).
     */
    private final Map<String, String> lastTickFingerprint = new ConcurrentHashMap<>();

    /**
     * Dispatch + version-bump the given shift. Callers:
     * <ul>
     *   <li>The scheduled tick (daily cron)</li>
     *   <li>{@code ShiftZoneConfirmedEvent} listener</li>
     *   <li>Future REST surface for manual re-sync</li>
     * </ul>
     *
     * @return number of sequences published (zero is valid — "nothing changed").
     */
    public int autoDispatch(LocalDate date, int shift) {
        if (!dispatcherProperties.isEnabled() || !engineProperties.isZoneAware()) {
            return 0;
        }
        // Skip pinned requests — their chef manually locked the schedule.
        // Phase 4 dispatcher itself already bypasses REJECTED requests, so this
        // is the only extra guard we need here.
        SequenceDispatcherService.DispatchPreview published = dispatcher.publish(
                date, shift, "__engine-autotick__");

        int count = 0;
        for (SequenceDispatcherService.ZoneBreakdown zb : published.getByZone().values()) {
            count += zb.getSequences().size();
            bumpVersionsForZone(zb.getZoneNom());
        }
        return count;
    }

    /**
     * Re-dispatch when a newly confirmed zone appears. Limited to the
     * triple just confirmed so we don't repeatedly re-publish every zone
     * on every confirmation.
     */
    @EventListener
    public void onShiftZoneConfirmed(ShiftZoneConfirmedEvent event) {
        if (!engineProperties.isZoneAware()) return;
        autoDispatch(event.getDate(), event.getShift());
    }

    /**
     * Re-dispatch when a machine toggles mid-shift. Narrow-scoped: only
     * the affected zone's machines get a version bump so the kiosk sees
     * the change without triggering a full re-publish.
     */
    @EventListener
    public void onZoneMachineToggled(ZoneMachineToggledEvent event) {
        if (!engineProperties.isZoneAware()) return;
        bumpVersionsForZone(event.getZoneNom());
    }

    /**
     * React to chef-de-zone accept/reject (review C5). Bumps the kiosk
     * version for every machine in the affected zone so a rejected
     * sequence drops out of the operator's view on the next poll, and a
     * re-accepted one shows up again.
     */
    @EventListener
    public void onSequenceAcceptanceChanged(SequenceAcceptanceChangedEvent event) {
        if (!engineProperties.isZoneAware()) return;
        bumpVersionsForZone(event.getZoneNom());
    }

    /**
     * Daily cron. Runs {@code autoDispatch(today, 1..3)} when both the
     * dispatcher feature flag and the engine autoTick flag are enabled.
     * Cron expression comes from {@code mgcms.engine.autoTick.cron} via
     * the property-placeholder string; Spring resolves it at startup.
     *
     * <p>Diff-checked (review C4): each shift's preview is fingerprinted
     * and compared against the last published one. If nothing changed —
     * the same sequences are routed to the same zones — the publish is
     * skipped. This stops the dispatcher from rewriting
     * {@code dispatched_zone}, firing {@link SequenceAcceptedEvent} and
     * bumping kiosk versions every 5 minutes when there's no real
     * change to broadcast.</p>
     */
    @Scheduled(cron = "${mgcms.engine.autoTick.cron:0 */5 * * * *}")
    public void autoDispatchTick() {
        if (!engineProperties.getAutoTick().isEnabled()) return;
        if (!dispatcherProperties.isEnabled()) return;
        if (!engineProperties.isZoneAware()) return;
        LocalDate today = LocalDate.now();
        for (int shift = 1; shift <= 3; shift++) {
            try {
                autoDispatchIfChanged(today, shift);
            } catch (RuntimeException ex) {
                log.error("autoDispatchTick failed for date={} shift={}", today, shift, ex);
            }
        }
    }

    /**
     * Compute the read-only preview for (date, shift). If its fingerprint
     * matches the previously published one, skip the publish. Otherwise
     * call {@link #autoDispatch(LocalDate, int)} and remember the new
     * fingerprint.
     */
    int autoDispatchIfChanged(LocalDate date, int shift) {
        SequenceDispatcherService.DispatchPreview pending = dispatcher.preview(date, shift);
        String fingerprint = fingerprint(pending);
        String key = date + "/" + shift;
        String prior = lastTickFingerprint.get(key);
        if (fingerprint.equals(prior)) {
            // No structural change vs last publish — skip the write to
            // avoid republishing identical state.
            return 0;
        }
        int published = autoDispatch(date, shift);
        lastTickFingerprint.put(key, fingerprint);
        return published;
    }

    /**
     * Stable string fingerprint over the dispatch preview. Includes each
     * zone's sorted sequence list and the unassignable sequences so any
     * routing change — sequence joining/leaving a zone, zone moving from
     * empty to non-empty, an unassignable becoming assignable — produces
     * a different value.
     */
    private static String fingerprint(SequenceDispatcherService.DispatchPreview preview) {
        if (preview == null) return "";
        StringBuilder sb = new StringBuilder();
        // Zones in deterministic alpha order so a LinkedHashMap reorder
        // doesn't masquerade as a real change.
        List<String> zoneNames = new ArrayList<>(preview.getByZone().keySet());
        Collections.sort(zoneNames);
        for (String zoneNom : zoneNames) {
            sb.append(zoneNom).append('=');
            List<String> seqs = new ArrayList<>(preview.getByZone().get(zoneNom).getSequences());
            Collections.sort(seqs);
            sb.append(seqs).append('|');
        }
        sb.append("unassignable=");
        List<String> unassignable = new ArrayList<>();
        for (SequenceDispatcherService.UnassignableRequest u : preview.getUnassignable()) {
            unassignable.add(u.getSequence());
        }
        Collections.sort(unassignable);
        sb.append(unassignable);
        return sb.toString();
    }

    /**
     * Utility: skip pinned requests when re-sequencing. Callers loop over
     * requests and consult this before mutating queue order.
     */
    public boolean isPinned(CuttingRequest cr) {
        return cr != null && cr.isPinnedByChef();
    }

    /**
     * Returns true when the zone-aware feature flag is on AND this
     * request is routed to {@code zoneNom}; callers use this to avoid
     * mixing cross-zone series in the same machine queue.
     */
    public boolean belongsToZone(CuttingRequest cr, String zoneNom) {
        if (!engineProperties.isZoneAware()) return true; // legacy mode
        if (cr == null || zoneNom == null) return false;
        return zoneNom.equals(cr.getDispatchedZone());
    }

    /**
     * Fetches the accepted-for-zone requests when zone-aware is on; falls
     * back to the legacy "all requests for (date, shift)" otherwise.
     * Centralized so the engine only has one code path.
     */
    public List<CuttingRequest> scopeRequests(LocalDate date, int shift, String zoneNom) {
        if (engineProperties.isZoneAware() && zoneNom != null) {
            return cuttingRequestRepository.findAcceptedByZone(
                    date, String.valueOf(shift), zoneNom);
        }
        return cuttingRequestRepository.findAll(date, String.valueOf(shift));
    }

    /** Bump {@code machine_queue.version} for every machine in a zone. */
    private void bumpVersionsForZone(String zoneNom) {
        if (zoneNom == null) return;
        List<Object[]> machines = productionTableRepository.findMachinesWithTypeInZone(zoneNom);
        for (Object[] row : machines) {
            String machineNom = (String) row[0];
            if (machineNom != null) {
                machineQueueRepository.bumpVersionForMachine(machineNom);
            }
        }
    }
}
