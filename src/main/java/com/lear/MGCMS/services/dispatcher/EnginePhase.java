package com.lear.MGCMS.services.dispatcher;

/**
 * Active phase for the {@link EngineMode#ALTERNATING} mode.
 *
 * <p>The engine switches between dispatching (zone-assignment optimization)
 * and ordonnancement (scheduling / timeline optimization) in a fixed
 * cadence so that both sub-problems get CPU time.</p>
 */
public enum EnginePhase {
    /** Optimizing zone assignments (sequence → zone). */
    DISPATCH,
    /** Optimizing scheduling / timeline within assigned zones. */
    ORDONNANCEMENT
}
