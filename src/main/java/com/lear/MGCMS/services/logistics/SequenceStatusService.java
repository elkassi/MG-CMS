package com.lear.MGCMS.services.logistics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.ReleaseZoneSource;
import com.lear.MGCMS.domain.CuttingRequest.SequenceStatus;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.dispatcher.SerieStatusDateValidator;
import com.lear.MGCMS.services.dispatcher.WorkbenchCacheService;
import com.lear.cms.repositories.SuiviPlanningRepository;

/**
 * Single writer for the {@link SequenceStatus} lifecycle on
 * {@link CuttingRequestData#getSequenceStatus()}. Validates the requested
 * transition, persists the new status (and {@code releaseZone} when releasing),
 * and invalidates the derived dispatcher / workbench / timeline caches so the
 * in-production set ({@code RELEASED, STARTED, MATERIAL_MISSING}) is recomputed.
 *
 * <p>All lifecycle writes except the per-serie auto-COMPLETED corrector should
 * funnel through {@link #transition(String, String, String)}: the picklist
 * release (→ {@code RELEASED}), the CMS-Prod spreading app (→ {@code STARTED}),
 * and the chef actions (→ {@code COMPLETED} / {@code INCOMPLETE} /
 * {@code MATERIAL_MISSING} / back to {@code STARTED}).</p>
 */
@Service
public class SequenceStatusService {

    /** Every status a caller may target through this service. */
    private static final List<String> WRITABLE = Arrays.asList(
            SequenceStatus.IMPORTED, SequenceStatus.RELEASED, SequenceStatus.STARTED,
            SequenceStatus.COMPLETED, SequenceStatus.MATERIAL_MISSING, SequenceStatus.INCOMPLETE);

    /**
     * Allowed source statuses for each target. A {@code null} (legacy) current
     * status is treated as in-production and may move to any target. The map is
     * intentionally permissive within the in-production set so a chef can
     * re-open / re-flag without fighting the state machine.
     */
    private static final Map<String, Set<String>> ALLOWED_FROM = buildAllowedFrom();

    private static Map<String, Set<String>> buildAllowedFrom() {
        Map<String, Set<String>> m = new LinkedHashMap<>();
        // Logistics releases a pre-release (IMPORTED) sequence.
        m.put(SequenceStatus.RELEASED, set(SequenceStatus.IMPORTED));
        // Spreading starts once released, or after stock is recovered.
        m.put(SequenceStatus.STARTED, set(SequenceStatus.RELEASED, SequenceStatus.MATERIAL_MISSING,
                SequenceStatus.STARTED, SequenceStatus.COMPLETED, SequenceStatus.INCOMPLETE));
        // Stock shortage discovered while in production.
        m.put(SequenceStatus.MATERIAL_MISSING, set(SequenceStatus.RELEASED, SequenceStatus.STARTED,
                SequenceStatus.MATERIAL_MISSING));
        // Manual completion by a chef (auto-completion bypasses this service).
        m.put(SequenceStatus.COMPLETED, set(SequenceStatus.RELEASED, SequenceStatus.STARTED,
                SequenceStatus.MATERIAL_MISSING));
        // Chef removes an unfinishable sequence from production.
        m.put(SequenceStatus.INCOMPLETE, set(SequenceStatus.RELEASED, SequenceStatus.STARTED,
                SequenceStatus.MATERIAL_MISSING));
        // IMPORTED is only set on creation; it is never a transition target here.
        return Collections.unmodifiableMap(m);
    }

    private static Set<String> set(String... values) {
        return new java.util.HashSet<>(Arrays.asList(values));
    }

    private static final Logger log = LoggerFactory.getLogger(SequenceStatusService.class);

    /** Max sequences per IN (...) batch — stays under SQL Server's 2100-parameter cap. */
    private static final int BATCH_SIZE = 1000;

