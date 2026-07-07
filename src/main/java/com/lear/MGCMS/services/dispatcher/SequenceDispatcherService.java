package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.dispatcher.DispatchAudit;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;

/**
 * Routes whole {@link CuttingRequest} sequences into zones for a given
 * (date, shift). Two entry points:
 *
 * <ul>
 *   <li>{@link #preview} — read-only; returns a per-zone breakdown and a
 *       list of unassignable series. Used by the Process page's "Dispatch"
 *       action before the chef clicks Publish.</li>
 *   <li>{@link #publish} — writes {@code dispatched_zone} +
 *       {@code zone_acceptance_status=PENDING} on every accepted
 *       {@link CuttingRequest}, and fires a {@link SequenceAcceptedEvent}
 *       per affected zone so chef-de-zone pages refresh.</li>
 * </ul>
 *
 * <p>A {@link CuttingRequest} is considered routable as soon as at least
 * one of its {@link CuttingRequestSerie} rows resolves to a zone via
 * {@link SerieZoneResolver}. The request's placement zone is the most
 * common zone across its series; ties resolve STRICT-first then
 * alphabetically. If every series in a request fails to resolve, the
 * request is reported unassignable and every failing serie gets a
 * {@code UnassignableSerie} audit row (written by
 * {@link SchedulableSerieFilter}).</p>
 *
 * <p>Gated by the {@code mgcms.dispatcher.enabled} property at the
 * controller layer — this service itself is safe to call unconditionally.</p>
 */
@Service
public class SequenceDispatcherService {

    @Autowired
    private CuttingRequestRepository cuttingRequestRepository;

    @Autowired
    private com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository serieDataRepository;

    @Autowired
    private SchedulableSerieFilter schedulableSerieFilter;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private com.lear.MGCMS.repositories.ZoneRepository zoneRepository;

    @Autowired
    private DispatchAuditService dispatchAuditService;

    @Autowired
    private com.lear.MGCMS.services.scheduling.ShiftClock shiftClock;

    @Autowired
    private com.lear.MGCMS.services.OrdonnancementService ordonnancementService;

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Read-only preview. No writes, no events.
     *
     * @return per-zone breakdowns + unassignable list.
     */
    @Transactional(readOnly = true)
    public DispatchPreview preview(LocalDate date, int shift) {
        return compute(date, shift);
    }

    /**
     * Real-time preview for all active sequences (sequenceStatus = ACTIVE or null).
     * Uses the current wall-clock date/shift for zone-resolution.
     */
    @Transactional(readOnly = true)
    public DispatchPreview previewActive() {
        return computeActive();
    }

    /**
     * Phase 10 — chef-de-zone accept/reject for a single sequence.
     * Writes {@code zone_acceptance_status} via the proper service-layer
     * transaction (review C6) and publishes a {@link
     * SequenceAcceptanceChangedEvent} so listeners (kiosk version
     * bump, engine reschedule) react to the flip (review C5).
     *
     * @param sequence            CuttingRequest sequence identifier.
     * @param status              {@code ACCEPTED} or {@code REJECTED}; case-insensitive,
     *                            normalized to upper-case before persistence.
     * @param changedByMatricule  matricule of the chef performing the change; may be null
     *                            when the controller can't resolve a user.
     * @return {@link Optional#empty()} when the sequence does not exist;
     *         the persisted {@link CuttingRequest} otherwise.
     */
    @Transactional
    public Optional<CuttingRequest> setAcceptance(String sequence, String status,
                                                  String changedByMatricule) {
        if (sequence == null) return Optional.empty();
        if (status == null) {
            throw new IllegalArgumentException("status must be ACCEPTED or REJECTED");
        }
        String normalized = status.trim().toUpperCase();
        if (!"ACCEPTED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new IllegalArgumentException("status must be ACCEPTED or REJECTED");
        }
        CuttingRequest cr = cuttingRequestRepository.findBySequence(sequence);
        if (cr == null) return Optional.empty();
        cr.setZoneAcceptanceStatus(normalized);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if ("ACCEPTED".equals(normalized)) {
            cr.setZoneAcceptedAt(now);
            cr.setZoneAcceptedBy(changedByMatricule);
            cr.setZoneRejectionReason(null);
        } else {
            cr.setZoneRejectionReason("Rejected by chef"); // TODO: pass reason from UI
        }
        cuttingRequestRepository.save(cr);
        if (ordonnancementService != null) {
            ordonnancementService.invalidateTimelineCache();
        }
        eventPublisher.publishEvent(new SequenceAcceptanceChangedEvent(
                sequence, cr.getDispatchedZone(), normalized, changedByMatricule));
        return Optional.of(cr);
    }

