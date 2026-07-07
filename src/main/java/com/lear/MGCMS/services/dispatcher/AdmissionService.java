package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.AdmissionBlockedAudit.ReasonCode;

/**
 * Phase 9 — last-mile admission gate used by the kiosk and the operator
 * REST surface right before a serie physically starts.
 *
 * <p>Asks the simpler question "can this serie legally run in this zone
 * for this shift, right now?" — by delegating to
 * {@link SerieZoneResolver} and requiring the resolution to land on the
 * zone the caller is claiming. Any rejection is written to
 * {@code admission_blocked_audit} regardless of whether
 * {@link AdmissionProperties#isEnforce()} is true, so the audit trail
 * exists even in shadow mode.</p>
 */
@Service
public class AdmissionService {

    @Autowired private SerieZoneResolver serieZoneResolver;
    @Autowired private AdmissionAuditService admissionAuditService;
    @Autowired private AdmissionProperties properties;

    /**
     * Result of an admission check. {@code allowed=false} paired with
     * {@link AdmissionProperties#isEnforce()} = true should make the
     * caller return 409 to the client.
     */
    public static final class Decision {
        private final boolean allowed;
        private final ReasonCode reason;
        private final String detail;

        public Decision(boolean allowed, ReasonCode reason, String detail) {
            this.allowed = allowed;
            this.reason = reason;
            this.detail = detail;
        }
        public static Decision allow() { return new Decision(true, null, null); }

        public boolean isAllowed()     { return allowed; }
        public ReasonCode getReason()  { return reason; }
        public String getDetail()      { return detail; }
    }

    /**
     * Check a serie against the current (date, shift, zoneNom). Always
     * writes an audit row on a block; callers decide whether the block
     * is blocking based on the {@code enforce} flag.
     */
    @Transactional
    public Decision check(SerieDispatchInfo serie, String zoneNom, LocalDate date, int shift,
                          String requestedByMatricule) {
        Objects.requireNonNull(serie, "serie");
        Objects.requireNonNull(zoneNom, "zoneNom");
        Objects.requireNonNull(date, "date");

        SerieZoneResolver.Resolution res = serieZoneResolver.resolve(serie, date, shift);
        if (!res.isAccepted()) {
            ReasonCode rc = mapReason(res.getFailureReason());
            String detail = String.format(
                    "serie=%s claimedZone=%s machineType=%s reason=%s",
                    serie.getSerieId(), zoneNom, serie.getMachine(), res.getFailureReason());
            block(serie, zoneNom, date, shift, rc, detail, requestedByMatricule);
            return new Decision(false, rc, detail);
        }
        if (!zoneNom.equalsIgnoreCase(res.getZone().getNom())) {
            String detail = String.format(
                    "serie=%s claimedZone=%s expectedZone=%s",
                    serie.getSerieId(), zoneNom, res.getZone().getNom());
            block(serie, zoneNom, date, shift, ReasonCode.OTHER, detail, requestedByMatricule);
            return new Decision(false, ReasonCode.OTHER, detail);
        }
        return Decision.allow();
    }

    /** Enforce flag — callers surface a 409 when this is true and {@link Decision#isAllowed()} is false. */
    public boolean isEnforceActive() {
        return properties.isEnforce();
    }

    private static ReasonCode mapReason(SerieZoneResolver.FailureReason fr) {
        if (fr == null) return ReasonCode.OTHER;
        switch (fr) {
            case NO_ZONE_ACCEPTING_TYPE:     return ReasonCode.NO_ZONE_ACCEPTING_TYPE;
            case ALL_ZONES_CLOSED_FOR_SHIFT: return ReasonCode.ALL_ZONES_CLOSED_FOR_SHIFT;
            case NO_ACTIVE_MACHINE_IN_ZONE:  return ReasonCode.NO_ACTIVE_MACHINE_IN_ZONE;
            default:                          return ReasonCode.OTHER;
        }
    }

    /**
     * Audit insertion is delegated to {@link AdmissionAuditService} so it
     * runs in its own {@code REQUIRES_NEW} transaction (review C3). A
     * transient audit-table failure no longer rolls back the operator's
     * admission decision.
     */
    private void block(SerieDispatchInfo serie, String zoneNom, LocalDate date, int shift,
                       ReasonCode rc, String detail, String requestedByMatricule) {
        admissionAuditService.recordBlock(serie, zoneNom, date, shift, rc, detail, requestedByMatricule);
    }
}
