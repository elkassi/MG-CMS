package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.lear.MGCMS.domain.Zone;

/**
 * Decides whether a sequence is locked to a specific zone (i.e. the engine
 * cannot move it and the UI should display it under "Locked").
 *
 * <h2>Two lock conditions</h2>
 * <ol>
 *   <li><b>Explicit:</b> {@code zoneAcceptanceStatus = 'ACCEPTED'} — the chef-de-zone
 *       has formally accepted the sequence. Lock zone = {@code dispatchedZone}.</li>
 *   <li><b>Implicit:</b> at least one serie has {@code statusCoupe != 'Waiting'}
 *       AND its {@code tableCoupe} resolves (via {@code ProductionTable}) to a
 *       Zone whose {@code category = STRICT}. Lock zone = that resolved STRICT
 *       zone (which may differ from the sequence's {@code dispatchedZone} —
 *       physical reality wins over the stale dispatch field).
 *       <ul>
 *         <li>Series whose table sits in a SHARED zone are <b>ignored</b> for
 *             implicit-lock detection (continue checking other series).</li>
 *         <li>If multiple series resolve to different STRICT zones, the
 *             tie-breaker is <b>most recent {@code dateDebutCoupe}</b> —
 *             where work is currently happening.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Pure function: no DB calls. The caller is responsible for batch-loading
 * the {@code tableNom → ZoneInfo} map once via
 * {@code ProductionTableRepository.findZoneInfoByTableNoms} and passing it in.</p>
 */
public final class LockResolver {

    /** Why a sequence is locked. */
    public enum LockReason {
        /** Chef-de-zone explicitly accepted the sequence. */
        EXPLICIT_ACCEPTED,
        /** A serie is being cut on a table sitting in a STRICT zone. */
        IMPLICIT_TABLE_STRICT
    }

    /** Lock decision returned by {@link #resolve}. */
    public static final class LockResult {
        private final String lockZoneNom;
        private final LockReason reason;
        private final String lockingSerieId;   // null for EXPLICIT
        private final String lockingTableNom;  // null for EXPLICIT
        private final String lockingStatusCoupe; // null for EXPLICIT

        public LockResult(String lockZoneNom, LockReason reason,
                          String lockingSerieId, String lockingTableNom,
                          String lockingStatusCoupe) {
            this.lockZoneNom = lockZoneNom;
            this.reason = reason;
            this.lockingSerieId = lockingSerieId;
            this.lockingTableNom = lockingTableNom;
            this.lockingStatusCoupe = lockingStatusCoupe;
        }

        public String getLockZoneNom()       { return lockZoneNom; }
        public LockReason getReason()        { return reason; }
        public String getLockingSerieId()    { return lockingSerieId; }
        public String getLockingTableNom()   { return lockingTableNom; }
        public String getLockingStatusCoupe(){ return lockingStatusCoupe; }
    }

    /** Per-serie input — only the fields the resolver actually reads. */
    public static final class SerieLockInput {
        public final String serieId;
        public final String statusCoupe;
        public final String tableCoupe;
        public final LocalDateTime dateDebutCoupe;

        public SerieLockInput(String serieId, String statusCoupe,
                              String tableCoupe, LocalDateTime dateDebutCoupe) {
            this.serieId = serieId;
            this.statusCoupe = statusCoupe;
            this.tableCoupe = tableCoupe;
            this.dateDebutCoupe = dateDebutCoupe;
        }
    }

    /** {@code tableNom → (zoneNom, category)} — precomputed by the caller. */
    public static final class TableZoneInfo {
        public final String zoneNom;
        public final Zone.Category category;
        public TableZoneInfo(String zoneNom, Zone.Category category) {
            this.zoneNom = zoneNom;
            this.category = category;
        }
    }

    /**
     * Decide if the sequence is locked, and if so to which zone.
     *
     * @param dispatchedZone        current value of {@code CuttingRequest.dispatchedZone}
     * @param zoneAcceptanceStatus  current value of {@code CuttingRequest.zoneAcceptanceStatus}
     * @param series                all series of the sequence (light projection)
     * @param tableToZone           {@code tableNom → TableZoneInfo} from
     *                              {@code ProductionTableRepository.findZoneInfoByTableNoms}
     * @return {@link Optional#empty()} when the sequence is not locked,
     *         otherwise the lock decision.
     */
    public static Optional<LockResult> resolve(
            String dispatchedZone,
            String zoneAcceptanceStatus,
            List<SerieLockInput> series,
            java.util.Map<String, TableZoneInfo> tableToZone) {

        // 1. Explicit lock — chef accepted.
        if ("ACCEPTED".equalsIgnoreCase(zoneAcceptanceStatus) && dispatchedZone != null) {
            return Optional.of(new LockResult(dispatchedZone, LockReason.EXPLICIT_ACCEPTED,
                    null, null, null));
        }

        // 2. Implicit lock — find serie(s) whose tableCoupe sits in a STRICT zone.
        if (series == null || series.isEmpty() || tableToZone == null) {
            return Optional.empty();
        }

        SerieLockInput winning = null;
        TableZoneInfo winningZone = null;
        for (SerieLockInput s : series) {
            if (s == null) continue;
            if (s.statusCoupe == null) continue;
            if ("Waiting".equalsIgnoreCase(s.statusCoupe.trim())) continue;
            if (s.tableCoupe == null) continue;

            TableZoneInfo zi = tableToZone.get(s.tableCoupe);
            if (zi == null) continue;
            if (zi.category != Zone.Category.STRICT) continue; // SHARED → ignore this serie

            if (winning == null) {
                winning = s;
                winningZone = zi;
            } else {
                // Tie-break: most recent dateDebutCoupe wins. Nulls lose.
                LocalDateTime cur = winning.dateDebutCoupe;
                LocalDateTime cand = s.dateDebutCoupe;
                if (cand != null && (cur == null || cand.isAfter(cur))) {
                    winning = s;
                    winningZone = zi;
                }
            }
        }

        if (winning == null) return Optional.empty();
        return Optional.of(new LockResult(
                winningZone.zoneNom, LockReason.IMPLICIT_TABLE_STRICT,
                winning.serieId, winning.tableCoupe, winning.statusCoupe));
    }

    private LockResolver() {} // static-only
}