    /**
     * Persist the dispatcher's decision. Accepted requests get their
     * {@code dispatched_zone} + {@code zone_acceptance_status=PENDING}
     * stamped, and one {@link SequenceAcceptedEvent} per affected zone is
     * fired after commit. Each affected sequence gets a
     * {@link DispatchAudit} row with trigger {@link DispatchAudit.Trigger#PUBLISH}.
     */
    @Transactional
    public DispatchPreview publish(LocalDate date, int shift, String publishedByMatricule) {
        DispatchPreview preview = compute(date, shift);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (ZoneBreakdown zb : preview.getByZone().values()) {
            for (String sequence : zb.getSequences()) {
                CuttingRequest cr = cuttingRequestRepository.findBySequence(sequence);
                if (cr == null) continue;
                String fromZone = cr.getDispatchedZone();
                cr.setDispatchedZone(zb.getZoneNom());
                cr.setZoneAcceptanceStatus("PENDING");
                cr.setDispatchedAt(now);
                cr.setDispatchedBy(publishedByMatricule);
                cuttingRequestRepository.save(cr);
                if (!zb.getZoneNom().equalsIgnoreCase(fromZone)) {
                    dispatchAuditService.write(sequence, fromZone, zb.getZoneNom(),
                            "Publish " + date + "/" + shift,
                            DispatchAudit.Trigger.PUBLISH, publishedByMatricule);
                }
            }
            if (!zb.getSequences().isEmpty()) {
                eventPublisher.publishEvent(new SequenceAcceptedEvent(
                        zb.getZoneNom(), date, shift, publishedByMatricule,
                        new ArrayList<>(zb.getSequences())));
            }
        }
        if (ordonnancementService != null) {
            ordonnancementService.invalidateTimelineCache();
        }
        return preview;
    }

    /**
     * Process action: force a sequence to a specific STRICT zone, overriding
     * the dispatcher's recommendation. Resets {@code zoneAcceptanceStatus =
     * PENDING} so the receiving chef must re-accept; pinned sequences are
     * not affected. Writes a {@link DispatchAudit} row.
     *
     * @return persisted CuttingRequest, or empty if the sequence doesn't exist
     *         or the target zone is unknown.
     */
    @Transactional
    public Optional<CuttingRequest> force(String sequence, String zoneNom,
                                           String reason, String byMatricule) {
        if (sequence == null || zoneNom == null) return Optional.empty();
        CuttingRequest cr = cuttingRequestRepository.findBySequence(sequence);
        if (cr == null) return Optional.empty();
        Zone target = zoneRepository.findByObjId(zoneNom);
        if (target == null) return Optional.empty();

        String fromZone = cr.getDispatchedZone();
        cr.setDispatchedZone(zoneNom);
        cr.setZoneAcceptanceStatus("PENDING");
        cuttingRequestRepository.save(cr);
        if (ordonnancementService != null) {
            ordonnancementService.invalidateTimelineCache();
        }
        dispatchAuditService.write(sequence, fromZone, zoneNom,
                reason, DispatchAudit.Trigger.MANUAL, byMatricule);
        return Optional.of(cr);
    }

