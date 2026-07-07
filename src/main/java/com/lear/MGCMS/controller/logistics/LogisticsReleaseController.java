package com.lear.MGCMS.controller.logistics;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.logistics.LogisticsReleaseService;
import com.lear.MGCMS.services.scheduling.ShiftClock;

/**
 * Read-only logistics release advisor (Phase 1 — advise only, no writes).
 *
 * <p>Returns, for the given (date, shift), the active not-yet-released sequences
 * with material availability, a suggested zone (STRICT-started ⇒ pinned), and
 * roll placement suggestions. Logistics still performs the actual
 * {@code Non demarre → Released} transition in the existing screen.</p>
 */
@RestController
@RequestMapping("/api/logistics/release")
public class LogisticsReleaseController {

    @Autowired private DispatcherProperties dispatcherProperties;
    @Autowired private ShiftClock shiftClock;
    @Autowired private LogisticsReleaseService logisticsReleaseService;

    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE','VARIANCE')")
    public ResponseEntity<?> candidates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer shift) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        if (date == null) { date = slot.date; }
        if (shift == null) { shift = slot.shift; }
        if (shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().body("shift must be 1, 2 or 3");
        }
        return ResponseEntity.ok(logisticsReleaseService.build(date, shift));
    }

    /** Stage 1 of the staged page load: the raw candidate sequences (no stock/advice) for instant feedback. */
    @GetMapping("/candidates/sequences")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE','VARIANCE')")
    public ResponseEntity<?> candidatesSequences(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer shift) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        if (date == null) { date = slot.date; }
        if (shift == null) { shift = slot.shift; }
        if (shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().body("shift must be 1, 2 or 3");
        }
        return ResponseEntity.ok(logisticsReleaseService.sequencesPreview(date, shift));
    }

    /** Stage 3 of the staged page load: magasin (R100) stock for several short materials at once. */
    @GetMapping("/candidates/magasin")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE','VARIANCE')")
    public ResponseEntity<?> candidatesMagasin(@RequestParam(required = false) String materials) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        List<String> list = (materials == null || materials.trim().isEmpty())
                ? Collections.emptyList()
                : Arrays.asList(materials.split(","));
        return ResponseEntity.ok(logisticsReleaseService.magasinDetailBatch(list));
    }

    @PostMapping("/commit")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE','VARIANCE')")
    public ResponseEntity<?> commit(@RequestBody CommitRequest request, Authentication authentication) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        LocalDate date = request != null && request.date != null ? request.date : slot.date;
        Integer shift = request != null && request.shift != null ? request.shift : slot.shift;
        if (shift == null || shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().body("shift must be 1, 2 or 3");
        }
        List<String> sequences = request != null && request.sequences != null
                ? request.sequences
                : Collections.emptyList();
        Map<String, Object> result = logisticsReleaseService.commit(
                date, shift, sequences, authentication != null ? authentication.getName() : null);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/recap")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE','VARIANCE')")
    public ResponseEntity<?> recap(@RequestBody RecapRequest request) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        List<String> sequences = request != null && request.sequences != null
                ? request.sequences
                : Collections.emptyList();
        return ResponseEntity.ok(logisticsReleaseService.recap(sequences));
    }

    /** Reprint: the most recent persisted picklist snapshot for a (date, shift), same shape as commit(). */
    @GetMapping("/picklist/last")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE','VARIANCE')")
    public ResponseEntity<?> lastPicklist(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer shift) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        if (date == null) { date = slot.date; }
        if (shift == null) { shift = slot.shift; }
        if (shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().body("shift must be 1, 2 or 3");
        }
        return ResponseEntity.ok(logisticsReleaseService.lastPicklist(date, shift));
    }

    @GetMapping("/recap/magasin")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE','VARIANCE')")
    public ResponseEntity<?> recapMagasin(@RequestParam String material) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        if (material == null || material.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("material is required");
        }
        return ResponseEntity.ok(logisticsReleaseService.magasinDetail(material));
    }

    public static class CommitRequest {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        public LocalDate date;
        public Integer shift;
        public List<String> sequences;
    }

    public static class RecapRequest {
        public List<String> sequences;
    }
}
