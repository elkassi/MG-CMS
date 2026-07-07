package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.dispatcher.AdmissionService;
import com.lear.MGCMS.services.dispatcher.AdmissionService.Decision;
import com.lear.MGCMS.services.dispatcher.SerieDispatchInfo;

/**
 * Phase 9 admission check.
 *
 * <p>Operator and kiosk both call {@code POST /api/admission/check} with
 * the minimal (serie, zone, date, shift) tuple. On a block the endpoint
 * returns either 409 (when {@code mgcms.admission.enforce=true}) or 200
 * with an advisory payload (shadow mode) — the audit row is written
 * either way.</p>
 *
 * <p>Authenticated by JWT (review M22): the kiosk is exempt from the
 * blanket security wall only on its {@code /api/kiosk/**} surface, so
 * any caller hitting {@code /api/admission/check} must present a valid
 * token. Roles allowed are the operator-facing roles plus PROCESS so
 * supervisors can run a check from the dispatch console. The
 * authenticated matricule is preferred over the payload's
 * {@code requestedByMatricule}-equivalent so the audit trail can't be
 * spoofed by a misbehaving client.</p>
 */
@RestController
@RequestMapping("/api/admission")
public class AdmissionController {

    @Autowired private AdmissionService admissionService;
    @Autowired private UserService userService;

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','IMPORTER','ADMIN')")
    public ResponseEntity<Map<String, Object>> check(@RequestBody CheckPayload payload,
                                                     Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // Belt-and-braces: @PreAuthorize already rejects anonymous
            // calls, but enforce here too so audit-trail integrity does
            // not depend on a single annotation being correct.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    singleton("error", "authentication required"));
        }
        if (payload == null || payload.serieId == null || payload.zoneNom == null
                || payload.date == null) {
            return ResponseEntity.badRequest().body(
                    singleton("error", "serieId, zoneNom, date, shift required"));
        }
        User u = userService.findByUsername(authentication.getName());
        String matricule = u == null ? authentication.getName() : u.getMatricule();
        SerieDispatchInfo s = new SerieDispatchInfo(
                payload.serieId, payload.sequenceId, payload.machineType,
                null, payload.nbrCouche, payload.placement);
        Decision d = admissionService.check(s, payload.zoneNom, payload.date, payload.shift, matricule);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allowed", d.isAllowed());
        body.put("reason",  d.getReason() == null ? null : d.getReason().name());
        body.put("detail",  d.getDetail());
        body.put("enforce", admissionService.isEnforceActive());
        if (d.isAllowed()) return ResponseEntity.ok(body);
        return admissionService.isEnforceActive()
                ? ResponseEntity.status(HttpStatus.CONFLICT).body(body)
                : ResponseEntity.ok(body);
    }

    private static Map<String, Object> singleton(String k, Object v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k, v);
        return m;
    }

    public static final class CheckPayload {
        public String serieId;
        public String sequenceId;
        public String zoneNom;
        public String machineType;       // SerieDispatchInfo.machine = type NAME
        public Integer nbrCouche;
        public String placement;
        public LocalDate date;
        public int shift;
    }
}
