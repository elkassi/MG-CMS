package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.dispatcher.DispatchEngineIndicatorSample;
import com.lear.MGCMS.domain.dispatcher.DispatchEngineRun;
import com.lear.MGCMS.domain.dispatcher.DispatchEngineRunSuggestion;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.dispatcher.ContinuousDispatchOptimizerService;
import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.EngineMode;

/**
 * REST surface for the Dispatching Engine (Phase B).
 *
 * <p>Endpoints mirror the state machine:</p>
 * <ul>
 *   <li>{@code /state} — read-only, open to PROCESS / ADMIN / CHEF_DE_ZONE.</li>
 *   <li>{@code /start} / {@code /pause} / {@code /resume} / {@code /stop} —
 *       PROCESS / ADMIN only.</li>
 *   <li>{@code /runs} — history of saved runs.</li>
 *   <li>{@code /runs/{id}/publish} — promote a saved suggestion to
 *       {@code dispatched_zone} rows.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dispatcher/engine")
public class DispatchEngineController {

    @Autowired private ContinuousDispatchOptimizerService optimizer;
    @Autowired private DispatcherProperties properties;
    @Autowired private UserService userService;
    @Autowired private com.lear.MGCMS.services.dispatcher.SerieStatusDateValidator serieStatusDateValidator;

    // ------------------------------------------------------------------ read

    @GetMapping("/state")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN','CHEF_DE_ZONE','CHEF_EQUIPE')")
    public ResponseEntity<?> state() {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(optimizer.getStateSnapshot());
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> runs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<DispatchEngineRun> result = optimizer.findRuns(pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (DispatchEngineRun r : result.getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("startedAt", r.getStartedAt());
            m.put("endedAt", r.getEndedAt());
            m.put("mode", r.getMode() != null ? r.getMode().name() : null);
            m.put("durationSec", r.getDurationSec());
            m.put("finalState", r.getFinalState() != null ? r.getFinalState().name() : null);
            m.put("iterations", r.getIterations());
            m.put("improvements", r.getImprovements());
            m.put("initialSpread", r.getInitialSpread());
            m.put("finalSpread", r.getFinalSpread());
            m.put("notes", r.getNotes());
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", items);
        out.put("totalElements", result.getTotalElements());
        out.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/runs/{id}/samples")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> samples(@PathVariable Long id) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        List<DispatchEngineIndicatorSample> rows = optimizer.findSamples(id);
        List<Map<String, Object>> out = new ArrayList<>();
        for (DispatchEngineIndicatorSample s : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("runId", s.getId() != null ? s.getId().getRunId() : null);
            m.put("iteration", s.getId() != null ? s.getId().getIteration() : null);
            m.put("sampleAt", s.getSampleAt());
            m.put("spreadPct", s.getSpreadPct());
            m.put("maxLoadPct", s.getMaxLoadPct());
            m.put("minLoadPct", s.getMinLoadPct());
            m.put("accepted", s.isAccepted());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    /**
     * Current in-memory schedule snapshot. Read-only debug surface for the
     * Process page — the public next-series endpoint reads from the same
     * underlying data via {@code /api/public/next-series}.
     */
    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN','CHEF_DE_ZONE')")
    public ResponseEntity<?> schedule(@RequestParam(required = false) String machine) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        com.lear.MGCMS.services.dispatcher.ScheduleSnapshot snap = optimizer.getCurrentSchedule();
        Map<String, java.util.List<com.lear.MGCMS.services.dispatcher.ScheduleSnapshot.PlannedSlot>> byMachine;
        if (machine != null && !machine.isBlank()) {
            byMachine = new LinkedHashMap<>();
            byMachine.put(machine, snap.slotsForMachine(machine));
        } else {
            byMachine = snap.groupByMachine();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalSlots", snap.size());
        Map<String, java.util.List<Map<String, Object>>> machines = new LinkedHashMap<>();
        for (Map.Entry<String, java.util.List<com.lear.MGCMS.services.dispatcher.ScheduleSnapshot.PlannedSlot>> e : byMachine.entrySet()) {
            java.util.List<Map<String, Object>> slots = new ArrayList<>();
            for (com.lear.MGCMS.services.dispatcher.ScheduleSnapshot.PlannedSlot s : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("serieId", s.getSerieId());
                m.put("sequenceId", s.getSequenceId());
                m.put("phase", s.getPhase().name());
                m.put("zoneNom", s.getZoneNom());
                m.put("plannedStart", s.getPlannedStart());
                m.put("plannedEnd", s.getPlannedEnd());
                m.put("plannedMinutes", s.getPlannedMinutes());
                slots.add(m);
            }
            machines.put(e.getKey(), slots);
        }
        out.put("machines", machines);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/runs/{id}/suggestions")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN','CHEF_DE_ZONE')")
    public ResponseEntity<?> suggestions(@PathVariable Long id) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        List<DispatchEngineRunSuggestion> rows = optimizer.findSuggestions(id);
        List<Map<String, Object>> out = new ArrayList<>();
        for (DispatchEngineRunSuggestion s : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("runId", s.getId() != null ? s.getId().getRunId() : null);
            m.put("sequence", s.getId() != null ? s.getId().getSequence() : null);
            m.put("suggestedZone", s.getSuggestedZone());
            m.put("previousZone", s.getPreviousZone());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    // ------------------------------------------------------------------ write

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> start(@RequestBody Map<String, Object> body,
                                   Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        try {
            // Force ALTERNATING mode regardless of request
            EngineMode mode = EngineMode.ALTERNATING;
            String dateStr = (String) body.get("date");
            LocalDate date = LocalDate.parse(dateStr);
            int shift = ((Number) body.get("shift")).intValue();

            String userId = resolveUserId(authentication);
            boolean started = optimizer.start(date, shift, mode, null, userId);
            String status = started ? "STARTED" : "ALREADY_RUNNING";
            return ResponseEntity.ok(Map.of("status", status, "state", optimizer.getState().name()));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Start the engine for all active sequences — no date/shift required.
     * Uses current wall-clock date/shift for zone resolution.
     */
    @PostMapping("/startActive")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> startActive(@RequestBody Map<String, Object> body,
                                         Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        try {
            // Force ALTERNATING mode regardless of request
            EngineMode mode = EngineMode.ALTERNATING;

            String userId = resolveUserId(authentication);
            boolean started = optimizer.startActive(mode, null, userId);
            String status = started ? "STARTED" : "ALREADY_RUNNING";
            return ResponseEntity.ok(Map.of("status", status, "state", optimizer.getState().name()));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/pause")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> pause() {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        optimizer.pause();
        return ResponseEntity.ok(Map.of("state", optimizer.getState().name()));
    }

    @PostMapping("/resume")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> resume() {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        optimizer.resume();
        return ResponseEntity.ok(Map.of("state", optimizer.getState().name()));
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> stop() {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        optimizer.stop();
        return ResponseEntity.ok(Map.of("state", optimizer.getState().name(), "runId", optimizer.getCurrentRunId()));
    }

    /**
     * Force a full snapshot reload — re-reads DB state, normalizes overlapping
     * coupe intervals first, then asks the engine to rebuild on its next iteration.
     * Use this when the UI suspects the engine has stale data.
     */
    @PostMapping("/reload")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN','CHEF_DE_ZONE','CHEF_EQUIPE')")
    public ResponseEntity<?> reload() {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        Map<String, Integer> corrections = Map.of();
        try {
            corrections = serieStatusDateValidator.normalizeProductionProgress();
        } catch (Exception ex) {
            // Normalisation is best-effort — never block the reload itself.
        }
        Map<String, Object> engineReload = optimizer.reloadActiveSnapshotFromGroundTruth();
        return ResponseEntity.ok(Map.of(
                "status", engineReload.getOrDefault("status", "REBUILD_REQUESTED"),
                "corrections", corrections,
                "overlapsPatched", corrections.getOrDefault("coupeClosed", 0),
                "engineState", optimizer.getState().name()));
    }

    /**
     * Lightweight per-sequence reload. Today this just triggers the same
     * engine rebuild (cheap at our scale: 100 seq / 1000 series). The
     * sequence id is logged for audit but does not narrow the rebuild scope.
     */
    @PostMapping("/reload-sequence/{sequence}")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN','CHEF_DE_ZONE','CHEF_EQUIPE')")
    public ResponseEntity<?> reloadSequence(@PathVariable String sequence) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        optimizer.requestRebuild();
        return ResponseEntity.ok(Map.of(
                "status", "REBUILD_REQUESTED",
                "sequence", sequence,
                "engineState", optimizer.getState().name()));
    }

    /**
     * Run the coupe-overlap normalizer without touching the engine. Returns
     * the count of series whose dateFinCoupe was patched.
     */
    @PostMapping("/normalize-coupe-overlaps")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> normalizeCoupeOverlaps() {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        Map<String, Integer> patched = serieStatusDateValidator.normalizeProductionProgress();
        optimizer.reloadActiveSnapshotFromGroundTruth();
        return ResponseEntity.ok(patched);
    }

    @PostMapping("/runs/{id}/publish")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> publishRun(@PathVariable Long id, Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        String matricule = resolveMatricule(authentication);
        int count = optimizer.publishRun(id, matricule);
        return ResponseEntity.ok(Map.of("published", count));
    }

    // ------------------------------------------------------------------ helpers

    private String resolveUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        User u = userService.findByUsername(auth.getName());
        return u != null ? u.getMatricule() : null;
    }

    private String resolveMatricule(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        User u = userService.findByUsername(auth.getName());
        return u != null ? u.getMatricule() : auth.getName();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
