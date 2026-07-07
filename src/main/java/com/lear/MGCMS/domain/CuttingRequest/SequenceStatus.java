package com.lear.MGCMS.domain.CuttingRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Canonical sequence-lifecycle vocabulary for
 * {@link CuttingRequest#getSequenceStatus()} (camelCase DB column
 * {@code sequenceStatus}). Replaces the legacy
 * {@code ACTIVE / PAUSED / WAITING_MATERIAL / COMPLETED} vocabulary.
 *
 * <p>Lifecycle:</p>
 * <ul>
 *   <li>{@link #IMPORTED} — set when a sequence is imported (the /preparation
 *       creation flow). Pre-release: excluded from production, but it is the
 *       picklist-candidate set.</li>
 *   <li>{@link #RELEASED} — logistics confirmed the picklist; a fixed
 *       {@link CuttingRequest#getReleaseZone() releaseZone} is persisted.</li>
 *   <li>{@link #STARTED} — a serie of the sequence has started spreading.</li>
 *   <li>{@link #COMPLETED} — every serie has {@code statusCoupe=Complete}
 *       (auto), or set manually by a chef.</li>
 *   <li>{@link #MATERIAL_MISSING} — the sequence cannot be spread completely
 *       with current stock; must stay visible to /processWorkbench + logistics.</li>
 *   <li>{@link #INCOMPLETE} — a chef removed an unfinishable sequence from
 *       production.</li>
 * </ul>
 *
 * <p>{@link #IN_PRODUCTION} ({@code RELEASED, STARTED, MATERIAL_MISSING}) is the
 * set the engine / workbench / dispatcher load. {@code IMPORTED} is pre-release;
 * {@code COMPLETED} / {@code INCOMPLETE} are out of production.</p>
 */
public final class SequenceStatus {

    private SequenceStatus() { }

    public static final String IMPORTED = "IMPORTED";
    public static final String RELEASED = "RELEASED";
    public static final String STARTED = "STARTED";
    public static final String COMPLETED = "COMPLETED";
    public static final String MATERIAL_MISSING = "MATERIAL_MISSING";
    public static final String INCOMPLETE = "INCOMPLETE";

    /**
     * Statuses the engine / workbench / dispatcher treat as live work.
     * {@code IMPORTED} is pre-release (picklist candidate, not in production);
     * {@code COMPLETED} / {@code INCOMPLETE} are terminal.
     */
    public static final List<String> IN_PRODUCTION =
            Collections.unmodifiableList(Arrays.asList(RELEASED, STARTED, MATERIAL_MISSING));
}
