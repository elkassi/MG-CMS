package com.lear.MGCMS.services.dispatcher;

import org.springframework.stereotype.Component;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;

/**
 * Small authorization helper used by the Phase 6 PdC role gate ("only
 * Process / Chef-Equipe can hit Recalculer") and by the admission gate
 * (Phase 9) to decide whether a user may override an admission block.
 *
 * <p>Centralized so role-name strings don't get typo'd across ten call
 * sites.</p>
 */
@Component
public class RoleGate {

    public static final String CHEF_DE_ZONE = "ROLE_CHEF_DE_ZONE";
    public static final String CHEF_EQUIPE  = "ROLE_CHEF_EQUIPE";
    public static final String PROCESS      = "ROLE_PROCESS";

    public boolean hasRole(User user, String role) {
        if (user == null || user.getRoles() == null || role == null) return false;
        for (Role r : user.getRoles()) {
            if (r != null && role.equals(r.getName())) return true;
        }
        return false;
    }

    public boolean canRecalculatePdC(User user) {
        return hasRole(user, PROCESS) || hasRole(user, CHEF_EQUIPE);
    }

    public boolean canDispatch(User user) {
        return hasRole(user, PROCESS);
    }

    public boolean canConfirmZone(User user) {
        return hasRole(user, CHEF_DE_ZONE) || hasRole(user, CHEF_EQUIPE);
    }

    public boolean canOverrideAdmission(User user) {
        return hasRole(user, PROCESS) || hasRole(user, CHEF_EQUIPE);
    }
}
