package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Fired when a chef-de-zone confirms (or re-confirms) a (date, shift, zone)
 * triple. Listeners include the Phase 7 ordonnancement engine — a newly
 * confirmed zone can finally be scheduled into.
 */
public final class ShiftZoneConfirmedEvent {

    private final LocalDate date;
    private final int shift;
    private final String zoneNom;
    private final String confirmedByMatricule;
    private final List<String> upMachineNoms;

    public ShiftZoneConfirmedEvent(LocalDate date, int shift, String zoneNom,
                                   String confirmedByMatricule, List<String> upMachineNoms) {
        this.date = date;
        this.shift = shift;
        this.zoneNom = zoneNom;
        this.confirmedByMatricule = confirmedByMatricule;
        this.upMachineNoms = upMachineNoms == null ? Collections.emptyList()
                : Collections.unmodifiableList(upMachineNoms);
    }

    public LocalDate getDate()                 { return date; }
    public int getShift()                      { return shift; }
    public String getZoneNom()                 { return zoneNom; }
    public String getConfirmedByMatricule()    { return confirmedByMatricule; }
    public List<String> getUpMachineNoms()     { return upMachineNoms; }
}
