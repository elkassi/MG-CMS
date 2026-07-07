package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.dispatcher.DispatchAudit;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.dispatcher.DispatchAuditService;
import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.PreviewWithLoad;
import com.lear.MGCMS.services.dispatcher.SequenceDispatcherService;
import com.lear.MGCMS.services.dispatcher.SequenceDispatcherService.DispatchPreview;
import com.lear.MGCMS.services.dispatcher.UserZoneService;
import com.lear.MGCMS.services.dispatcher.ZoneLoadDto;
import com.lear.MGCMS.services.dispatcher.ZoneLoadService;

/**
 * REST surface for the Phase 4 dispatcher. Endpoints:
 * <ul>
 *   <li>Return 404 when {@code mgcms.dispatcher.enabled=false} so the
 *       feature is invisible to the UI on deployments that haven't
 *       flipped the flag.</li>
 *   <li>{@code /preview} + {@code /publish} require {@code ROLE_PROCESS}
 *       — only the Process role can dispatch sequences into zones.</li>
 *   <li>{@code /sequence/{sequence}/acceptance} (Phase 10) is open to
 *       chef-de-zone / chef-d'équipe / process and delegates to
 *       {@link SequenceDispatcherService#setAcceptance} so the write
 *       lives in the service layer and a {@link
 *       com.lear.MGCMS.services.dispatcher.SequenceAcceptanceChangedEvent}
 *       fires on every flip.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dispatcher")
public class DispatcherController {

    @Autowired private SequenceDispatcherService dispatcher;
    @Autowired private DispatcherProperties properties;
    @Autowired private UserService userService;
    @Autowired private DispatchAuditService dispatchAuditService;
    @Autowired private UserZoneService userZoneService;
    @Autowired private ZoneLoadService zoneLoadService;

    /** Dry-run — read-only, no writes. */
    @GetMapping("/preview")
    @PreAuthorize("hasRole('PROCESS')")
    public ResponseEntity<DispatchPreview> preview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dispatcher.preview(date, shift));
    }

    /** Dry-run with load matrix — read-only, no writes. */
    @GetMapping("/previewWithLoad")
    @PreAuthorize("hasRole('PROCESS')")
    public ResponseEntity<PreviewWithLoad> previewWithLoad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        DispatchPreview preview = dispatcher.preview(date, shift);
        ZoneLoadDto load = zoneLoadService.computeMatrix(date, shift);
        return ResponseEntity.ok(new PreviewWithLoad(preview, load));
    }

    /**
     * Real-time preview for all active sequences — no date/shift selection required.
     * Uses current wall-clock date/shift for zone resolution.
     */
    @GetMapping("/previewActive")
    @PreAuthorize("hasRole('PROCESS')")
    public ResponseEntity<PreviewWithLoad> previewActive() {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        DispatchPreview preview = dispatcher.previewActive();
        ZoneLoadDto load = zoneLoadService.computeMatrix(preview.getDate(), preview.getShift());
        return ResponseEntity.ok(new PreviewWithLoad(preview, load));
    }

    /** Commit step — writes dispatched_zone + fires events. */
    @PostMapping("/publish")
    @PreAuthorize("hasRole('PROCESS')")
    public ResponseEntity<DispatchPreview> publish(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift,
            Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        String matricule = resolveMatricule(authentication);
        return ResponseEntity.status(HttpStatus.OK)
                .body(dispatcher.publish(date, shift, matricule));
    }

    /**
     * Chef-de-zone acceptance action (Phase 10).
     * Flips a CuttingRequest's {@code zone_acceptance_status} to
     * {@code ACCEPTED} or {@code REJECTED} via the service layer so the
     * write goes through the proper transaction boundary and an
     * acceptance event is published for downstream listeners (review C5/C6).
     * 404 when the feature flag is off or the sequence is unknown.
     */
    @PostMapping("/sequence/{sequence}/acceptance")
    @PreAuthorize("hasAnyRole('CHEF_DE_ZONE','CHEF_EQUIPE','PROCESS')")
    public ResponseEntity<?> setAcceptance(
            @PathVariable String sequence,
            @RequestParam String status,
            Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        try {
            String matricule = resolveMatricule(authentication);
            Optional<CuttingRequest> result = dispatcher.setAcceptance(sequence, status, matricule);
            if (!result.isPresent()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Force a sequence to a specific zone, overriding the dispatcher's
     * recommendation. Body: {@code {"zoneNom": "...", "reason": "..."}}.
     * Resets acceptance to PENDING so the chef must re-accept.
     */
    @PostMapping("/sequence/{sequence}/force")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> force(
            @PathVariable String sequence,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        String zoneNom = body == null ? null : body.get("zoneNom");
        String reason  = body == null ? null : body.get("reason");
        if (zoneNom == null || zoneNom.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "zoneNom required"));
        }
        Optional<CuttingRequest> result = dispatcher.force(
                sequence, zoneNom.trim(), reason, resolveMatricule(authentication));
        return result.isPresent()
                ? ResponseEntity.ok(Map.of("sequence", sequence, "dispatchedZone", zoneNom))
                : ResponseEntity.notFound().build();
    }

    /**
     * Pin a sequence to its current zone — greedy re-dispatch will not
     * touch it. Body: {@code {"reason": "..."}}.
     * Caller must own the sequence's dispatched zone (via UserZone) or be
     * PROCESS / ADMIN.
     */
    @PostMapping("/sequence/{sequence}/pin")
    @PreAuthorize("hasAnyRole('CHEF_DE_ZONE','CHEF_EQUIPE','PROCESS','ADMIN')")
    public ResponseEntity<?> pin(
            @PathVariable String sequence,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        String reason = body == null ? null : body.get("reason");
        Optional<CuttingRequest> result = dispatcher.pin(
                sequence, reason, resolveMatricule(authentication));
        return result.isPresent()
                ? ResponseEntity.ok(Map.of("sequence", sequence, "pinnedByChef", true))
                : ResponseEntity.notFound().build();
    }

    /** Remove a chef pin. PROCESS / ADMIN only. */
    @PostMapping("/sequence/{sequence}/unpin")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<?> unpin(
            @PathVariable String sequence,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        String reason = body == null ? null : body.get("reason");
        Optional<CuttingRequest> result = dispatcher.unpin(
                sequence, reason, resolveMatricule(authentication));
        return result.isPresent()
                ? ResponseEntity.ok(Map.of("sequence", sequence, "pinnedByChef", false))
                : ResponseEntity.notFound().build();
    }

    /**
     * Chef pulls a SHARED-overflow sequence into one of their STRICT zones.
     * Body: {@code {"zoneNom": "...", "reason": "..."}}.
     * Validates UserZone ownership server-side — chefs can only pull into
     * zones they own.
     */
    @PostMapping("/sequence/{sequence}/pull")
    @PreAuthorize("hasAnyRole('CHEF_DE_ZONE','CHEF_EQUIPE','PROCESS','ADMIN')")
    public ResponseEntity<?> pull(
            @PathVariable String sequence,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        String zoneNom = body == null ? null : body.get("zoneNom");
        String reason  = body == null ? null : body.get("reason");
        if (zoneNom == null || zoneNom.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "zoneNom required"));
        }
        // Ownership check (skip for PROCESS / ADMIN).
        if (authentication != null
                && !hasAnyAuthority(authentication, "ROLE_PROCESS", "ROLE_ADMIN", "ROLE_CHEF_EQUIPE")) {
            User u = userService.findByUsername(authentication.getName());
            if (u == null || !chefOwnsZone(u, zoneNom.trim())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "FORBIDDEN_ZONE",
                                     "message", "Vous ne gérez pas la zone " + zoneNom));
            }
        }
        Optional<CuttingRequest> result = dispatcher.pull(
                sequence, zoneNom.trim(), reason, resolveMatricule(authentication));
        return result.isPresent()
                ? ResponseEntity.ok(Map.of("sequence", sequence, "dispatchedZone", zoneNom))
                : ResponseEntity.notFound().build();
    }

    /**
     * Full re-greedy of a (date, shift). Pinned sequences keep their zone,
     * everything else is recomputed. Returns the post-rebalance preview.
     */
    @PostMapping("/rebalance")
    @PreAuthorize("hasAnyRole('PROCESS','ADMIN')")
    public ResponseEntity<DispatchPreview> rebalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift,
            Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dispatcher.rebalance(date, shift, resolveMatricule(authentication)));
    }

    /**
     * Latest dispatch audit rows for the audit panel.
     * @param limit  max 500, default 50.
     */
    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<?> audit(
            @RequestParam(defaultValue = "50") int limit) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        List<DispatchAudit> rows = dispatchAuditService.recent(limit);
        // Project to a wire-friendly shape so we don't leak internals.
        List<Map<String, Object>> out = new java.util.ArrayList<>(rows.size());
        for (DispatchAudit a : rows) {
            Map<String, Object> r = new HashMap<>();
            r.put("id", a.getId());
            r.put("sequence", a.getSequence());
            r.put("fromZone", a.getFromZone());
            r.put("toZone", a.getToZone());
            r.put("reason", a.getReason());
            r.put("trigger", a.getTrigger() == null ? null : a.getTrigger().name());
            r.put("matricule", a.getMatricule());
            r.put("createdAt", a.getCreatedAt());
            out.add(r);
        }
        return ResponseEntity.ok(out);
    }

    /** Audit trail for one sequence (drill-down). */
    @GetMapping("/sequence/{sequence}/audit")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<?> sequenceAudit(@PathVariable String sequence) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dispatchAuditService.bySequence(sequence));
    }

    private static boolean hasAnyAuthority(Authentication auth, String... roles) {
        if (auth == null || auth.getAuthorities() == null) return false;
        for (String role : roles) {
            for (org.springframework.security.core.GrantedAuthority a : auth.getAuthorities()) {
                if (role.equals(a.getAuthority())) return true;
            }
        }
        return false;
    }

    private boolean chefOwnsZone(User u, String zoneNom) {
        if (u == null || zoneNom == null) return false;
        for (Zone z : userZoneService.findZonesForUser(u)) {
            if (z != null && zoneNom.equalsIgnoreCase(z.getNom())) return true;
        }
        return false;
    }

    private String resolveMatricule(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        User u = userService.findByUsername(auth.getName());
        return u == null ? auth.getName() : u.getMatricule();
    }
}
