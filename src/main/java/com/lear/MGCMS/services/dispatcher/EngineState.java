package com.lear.MGCMS.services.dispatcher;

/**
 * Lifecycle states for the {@link ContinuousDispatchOptimizerService}.
 */
public enum EngineState {
    IDLE,
    WARMING,
    IMPROVING,
    PAUSED,
    STOPPED
}
