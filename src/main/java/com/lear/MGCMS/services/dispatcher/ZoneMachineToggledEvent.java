package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;

/**
 * Mid-shift toggle of a single machine inside a confirmed zone. Smaller
 * scope than {@link ShiftZoneConfirmedEvent} — no bulk re-schedule, just
 * a targeted re-route for work stuck behind the now-down machine.
 */
public final class ZoneMachineToggledEvent {

    private final LocalDate date;
    private final int shift;
    private final String zoneNom;
    private final String machineNom;
    private final boolean nowUp;
    private final String toggledByMatricule;

    public ZoneMachineToggledEvent(LocalDate date, int shift, String zoneNom,
                                   String machineNom, boolean nowUp, String toggledByMatricule) {
        this.date = date;
        this.shift = shift;
        this.zoneNom = zoneNom;
        this.machineNom = machineNom;
        this.nowUp = nowUp;
        this.toggledByMatricule = toggledByMatricule;
    }

    public LocalDate getDate()               { return date; }
    public int getShift()                    { return shift; }
    public String getZoneNom()               { return zoneNom; }
    public String getMachineNom()            { return machineNom; }
    public boolean isNowUp()                 { return nowUp; }
    public String getToggledByMatricule()    { return toggledByMatricule; }
}