    /**
     * Chef pulls a SHARED-overflow sequence into one of their STRICT zones.
     * Functionally identical to {@link #force} but flagged differently in
     * the audit so we can measure how often chefs reroute their own work.
     * The caller (controller) is responsible for verifying that
     * {@code byMatricule} owns {@code zoneNom} (UserZone check).
     */
    @Transactional
    public Optional<CuttingRequest> pull(String sequence, String zoneNom,
                                          String reason, String byMatricule) {
        if (sequence == null || zoneNom == null) return Optional.empty();
        CuttingRequest cr = cuttingRequestRepository.findBySequence(sequence);
        if (cr == null) return Optional.empty();
        Zone target = zoneRepository.findByObjId(zoneNom);
        if (target == null) return Optional.empty();

        String fromZone = cr.getDispatchedZone();
        cr.setDispatchedZone(zoneNom);
        cr.setZoneAcceptanceStatus("ACCEPTED");
        cr.setPinnedByChef(true);
        cuttingRequestRepository.save(cr);
        if (ordonnancementService != null) {
            ordonnancementService.invalidateTimelineCache();
        }
        dispatchAuditService.write(sequence, fromZone, zoneNom,
                reason, DispatchAudit.Trigger.CHEF_PULL, byMatricule);
        return Optional.of(cr);
    }

    /**
     * Chef pin: mark the sequence immune to greedy re-dispatch. The chef
     * must already own the sequence's zone (verified by the controller).
     */
    @Transactional
    public Optional<CuttingRequest> pin(String sequence, String reason, String byMatricule) {
        if (sequence == null) return Optional.empty();
        CuttingRequest cr = cuttingRequestRepository.findBySequence(sequence);
        if (cr == null) return Optional.empty();
        if (cr.isPinnedByChef()) return Optional.of(cr);
        cr.setPinnedByChef(true);
        cuttingRequestRepository.save(cr);
        if (ordonnancementService != null) {
            ordonnancementService.invalidateTimelineCache();
        }
        dispatchAuditService.write(sequence, cr.getDispatchedZone(), cr.getDispatchedZone(),
                reason, DispatchAudit.Trigger.CHEF_PIN, byMatricule);
        return Optional.of(cr);
    }

    /**
     * Process / Admin removes a chef pin. Greedy re-dispatch may then move
     * the sequence on the next tick.
     */
    @Transactional
    public Optional<CuttingRequest> unpin(String sequence, String reason, String byMatricule) {
        if (sequence == null) return Optional.empty();
        CuttingRequest cr = cuttingRequestRepository.findBySequence(sequence);
        if (cr == null) return Optional.empty();
        if (!cr.isPinnedByChef()) return Optional.of(cr);
        cr.setPinnedByChef(false);
        cuttingRequestRepository.save(cr);
        if (ordonnancementService != null) {
            ordonnancementService.invalidateTimelineCache();
        }
        dispatchAuditService.write(sequence, cr.getDispatchedZone(), cr.getDispatchedZone(),
                reason, DispatchAudit.Trigger.UNPIN, byMatricule);
        return Optional.of(cr);
    }

    /**
     * Full re-greedy of a (date, shift). Clears the dispatched zone for any
     * non-pinned, non-rejected request in scope, then runs publish. Pinned
     * sequences keep their previous routing. Used by the "Réquilibrer"
     * button on the Process Dispatcher page.
     *
     * @return the post-rebalance preview (same shape as {@link #publish}).
     */
    @Transactional
    public DispatchPreview rebalance(LocalDate date, int shift, String byMatricule) {
        List<CuttingRequest> scope = cuttingRequestRepository.findAll(date, String.valueOf(shift));
        for (CuttingRequest cr : scope) {
            if (cr.isPinnedByChef()) continue;
            if ("REJECTED".equalsIgnoreCase(cr.getZoneAcceptanceStatus())) continue;
            String oldZone = cr.getDispatchedZone();
            cr.setDispatchedZone(null);
            cr.setZoneAcceptanceStatus(null);
            cuttingRequestRepository.save(cr);
            dispatchAuditService.write(cr.getSequence(), oldZone, null,
                    "Rebalance " + date + "/" + shift,
                    DispatchAudit.Trigger.REBALANCE, byMatricule);
        }
        return publish(date, shift, byMatricule);
    }

