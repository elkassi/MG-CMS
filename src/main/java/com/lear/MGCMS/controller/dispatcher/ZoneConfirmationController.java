package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.dispatcher.ShiftZoneConfirmation;
import com.lear.MGCMS.domain.dispatcher.ShiftZoneConfirmationMachine;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.ShiftZoneConfirmationService;

/**
 * REST surface for chef-de-zone confirmation (Phase 5).
 *
 * <p>All endpoints 404 when {@code mgcms.dispatcher.enabled=false} so the
 * chef-de-zone React pages degrade silently on deployments where the
 * subsystem is still behind a flag.</p>
 *
 * <p>Authorization: {@code /confirm} and {@code /toggle} require
 * {@code ROLE_CHEF_DE_ZONE} or {@code ROLE_CHEF_EQUIPE}. The read endpoints
 * are open to any authenticated user so the Process dashboard can show
 * cross-zone state.</p>
 */
@RestController
@RequestMapping("/api/zone")
public class ZoneConfirmationController {

    @Autowired private ShiftZoneConfirmationService confirmationService;
    @Autowired private DispatcherProperties properties;
    @Autowired private UserService userService;

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('CHEF_DE_ZONE','CHEF_EQUIPE')")
    public ResponseEntity<?> confirm(@RequestBody ConfirmPayload payload,
                                     Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        if (payload == null || payload.date == null || payload.zoneNom == null) {
            return ResponseEntity.badRequest().body("date and zoneNom required");
        }
        User me = resolveUser(authentication);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<String> up = payload.upMachineNoms == null
                ? Collections.emptyList() : payload.upMachineNoms;
        ShiftZoneConfirmation saved =
                confirmationService.confirm(payload.date, payload.shift, payload.zoneNom, up, me);
        return ResponseEntity.ok(toDto(saved));
    }

    @PostMapping("/toggleMachine")
    @PreAuthorize("hasAnyRole('CHEF_DE_ZONE','CHEF_EQUIPE')")
    public ResponseEntity<?> toggleMachine(@RequestBody ToggleMachinePayload payload,
                                           Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        if (payload == null || payload.date == null || payload.zoneNom == null
                || payload.machineNom == null) {
            return ResponseEntity.badRequest().body("date, zoneNom, machineNom required");
        }
        User me = resolveUser(authentication);
        ShiftZoneConfirmationMachine m = confirmationService.toggleMachine(
                payload.date, payload.shift, payload.zoneNom,
                payload.machineNom, payload.nowUp, me);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("machineNom", m.getMachineNom());
        out.put("isUp", m.isUp());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/confirmation/{zoneNom}")
    public ResponseEntity<?> find(@PathVariable String zoneNom,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestParam int shift) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        return confirmationService.find(date, shift, zoneNom)
                .<ResponseEntity<?>>map(v -> ResponseEntity.ok(toDto(v)))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Confirmation if it exists, otherwise a pre-filled preview synthesised
     * from {@code EtatMachineHistorique} (M or null = up). Always returns
     * 200 with the full machine list, plus a {@code canWrite} flag derived
     * from the caller's roles so the UI can disable the Confirmer button
     * for read-only viewers (PROCESS / ADMIN).
     */
    @GetMapping("/confirmation/{zoneNom}/orPreview")
    public ResponseEntity<?> findOrPreview(@PathVariable String zoneNom,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                           @RequestParam int shift,
                                           Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        Map<String, Object> body = new LinkedHashMap<>(
                confirmationService.findOrPreview(date, shift, zoneNom));
        body.put("canWrite", hasAnyAuthority(authentication, "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE"));
        return ResponseEntity.ok(body);
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

    @GetMapping("/confirmations")
    public ResponseEntity<?> findForShift(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (ShiftZoneConfirmation szc : confirmationService.findAllForShift(date, shift)) {
            out.add(toDto(szc));
        }
        return ResponseEntity.ok(out);
    }

    // ------------------------------------------------------------------
    // Helpers / DTOs
    // ------------------------------------------------------------------

    private User resolveUser(Authentication auth) {
        if (auth == null) return null;
        return userService.findByUsername(auth.getName());
    }

    private Map<String, Object> toDto(ShiftZoneConfirmation szc) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", szc.getId());
        out.put("date", szc.getDateProduction());
        out.put("shift", szc.getShiftNumber());
        out.put("zoneNom", szc.getZone() == null ? null : szc.getZone().getNom());
        out.put("confirmedAt", szc.getConfirmedAt());
        out.put("confirmedByMatricule",
                szc.getConfirmedBy() == null ? null : szc.getConfirmedBy().getMatricule());
        List<Map<String, Object>> machines = new java.util.ArrayList<>();
        for (ShiftZoneConfirmationMachine m : szc.getMachines()) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("machineNom", m.getMachineNom());
            mm.put("isUp", m.isUp());
            machines.add(mm);
        }
        out.put("machines", machines);
        return out;
    }

    public static final class ConfirmPayload {
        public LocalDate date;
        public int shift;
        public String zoneNom;
        public List<String> upMachineNoms;
    }

    public static final class ToggleMachinePayload {
        public LocalDate date;
        public int shift;
        public String zoneNom;
        public String machineNom;
        public boolean nowUp;
    }
}
