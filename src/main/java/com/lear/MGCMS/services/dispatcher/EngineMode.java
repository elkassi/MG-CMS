package com.lear.MGCMS.services.dispatcher;

/**
 * Execution mode for the {@link ContinuousDispatchOptimizerService}.
 */
public enum EngineMode {
    CONTINUOUS,
    FIXED_DURATION,
    /** Switches between {@link EnginePhase#DISPATCH} and
     *  {@link EnginePhase#ORDONNANCEMENT} every cycle. */
    ALTERNATING
}
