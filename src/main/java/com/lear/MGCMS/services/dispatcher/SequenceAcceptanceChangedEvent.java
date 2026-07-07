package com.lear.MGCMS.services.dispatcher;

/**
 * Published by {@link SequenceDispatcherService#setAcceptance} when a
 * chef-de-zone (or chef-d'équipe) accepts or rejects a sequence routed
 * to their zone (Phase 10).
 *
 * <p>Listeners react to acceptance flips so the operator-facing
 * surfaces stay in sync — typically by bumping the kiosk version for
 * every machine in the affected zone so a rejected sequence stops
 * being shown as next-up.</p>
 *
 * <p>Immutable; safe to pass around asynchronously.</p>
 */
public final class SequenceAcceptanceChangedEvent {

    private final String sequence;
    private final String zoneNom;
    /** {@code ACCEPTED} or {@code REJECTED}. */
    private final String status;
    private final String changedByMatricule;

    public SequenceAcceptanceChangedEvent(String sequence, String zoneNom,
                                          String status, String changedByMatricule) {
        this.sequence = sequence;
        this.zoneNom = zoneNom;
        this.status = status;
        this.changedByMatricule = changedByMatricule;
    }

    public String getSequence()           { return sequence; }
    public String getZoneNom()            { return zoneNom; }
    public String getStatus()             { return status; }
    public String getChangedByMatricule() { return changedByMatricule; }
}
