package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Published by {@link SequenceDispatcherService#publish} once the chef
 * (process role) confirms a dispatch decision. Listeners include:
 *
 * <ul>
 *   <li>Chef-de-zone live page (Phase 10) — refreshes its inbox.</li>
 *   <li>Zone-aware ordonnancement engine (Phase 7) — schedules the newly
 *       accepted sequences into machine queues for the targeted zone.</li>
 * </ul>
 *
 * <p>Immutable; safe to pass around asynchronously.</p>
 */
public final class SequenceAcceptedEvent {

    private final String zoneNom;
    private final LocalDate date;
    private final int shift;
    private final String publishedByMatricule;
    private final List<String> sequences;

    public SequenceAcceptedEvent(String zoneNom, LocalDate date, int shift,
                                 String publishedByMatricule, List<String> sequences) {
        this.zoneNom = zoneNom;
        this.date = date;
        this.shift = shift;
        this.publishedByMatricule = publishedByMatricule;
        this.sequences = sequences == null ? Collections.emptyList()
                : Collections.unmodifiableList(sequences);
    }

    public String getZoneNom()             { return zoneNom; }
    public LocalDate getDate()             { return date; }
    public int getShift()                  { return shift; }
    public String getPublishedByMatricule(){ return publishedByMatricule; }
    public List<String> getSequences()     { return sequences; }
}
