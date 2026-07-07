package com.lear.MGCMS.controller.logistics;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.CuttingRequest.SequenceStatus;
import com.lear.MGCMS.services.logistics.SequenceStatusService;
import com.lear.MGCMS.services.logistics.SequenceZoneAutoCorrectService;

/**
 * Sequence-lifecycle transition endpoints (see {@link SequenceStatus}).
 *
 * <ul>
 *   <li>{@code POST /api/sequence/{sequence}/start} — set {@code STARTED}.
 *       Callable by the CMS-Prod spreading app (PROCESS) and by chefs/admin.</li>
 *   <li>{@code POST /api/sequence/{sequence}/complete} — set {@code COMPLETED}
 *       (chef/admin manual completion).</li>
 *   <li>{@code POST /api/sequence/{sequence}/incomplete} — set {@code INCOMPLETE}
 *       (chef removes an unfinishable sequence from production).</li>
 *   <li>{@code POST /api/sequence/{sequence}/material-missing} — set
 *       {@code MATERIAL_MISSING}.</li>
 *   <li>{@code POST /api/sequence/{sequence}/rectify},
 *       {@code POST /api/sequence/rectify-bulk},
 *       {@code GET /api/sequence/rectification} — chef rectification override
 *       (free status/zone correction with suiviplanning write-through),
 *       gated by {@code mgcms.sequence.rectify.enabled}.</li>
 * </ul>
 *
 * <p>The {@code RELEASED} transition is intentionally not exposed through the
 * lifecycle endpoints — the picklist/logistics-release feature calls
 * {@link SequenceStatusService#transition(String, String, String)} directly so
 * it can supply the fixed release zone. (Rectify can set {@code RELEASED}, but
 * only with an explicit or already-recorded zone.)</p>
 */
@RestController
@RequestMapping("/api/sequence")
public class SequenceStatusController {

    @Autowired
    private SequenceStatusService sequenceStatusService;

    @Autowired
    private SequenceZoneAutoCorrectService zoneAutoCorrectService;

    /**
     * Mark a sequence as STARTED (a serie has begun spreading). Exposed to the
     * CMS-Prod spreading app (PROCESS) as well as chefs/admin.
     */
    @PostMapping("/{sequence}/start")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> start(@PathVariable String sequence) {
        return respond(sequenceStatusService.transition(sequence, SequenceStatus.STARTED, null));
    }

    @PostMapping("/{sequence}/complete")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable String sequence) {
        return respond(sequenceStatusService.transition(sequence, SequenceStatus.COMPLETED, null));
    }

    @PostMapping("/{sequence}/incomplete")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> incomplete(@PathVariable String sequence) {
        return respond(sequenceStatusService.transition(sequence, SequenceStatus.INCOMPLETE, null));
    }

    @PostMapping("/{sequence}/material-missing")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> materialMissing(@PathVariable String sequence) {
        return respond(sequenceStatusService.transition(sequence, SequenceStatus.MATERIAL_MISSING, null));
    }

    /**
     * Chef rectification: "this sequence is not in my zone" — reassign it so
     * the per-zone box occupancy is correct. Body: {@code {"zone": "NEJMA"}}.
     */
    @PostMapping("/{sequence}/zone")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> reassignZone(@PathVariable String sequence,
                                                            @RequestBody Map<String, String> body) {
        return respond(sequenceStatusService.reassignZone(sequence, body != null ? body.get("zone") : null));
    }

    /**
     * Chef rectification override (cleanup phase): set any status and/or zone,
     * bypassing the state machine, with write-through to suiviplanning so the
     * 20-min sync keeps the correction. Body: {@code {"status": "...", "zone": "..."}}
     * (either field optional). Globally disabled via
     * {@code mgcms.sequence.rectify.enabled=false}.
     */
    @PostMapping("/{sequence}/rectify")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> rectify(@PathVariable String sequence,
                                                       @RequestBody Map<String, String> body,
                                                       Principal principal) {
        String status = body != null ? body.get("status") : null;
        String zone = body != null ? body.get("zone") : null;
        return respond(sequenceStatusService.rectify(sequence, status, zone, userOf(principal)));
    }

    /**
     * Bulk rectification — same status applied to many sequences in one call
     * (one derived-view refresh at the end). Body:
     * {@code {"sequences": [...], "status": "COMPLETED"}}.
     */
    @PostMapping("/rectify-bulk")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> rectifyBulk(@RequestBody Map<String, Object> body,
                                                           Principal principal) {
        @SuppressWarnings("unchecked")
        List<String> sequences = body != null ? (List<String>) body.get("sequences") : null;
        String status = body != null ? (String) body.get("status") : null;
        return respond(sequenceStatusService.rectifyBulk(sequences, status, userOf(principal)));
    }

    /**
     * Data for the chef rectification screen: every sequence planned in the
     * last {@code days} days with status, zone, serie progress and box count.
     */
    @GetMapping("/rectification")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> rectificationList(@RequestParam(defaultValue = "1") int days) {
        return ResponseEntity.ok(sequenceStatusService.rectificationList(days));
    }

    /**
     * Run one zone auto-correction pass now (the scheduled job runs every
     * 15 minutes anyway) — lets a chef refresh the inferred zones on demand
     * during the cleanup phase.
     */
    @PostMapping("/zone-autofix")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','CHEF_DE_ZONE','ADMIN')")
    public ResponseEntity<Map<String, Object>> zoneAutofix() {
        return ResponseEntity.ok(zoneAutoCorrectService.runOnce());
    }

    private static String userOf(Principal principal) {
        return principal != null ? principal.getName() : "?";
    }

    private ResponseEntity<Map<String, Object>> respond(Map<String, Object> result) {
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }
}