    /**
     * sequenceStatus -&gt; suiviplanning.Statu write-through used by chef
     * rectification. MATERIAL_MISSING / INCOMPLETE are absent on purpose:
     * suiviplanning has no representation for them and the 20-min sync already
     * never overwrites them.
     */
    private static final Map<String, String> STATU_BY_STATUS = buildStatuByStatus();

    private static Map<String, String> buildStatuByStatus() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(SequenceStatus.IMPORTED, "Non demarre");
        m.put(SequenceStatus.RELEASED, "Released");
        m.put(SequenceStatus.STARTED, "En cours");
        m.put(SequenceStatus.COMPLETED, "Complet");
        return Collections.unmodifiableMap(m);
    }

    /**
     * Master switch for the chef rectification override (free transitions +
     * suiviplanning write-through). Meant to be turned off once the historical
     * status mess is cleaned up: set {@code mgcms.sequence.rectify.enabled=false}.
     */
    @Value("${mgcms.sequence.rectify.enabled:true}")
    private boolean rectifyEnabled;

    @Autowired
    private CuttingRequestDataRepository requestDataRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private CuttingRequestSerieDataRepository serieDataRepository;

    @Autowired
    private CuttingRequestBoxInfoRepository boxInfoRepository;

    @Autowired
    private SuiviPlanningRepository suiviPlanningRepository;

    @Autowired(required = false)
    private SerieStatusDateValidator serieStatusDateValidator;

    @Autowired(required = false)
    private WorkbenchCacheService workbenchCacheService;

    @Autowired(required = false)
    private OrdonnancementService ordonnancementService;

    /**
     * Transition a sequence to {@code newStatus}.
     *
     * @param sequence   the CuttingRequest sequence id
     * @param newStatus  one of {@link SequenceStatus}
     * @param zoneOrNull the fixed release zone — required (and only used) when
     *                   {@code newStatus == RELEASED}
     * @return a result map: {@code success}, and on success
     *         {@code sequence / oldStatus / newStatus} (+ {@code releaseZone}
     *         when set); on failure {@code error}.
     */
    public Map<String, Object> transition(String sequence, String newStatus, String zoneOrNull) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (!WRITABLE.contains(newStatus)) {
            result.put("success", false);
            result.put("error", "Statut invalide: " + newStatus);
            return result;
        }

        CuttingRequestData request = requestDataRepository.findBySequence(sequence);
        if (request == null) {
            result.put("success", false);
            result.put("error", "Séquence non trouvée: " + sequence);
            return result;
        }

        String oldStatus = request.getSequenceStatus();
        if (!isTransitionAllowed(oldStatus, newStatus)) {
            result.put("success", false);
            result.put("error", "Transition non autorisée: " + oldStatus + " -> " + newStatus);
            return result;
        }

        if (SequenceStatus.RELEASED.equals(newStatus)) {
            if (zoneOrNull == null || zoneOrNull.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Zone de release requise pour le statut RELEASED");
                return result;
            }
            request.setReleaseZone(zoneOrNull.trim());
            // The picklist fixed this zone — locked against auto-correction.
            request.setReleaseZoneSource(ReleaseZoneSource.LOGISTICS);
        }

        request.setSequenceStatus(newStatus);
        requestDataRepository.save(request);

        // Write the change through to the legacy board in the same call so the
        // 20-min suiviplanning->sequenceStatus sync doesn't revert it and the
        // dispatcher/workbench reflect the floor immediately (closes the up-to-20-min
        // divergence — e.g. CMS-Prod /start setting STARTED). Only the four statuses
        // suiviplanning represents are written; MATERIAL_MISSING/INCOMPLETE have no
        // mapping and are deliberately left untouched.
        int suiviRows = 0;
        String statu = STATU_BY_STATUS.get(newStatus);
        if (statu != null) {
            suiviRows = suiviPlanningRepository.updateStatuBySequence(sequence, statu);
        }

        result.put("success", true);
        result.put("sequence", sequence);
        result.put("oldStatus", oldStatus);
        result.put("newStatus", newStatus);
        result.put("suiviRows", suiviRows);
        if (request.getReleaseZone() != null) {
            result.put("releaseZone", request.getReleaseZone());
        }
        afterProductionDataChange();
        return result;
    }

    /**
     * Chef rectification: this sequence is attributed to the wrong zone — move
     * it. Writes {@code releaseZone}, the highest-priority unlocked zone source
     * in the effective-zone resolution, so every derived view (live charge,
     * floor, box counts) follows. Only meaningful for in-production sequences;
     * a sequence physically started on a table keeps showing under its lock
     * zone regardless (physical reality wins).
     */
    public Map<String, Object> reassignZone(String sequence, String zoneNom) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (zoneNom == null || zoneNom.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "Zone requise");
            return result;
        }
        String zone = zoneNom.trim();
        Zone target = zoneRepository.findById(zone).orElse(null);
        if (target == null || !target.isActive()) {
            result.put("success", false);
            result.put("error", "Zone inconnue ou inactive: " + zone);
            return result;
        }
        CuttingRequestData request = requestDataRepository.findBySequence(sequence);
        if (request == null) {
            result.put("success", false);
            result.put("error", "Séquence non trouvée: " + sequence);
            return result;
        }
        String status = request.getSequenceStatus();
        boolean inProduction = status == null
                || SequenceStatus.RELEASED.equals(status)
                || SequenceStatus.STARTED.equals(status)
                || SequenceStatus.MATERIAL_MISSING.equals(status);
        if (!inProduction) {
            result.put("success", false);
            result.put("error", "Séquence non en production (" + status + ") — zone non modifiable");
            return result;
        }
        String oldZone = request.getReleaseZone();
        request.setReleaseZone(zone);
        request.setReleaseZoneSource(ReleaseZoneSource.CHEF);
        requestDataRepository.save(request);

        result.put("success", true);
        result.put("sequence", sequence);
        result.put("oldZone", oldZone);
        result.put("newZone", zone);
        afterProductionDataChange();
        return result;
    }

    /** Whether the chef rectification override is currently enabled. */
    public boolean isRectifyEnabled() {
        return rectifyEnabled;
    }

    /**
     * Chef rectification override: set any {@link SequenceStatus} and/or the
     * release zone, bypassing the {@code ALLOWED_FROM} state machine. For the
     * four statuses suiviplanning knows ({@code IMPORTED / RELEASED / STARTED /
     * COMPLETED}) the correction is written through to
     * {@code suiviplanning.Statu} so the 20-min sync agrees instead of
     * reverting it. Disabled globally via
     * {@code mgcms.sequence.rectify.enabled=false} once the cleanup phase is
     * over.
     *
     * @param sequence     the CuttingRequest sequence id
     * @param statusOrNull target status, or null to only change the zone
     * @param zoneOrNull   target release zone, or null to keep the current one
     *                     (required when rectifying to RELEASED with no zone on
     *                     record; rejected with IMPORTED, which clears the zone)
     * @param user         authenticated login, for the audit log
     */
    public Map<String, Object> rectify(String sequence, String statusOrNull, String zoneOrNull, String user) {
        Map<String, Object> result = doRectify(sequence, statusOrNull, zoneOrNull, user);
        if (Boolean.TRUE.equals(result.get("success"))) {
            afterProductionDataChange();
        }
        return result;
    }

    /**
     * Bulk variant of {@link #rectify} — same semantics per sequence, but the
     * derived-view refresh runs once at the end instead of once per sequence
     * (the optimizer snapshot reload is too heavy to run in a loop).
     */
    public Map<String, Object> rectifyBulk(List<String> sequences, String status, String user) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (sequences == null || sequences.isEmpty()) {
            result.put("success", false);
            result.put("error", "Aucune séquence fournie");
            return result;
        }
        List<Map<String, Object>> outcomes = new ArrayList<>();
        int ok = 0;
        for (String sequence : sequences) {
            Map<String, Object> one = doRectify(sequence, status, null, user);
            if (Boolean.TRUE.equals(one.get("success"))) {
                ok++;
            }
            outcomes.add(one);
        }
        if (ok > 0) {
            afterProductionDataChange();
        }
        result.put("success", ok > 0);
        result.put("updated", ok);
        result.put("failed", sequences.size() - ok);
        result.put("results", outcomes);
        return result;
    }

    private Map<String, Object> doRectify(String sequence, String statusOrNull, String zoneOrNull, String user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sequence", sequence);

        if (!rectifyEnabled) {
            result.put("success", false);
            result.put("error", "La rectification est désactivée (mgcms.sequence.rectify.enabled=false)");
            return result;
        }
        String status = trimToNull(statusOrNull);
        String zone = trimToNull(zoneOrNull);
        if (status == null && zone == null) {
            result.put("success", false);
            result.put("error", "Statut ou zone requis");
            return result;
        }
        if (status != null && !WRITABLE.contains(status)) {
            result.put("success", false);
            result.put("error", "Statut invalide: " + status);
            return result;
        }
        if (SequenceStatus.IMPORTED.equals(status) && zone != null) {
            result.put("success", false);
            result.put("error", "Zone non applicable au statut IMPORTED (elle sera réinitialisée)");
            return result;
        }
        if (zone != null) {
            Zone target = zoneRepository.findById(zone).orElse(null);
            if (target == null || !target.isActive()) {
                result.put("success", false);
                result.put("error", "Zone inconnue ou inactive: " + zone);
                return result;
            }
        }
        CuttingRequestData request = requestDataRepository.findBySequence(sequence);
        if (request == null) {
            result.put("success", false);
            result.put("error", "Séquence non trouvée: " + sequence);
            return result;
        }

        String oldStatus = request.getSequenceStatus();
        String oldZone = request.getReleaseZone();
        if (zone != null) {
            request.setReleaseZone(zone);
            // Explicit chef decision — locked against auto-correction.
            request.setReleaseZoneSource(ReleaseZoneSource.CHEF);
        }
        if (status != null) {
            if (SequenceStatus.RELEASED.equals(status) && request.getReleaseZone() == null) {
                result.put("success", false);
                result.put("error", "Zone de release requise pour le statut RELEASED");
                return result;
            }
            if (SequenceStatus.IMPORTED.equals(status)) {
                request.setReleaseZone(null);
                request.setReleaseZoneSource(null);
            }
            request.setSequenceStatus(status);
        }
        requestDataRepository.save(request);

        int suiviRows = 0;
        String statu = status != null ? STATU_BY_STATUS.get(status) : null;
        if (statu != null) {
            suiviRows = suiviPlanningRepository.updateStatuBySequence(sequence, statu);
        }
        log.info("Sequence rectification by {}: {} status {} -> {}, zone {} -> {} (suiviplanning rows: {})",
                user, sequence, oldStatus, request.getSequenceStatus(), oldZone, request.getReleaseZone(), suiviRows);

        result.put("success", true);
        result.put("oldStatus", oldStatus);
        result.put("newStatus", request.getSequenceStatus());
        result.put("oldZone", oldZone);
        result.put("newZone", request.getReleaseZone());
        result.put("suiviRows", suiviRows);
        return result;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Rows for the chef rectification screen: every sequence planned in the
     * last {@code days} days (1..60, default 7) with its status, zone, serie
     * progress (closed/total) and box count, plus the active-zone list for the
     * reassignment dropdown. Filtering by zone/status/text happens client-side
     * — the window keeps the set to a few hundred rows.
     */
    public Map<String, Object> rectificationList(int days) {
        int window = Math.max(1, Math.min(days, 60));
        LocalDate cutoff = LocalDate.now().minusDays(window);
        List<Object[]> raw = requestDataRepository.findRectificationRows(cutoff);

        List<String> sequences = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            sequences.add((String) row[0]);
        }
        Map<String, int[]> serieProgress = loadSerieProgress(sequences);
        Map<String, Integer> boxCounts = loadBoxCounts(sequences);

        List<Map<String, Object>> rows = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            String sequence = (String) row[0];
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("sequence", sequence);
            r.put("status", row[1]);
            r.put("zone", row[2]);
            r.put("projet", row[3]);
            r.put("planningDate", row[4] != null ? row[4].toString() : null);
            r.put("shift", row[5]);
            r.put("dueDate", row[6] != null ? row[6].toString() : null);
            r.put("dueShift", row[7]);
            r.put("zoneSource", row[8]);
            int[] progress = serieProgress.get(sequence);
            r.put("seriesTotal", progress != null ? progress[0] : 0);
            r.put("seriesDone", progress != null ? progress[1] : 0);
            r.put("boxes", boxCounts.getOrDefault(sequence, 0));
            rows.add(r);
        }

        List<String> zones = new ArrayList<>();
        for (Zone z : zoneRepository.findAll()) {
            if (z.isActive()) {
                zones.add(z.getNom());
            }
        }
        Collections.sort(zones);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rectifyEnabled", rectifyEnabled);
        result.put("days", window);
        result.put("zones", zones);
        result.put("rows", rows);
        return result;
    }

    /** sequence -&gt; [total series, series with statusCoupe=Complete], batched. */
    private Map<String, int[]> loadSerieProgress(List<String> sequences) {
        Map<String, int[]> out = new HashMap<>();
        for (int i = 0; i < sequences.size(); i += BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + BATCH_SIZE, sequences.size()));
            for (Object[] row : serieDataRepository.countSerieProgressBySequences(batch)) {
                if (row == null || row.length < 3 || row[0] == null) continue;
                int total = row[1] != null ? ((Number) row[1]).intValue() : 0;
                int done = row[2] != null ? ((Number) row[2]).intValue() : 0;
                out.put(String.valueOf(row[0]), new int[] { total, done });
            }
        }
        return out;
    }

    /** sequence -&gt; box count, batched — same source as the floor / release views. */
    private Map<String, Integer> loadBoxCounts(List<String> sequences) {
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < sequences.size(); i += BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + BATCH_SIZE, sequences.size()));
            for (Object[] row : boxInfoRepository.countBoxesBySequences(batch)) {
                if (row == null || row.length < 2 || row[0] == null) continue;
                out.put(String.valueOf(row[0]), row[1] != null ? ((Number) row[1]).intValue() : 0);
            }
        }
        return out;
    }

    private boolean isTransitionAllowed(String oldStatus, String newStatus) {
        if (oldStatus == null) {
            // Legacy rows behave as in-production; allow any post-release target.
            return !SequenceStatus.IMPORTED.equals(newStatus);
        }
        if (oldStatus.equals(newStatus)) {
            return true; // idempotent re-write
        }
        Set<String> sources = ALLOWED_FROM.get(newStatus);
        return sources != null && sources.contains(oldStatus);
    }

    /**
     * Mirror of {@code MaterialDemandForecastService.afterProductionDataChange()}
     * — refreshes the derived views that key off {@code sequenceStatus}. Failures
     * are swallowed; the next poll rebuilds them.
     */
    private void afterProductionDataChange() {
        try {
            if (serieStatusDateValidator != null) {
                serieStatusDateValidator.normalizeProductionProgress();
            }
            if (ordonnancementService != null) {
                ordonnancementService.invalidateTimelineCache();
            }
            if (workbenchCacheService != null) {
                workbenchCacheService.invalidateAll();
            }
            // Deliberately NOT calling optimizerService.reloadActiveSnapshotFromGroundTruth()
            // here. With the continuous optimizer shelved (2026-05-31 pivot) the engine stays
            // IDLE, so that call takes the heavy synchronous branch (buildSnapshotForActive +
            // greedyWarmStart + rebuildSchedule) on the HTTP request thread — which is what made
            // every chef rectify/start/complete request hang ("stays pending"). The scheduled
            // 10-min refresh keeps any future engine snapshot fresh; the three cache
            // invalidations above are what the rectification/floor/charge views actually need.
        } catch (Exception ignored) {
            // Derived views refresh on the next poll if this eager refresh fails.
        }
    }
}
