package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.dispatcher.UserZone;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.dispatcher.UserZoneRepository;

/**
 * Thin wrapper over {@link UserZoneRepository} that applies the role-aware
 * rules every caller needs:
 *
 * <ul>
 *   <li>{@code ROLE_CHEF_DE_ZONE} — sees only the zones with a {@link UserZone}
 *       row explicitly assigning them.</li>
 *   <li>{@code ROLE_CHEF_EQUIPE} — sees <em>every</em> active zone, regardless
 *       of whether an explicit assignment exists (per Phase 3 spec).</li>
 *   <li>{@code ROLE_PROCESS} — treated like {@code ROLE_CHEF_EQUIPE} for
 *       read-through; write operations are restricted by the controller layer.</li>
 * </ul>
 *
 * <p>Write paths ({@link #assign}, {@link #revoke}) soft-delete via
 * {@code revokedAt} so the audit trail survives. A follow-up {@code assign}
 * re-animates a revoked row rather than creating a duplicate (the
 * {@code (user_id, zone_nom)} unique constraint enforces this).</p>
 */
@Service
public class UserZoneService {

    static final String ROLE_CHEF_EQUIPE = "ROLE_CHEF_EQUIPE";
    static final String ROLE_PROCESS     = "ROLE_PROCESS";

    @Autowired
    private UserZoneRepository userZoneRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    /**
     * Active zones this user owns. For {@code ROLE_CHEF_EQUIPE} and
     * {@code ROLE_PROCESS}, returns <em>every</em> active zone.
     */
    @Transactional(readOnly = true)
    public List<Zone> findZonesForUser(User user) {
        if (user == null) return List.of();
        if (hasRole(user, ROLE_CHEF_EQUIPE) || hasRole(user, ROLE_PROCESS)) {
            return zoneRepository.findAllActive();
        }
        List<UserZone> links = userZoneRepository.findActiveByUserMatricule(user.getMatricule());
        List<Zone> zones = new ArrayList<>(links.size());
        for (UserZone uz : links) {
            Zone z = uz.getZone();
            if (z != null && z.isActive()) zones.add(z);
        }
        return zones;
    }

    /**
     * The user's default zone, if any. Chef-equipe users may not have
     * explicit links — in that case this returns empty (the UI typically
     * lands them on a zone picker).
     */
    @Transactional(readOnly = true)
    public Optional<Zone> findDefaultZoneForUser(User user) {
        if (user == null) return Optional.empty();
        List<UserZone> defaults = userZoneRepository.findDefaultsForUser(user.getMatricule());
        for (UserZone uz : defaults) {
            Zone z = uz.getZone();
            if (z != null && z.isActive()) return Optional.of(z);
        }
        return Optional.empty();
    }

    /**
     * Idempotent assign. If a (user, zone) link already exists — active or
     * revoked — it is re-activated and re-flagged as default if requested.
     * This is the only method that can promote a zone to {@code isDefault=true}
     * for a user; it demotes any other default links to keep invariant
     * "at most one default per user" outside of transient windows.
     */
    @Transactional
    public void assign(User user, Zone zone, String assignedByMatricule, boolean isDefault) {
        Objects.requireNonNull(user,  "user");
        Objects.requireNonNull(zone,  "zone");

        Optional<UserZone> existing = userZoneRepository.findByUserAndZone(
                user.getMatricule(), zone.getNom());

        UserZone uz = existing.orElseGet(() -> new UserZone(user, zone, false, assignedByMatricule));
        uz.setRevokedAt(null);
        if (assignedByMatricule != null) uz.setAssignedBy(assignedByMatricule);
        if (uz.getAssignedAt() == null) uz.setAssignedAt(LocalDateTime.now());
        uz.setDefault(isDefault);
        userZoneRepository.save(uz);

        if (isDefault) {
            // Demote any other defaults for this user.
            List<UserZone> others = userZoneRepository.findDefaultsForUser(user.getMatricule());
            for (UserZone other : others) {
                if (!Objects.equals(other.getId(), uz.getId()) && other.isDefault()) {
                    other.setDefault(false);
                    userZoneRepository.save(other);
                }
            }
        }
    }

    /**
     * Soft-revoke. The row is kept with a {@code revokedAt} stamp so
     * historical who-confirmed-what queries still work.
     */
    @Transactional
    public void revoke(User user, Zone zone) {
        if (user == null || zone == null) return;
        userZoneRepository.findByUserAndZone(user.getMatricule(), zone.getNom())
                .ifPresent(uz -> {
                    if (uz.getRevokedAt() == null) {
                        uz.setRevokedAt(LocalDateTime.now());
                        uz.setDefault(false);
                        userZoneRepository.save(uz);
                    }
                });
    }

    /**
     * Authorization check for admission / confirmation flows. Chef-equipe
     * and process roles always pass; a zone-level chef must have an active
     * link.
     */
    @Transactional(readOnly = true)
    public boolean userOwnsZone(User user, Zone zone) {
        if (user == null || zone == null) return false;
        if (hasRole(user, ROLE_CHEF_EQUIPE) || hasRole(user, ROLE_PROCESS)) return true;
        return userZoneRepository
                .findByUserAndZone(user.getMatricule(), zone.getNom())
                .map(uz -> uz.getRevokedAt() == null)
                .orElse(false);
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private static boolean hasRole(User user, String name) {
        if (user == null || user.getRoles() == null) return false;
        for (Role r : user.getRoles()) {
            if (r != null && name.equals(r.getName())) return true;
        }
        return false;
    }
}