    // ------------------------------------------------------------------
    // Core dispatcher
    // ------------------------------------------------------------------

    private DispatchPreview compute(LocalDate date, int shift) {
        // Light path — projection queries, no full @Entity graph (see audit #3).
        List<Object[]> requestRows = cuttingRequestRepository.findAllLight(date, String.valueOf(shift));
        Map<String, RequestLight> requestBySeq = new LinkedHashMap<>();
        for (Object[] r : requestRows) {
            requestBySeq.put((String) r[0],
                    new RequestLight((String) r[0], (String) r[1], (String) r[2],
                            r[3] != null && Boolean.TRUE.equals(r[3]), (String) r[4]));
        }

        List<Object[]> serieRows = serieDataRepository.findSeriesByDateShiftLight(date, String.valueOf(shift));
        // Group series by sequence.
        Map<String, List<SerieDispatchInfo>> infosBySeq = new LinkedHashMap<>();
        for (Object[] sr : serieRows) {
            String serieId   = (String) sr[0];
            String sequence  = (String) sr[1];
            String machineType = (String) sr[2];
            Integer nbrCouche = sr[4] != null ? ((Number) sr[4]).intValue() : null;
            String placement = (String) sr[5];

            RequestLight req = requestBySeq.get(sequence);
            if (req == null) continue;

            infosBySeq.computeIfAbsent(sequence, k -> new ArrayList<>())
                      .add(new SerieDispatchInfo(
                              serieId, sequence, machineType,
                              null, nbrCouche, placement, req.preferredZoneNom));
        }

        Map<String, ZoneBreakdown> byZone = new LinkedHashMap<>();
        List<UnassignableRequest> unassignable = new ArrayList<>();

        for (RequestLight req : requestBySeq.values()) {
            // Skip already-rejected requests; keep PENDING/ACCEPTED re-eligible in case
            // the chef reverted a decision and wants a fresh preview.
            if ("REJECTED".equalsIgnoreCase(req.zoneAcceptanceStatus)) continue;

            List<SerieDispatchInfo> dispatchInfos = infosBySeq.getOrDefault(req.sequence, Collections.emptyList());
            SchedulableSerieFilter.FilterResult fr =
                    schedulableSerieFilter.filter(dispatchInfos, date, shift);

            if (fr.getSchedulable().isEmpty()) {
                // Every serie on this request failed to resolve.
                unassignable.add(new UnassignableRequest(req.sequence, fr.getRejected()));
                continue;
            }

            Zone chosen = chooseZone(fr.getSchedulable().values(),
                    req.preferredZoneNom != null ? zoneRepository.findByObjId(req.preferredZoneNom) : null,
                    byZone);
            byZone.computeIfAbsent(chosen.getNom(), n -> new ZoneBreakdown(n, chosen.getCategory()))
                  .addSequence(req.sequence);
        }

        return new DispatchPreview(date, shift, byZone, unassignable);
    }

    /**
     * Compute preview for all active sequences, using current date/shift
     * for zone-resolution. Excludes series whose statusCoupe = 'Complete'.
     */
    private static final int SQL_BATCH_SIZE = 2000;

    private DispatchPreview computeActive() {
        com.lear.MGCMS.services.scheduling.ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        LocalDate nowDate = slot.date;
        int nowShift = slot.shift;

        List<Object[]> requestRows = cuttingRequestRepository.findActiveDueOnOrBeforeLight(
                nowDate, String.valueOf(nowShift));
        Map<String, RequestLight> requestBySeq = new LinkedHashMap<>();
        for (Object[] r : requestRows) {
            requestBySeq.put((String) r[0],
                    new RequestLight((String) r[0], (String) r[1], (String) r[2],
                            r[3] != null && Boolean.TRUE.equals(r[3]), (String) r[4]));
        }

        List<String> sequences = new ArrayList<>(requestBySeq.keySet());
        List<Object[]> serieRows = new ArrayList<>();
        if (!sequences.isEmpty()) {
            for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
                List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
                serieRows.addAll(serieDataRepository.findActiveSeriesBySequencesLight(batch));
            }
        }

