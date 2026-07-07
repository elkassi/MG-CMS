package com.lear.MGCMS.controller.dispatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.dispatcher.UserZoneRepository;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.UserZoneService;

/**
 * REST surface for {@link UserZoneService}.
 *
 * <p>Phase 5: admin (CHEF_EQUIPE / PROCESS) assigns and revokes chef-de-zone
 * links; any logged-in user can hit {@code /me} to read their zones and
 * default. All endpoints 404 when the dispatcher flag is off.</p>
 */
@RestController
@RequestMapping("/api/userZone")
public class UserZoneController {

    @Autowired private UserZoneService userZoneService;
    @Autowired private UserService userService;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private UserZoneRepository userZoneRepository;
    @Autowired private DispatcherProperties properties;

    /** Convenience for the React pages: the caller's zones + default. */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        User me = resolve(authentication);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Zone> zones = userZoneService.findZonesForUser(me);
        Zone def = userZoneService.findDefaultZoneForUser(me).orElse(null);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("matricule", me.getMatricule());
        out.put("defaultZone", def == null ? null : def.getNom());
        List<Map<String, Object>> list = new ArrayList<>();
        for (Zone z : zones) {
            Map<String, Object> zd = new LinkedHashMap<>();
            zd.put("nom", z.getNom());
            zd.put("category", z.getCategory());
            zd.put("active", z.isActive());
            zd.put("isDefault", def != null && def.getNom().equals(z.getNom()));
            list.add(zd);
        }
        out.put("zones", list);
        return ResponseEntity.ok(out);
    }

    /**
     * Paginated list of active user-zone assignments.
     * Joins with User and Zone for display columns.
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','PROCESS','CHEF_EQUIPE')")
    public ResponseEntity<?> all(
            @RequestParam(required = false) String matricule,
            @RequestParam(required = false) String zoneNom,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        Page<Object[]> result = userZoneRepository.findAllActiveJoined(
                matricule, zoneNom, PageRequest.of(page, size));

        List<Map<String, Object>> content = new ArrayList<>();
        for (Object[] row : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("matricule", row[0]);
            item.put("firstName", row[1]);
            item.put("lastName", row[2]);
            item.put("zoneNom", row[3]);
            item.put("category", row[4] != null ? row[4].toString() : null);
            item.put("isDefault", row[5]);
            item.put("assignedBy", row[6]);
            item.put("assignedAt", row[7]);
            content.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", content);
        out.put("page", result.getNumber());
        out.put("size", result.getSize());
        out.put("totalElements", result.getTotalElements());
        out.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(out);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','PROCESS')")
    public ResponseEntity<?> assign(@RequestBody AssignPayload payload,
                                    Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        if (payload == null || payload.matricule == null || payload.zoneNom == null) {
            return ResponseEntity.badRequest().body("matricule and zoneNom required");
        }
        User target = userService.findByUsername(payload.matricule);
        if (target == null) {
            // Some installs store matricule as the primary id — fall through and let
            // UserService return null. Caller surfaces 404 below.
        }
        if (target == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Zone zone = zoneRepository.findByObjId(payload.zoneNom);
        if (zone == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        User me = resolve(authentication);
        String assignedBy = me == null ? null : me.getMatricule();
        userZoneService.assign(target, zone, assignedBy, payload.isDefault);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE','PROCESS')")
    public ResponseEntity<?> revoke(@RequestBody AssignPayload payload) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        if (payload == null || payload.matricule == null || payload.zoneNom == null) {
            return ResponseEntity.badRequest().body("matricule and zoneNom required");
        }
        User target = userService.findByUsername(payload.matricule);
        if (target == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Zone zone = zoneRepository.findByObjId(payload.zoneNom);
        if (zone == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        userZoneService.revoke(target, zone);
        return ResponseEntity.ok().build();
    }

    /**
     * Atomically set one zone as default for a user, demoting all others.
     * Delegates to {@link UserZoneService#assign} which already handles the
     * atomic demotion invariant.
     */
    @PostMapping("/setDefault")
    @PreAuthorize("hasAnyRole('ADMIN','PROCESS','CHEF_EQUIPE')")
    public ResponseEntity<?> setDefault(@RequestBody SetDefaultPayload payload,
                                        Authentication authentication) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        if (payload == null || payload.matricule == null || payload.zoneNom == null) {
            return ResponseEntity.badRequest().body("matricule and zoneNom required");
        }
        User target = userService.findByUsername(payload.matricule);
        if (target == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Zone zone = zoneRepository.findByObjId(payload.zoneNom);
        if (zone == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        User me = resolve(authentication);
        String assignedBy = me == null ? null : me.getMatricule();
        userZoneService.assign(target, zone, assignedBy, true);
        return ResponseEntity.ok().build();
    }

    private User resolve(Authentication auth) {
        if (auth == null) return null;
        return userService.findByUsername(auth.getName());
    }

    public static final class AssignPayload {
        public String matricule;
        public String zoneNom;
        public boolean isDefault;
    }

    public static final class SetDefaultPayload {
        public String matricule;
        public String zoneNom;
    }
}