        // Sequences with at least one statusCoupe='Complete' serie are anchored to
        // their current dispatched_zone — cutting has begun on the floor and the
        // chef-de-zone has effectively committed the sequence to that zone.
        Set<String> frozenSequences = new HashSet<>();
        if (!sequences.isEmpty()) {
            for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
                List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
                frozenSequences.addAll(serieDataRepository.findSequenceIdsWithCompleteSeriesIn(batch));
            }
        }

        Map<String, List<SerieDispatchInfo>> infosBySeq = new LinkedHashMap<>();
        for (Object[] sr : serieRows) {
            String serieId   = (String) sr[0];
            String sequence  = (String) sr[1];
            String machineType = (String) sr[2];
            Integer nbrCouche = sr[4] != null ? ((Number) sr[4]).intValue() : null;
            String placement = (String) sr[5];

            RequestLight req = requestBySeq.get(sequence);
            if (req == null) continue;

            infosBySeq.computeIfAbsent(sequence, k -> new ArrayList<>())
                      .add(new SerieDispatchInfo(
                              serieId, sequence, machineType,
                              null, nbrCouche, placement, req.preferredZoneNom));
        }

        Map<String, ZoneBreakdown> byZone = new LinkedHashMap<>();
        List<UnassignableRequest> unassignable = new ArrayList<>();

        for (RequestLight req : requestBySeq.values()) {
            if ("REJECTED".equalsIgnoreCase(req.zoneAcceptanceStatus)) continue;

            if (frozenSequences.contains(req.sequence) && req.dispatchedZone != null) {
                Zone fixed = zoneRepository.findByObjId(req.dispatchedZone);
                if (fixed != null) {
                    byZone.computeIfAbsent(fixed.getNom(), n -> new ZoneBreakdown(n, fixed.getCategory()))
                          .addSequence(req.sequence);
                    continue;
                }
            }

            List<SerieDispatchInfo> dispatchInfos = infosBySeq.getOrDefault(req.sequence, Collections.emptyList());
            SchedulableSerieFilter.FilterResult fr =
                    schedulableSerieFilter.filter(dispatchInfos, nowDate, nowShift);

            if (fr.getSchedulable().isEmpty()) {
                unassignable.add(new UnassignableRequest(req.sequence, fr.getRejected()));
                continue;
            }

            Zone chosen = chooseZone(fr.getSchedulable().values(),
                    req.preferredZoneNom != null ? zoneRepository.findByObjId(req.preferredZoneNom) : null,
                    byZone);
            byZone.computeIfAbsent(chosen.getNom(), n -> new ZoneBreakdown(n, chosen.getCategory()))
                  .addSequence(req.sequence);
        }

        return new DispatchPreview(nowDate, nowShift, byZone, unassignable);
    }

    /**
     * Request-level tiebreaker: pick the zone covering the most series on
     * the request; ties prefer STRICT, then the zone with fewer sequences
     * already routed in the current preview (load-aware in-batch
     * balancing), then nom-alphabetical as the deterministic fallback.
     *
     * <p>Passing {@code byZoneSoFar} lets the dispatcher self-balance: when
     * two zones are equally valid for a request, we route to whichever has
     * fewer sequences in the in-progress preview. This replaces the older
     * pure-alpha tiebreak that systematically biased one zone.</p>
     */
    private Zone chooseZone(java.util.Collection<Zone> zones, Zone preferredZone,
                            Map<String, ZoneBreakdown> byZoneSoFar) {
        if (preferredZone != null && preferredZone.getNom() != null) {
            for (Zone zone : zones) {
                if (zone != null && preferredZone.getNom().equalsIgnoreCase(zone.getNom())) {
                    return zone;
                }
            }
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Zone> byNom = new LinkedHashMap<>();
        for (Zone z : zones) {
            if (z == null) continue;
            counts.merge(z.getNom(), 1, Integer::sum);
            byNom.put(z.getNom(), z);
        }
        return counts.entrySet().stream()
                .max((a, b) -> {
                    int cmp = Integer.compare(a.getValue(), b.getValue());
                    if (cmp != 0) return cmp;
                    Zone za = byNom.get(a.getKey());
                    Zone zb = byNom.get(b.getKey());
                    boolean aStrict = za != null && za.getCategory() == Zone.Category.STRICT;
                    boolean bStrict = zb != null && zb.getCategory() == Zone.Category.STRICT;
                    if (aStrict != bStrict) return aStrict ? 1 : -1;
                    int aCount = byZoneSoFar != null && byZoneSoFar.get(a.getKey()) != null
                            ? byZoneSoFar.get(a.getKey()).getCount() : 0;
                    int bCount = byZoneSoFar != null && byZoneSoFar.get(b.getKey()) != null
                            ? byZoneSoFar.get(b.getKey()).getCount() : 0;
                    if (aCount != bCount) return Integer.compare(bCount, aCount); // smaller count wins (max reverses)
                    return b.getKey().compareTo(a.getKey()); // deterministic final tiebreak
                })
                .map(e -> byNom.get(e.getKey()))
                .orElse(null);
    }

    private static <T> List<T> safe(List<T> in) {
        return in == null ? new ArrayList<>() : in;
    }

    // ------------------------------------------------------------------
    // DTOs
    // ------------------------------------------------------------------

    public static final class DispatchPreview {
        private final LocalDate date;
        private final int shift;
        private final Map<String, ZoneBreakdown> byZone;
        private final List<UnassignableRequest> unassignable;

        public DispatchPreview(LocalDate date, int shift,
                               Map<String, ZoneBreakdown> byZone,
                               List<UnassignableRequest> unassignable) {
            this.date = date; this.shift = shift;
            this.byZone = byZone; this.unassignable = unassignable;
        }

        public LocalDate getDate()                         { return date; }
        public int getShift()                              { return shift; }
        public Map<String, ZoneBreakdown> getByZone()      { return byZone; }
        public List<UnassignableRequest> getUnassignable() { return unassignable; }
    }

    public static final class ZoneBreakdown {
        private final String zoneNom;
        private final Zone.Category category;
        private final List<String> sequences = new ArrayList<>();

        public ZoneBreakdown(String zoneNom, Zone.Category category) {
            this.zoneNom = zoneNom; this.category = category;
        }
        public void addSequence(String s) { sequences.add(s); }

        public String getZoneNom()       { return zoneNom; }
        public Zone.Category getCategory() { return category; }
        public List<String> getSequences() { return sequences; }
        public int getCount()            { return sequences.size(); }
    }

    public static final class UnassignableRequest {
        private final String sequence;
        private final List<SchedulableSerieFilter.Rejection> serieRejections;

        public UnassignableRequest(String sequence,
                                   List<SchedulableSerieFilter.Rejection> serieRejections) {
            this.sequence = sequence;
            this.serieRejections = serieRejections;
        }

        public String getSequence() { return sequence; }
        public List<SchedulableSerieFilter.Rejection> getSerieRejections() { return serieRejections; }
    }

    /** Lightweight request scalar used by the projection path in {@link #compute}. */
    private static final class RequestLight {
        final String sequence;
        final String dispatchedZone;
        final String zoneAcceptanceStatus;
        final boolean pinnedByChef;
        final String preferredZoneNom;
        RequestLight(String sequence, String dispatchedZone, String zoneAcceptanceStatus,
                     boolean pinnedByChef, String preferredZoneNom) {
            this.sequence = sequence;
            this.dispatchedZone = dispatchedZone;
            this.zoneAcceptanceStatus = zoneAcceptanceStatus;
            this.pinnedByChef = pinnedByChef;
            this.preferredZoneNom = preferredZoneNom;
        }
    }
}
