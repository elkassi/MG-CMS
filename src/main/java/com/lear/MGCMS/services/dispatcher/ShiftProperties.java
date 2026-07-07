package com.lear.MGCMS.services.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bound to {@code mgcms.shift.*}. Central place for shift timing knobs
 * so Phase 6 PdC overlays, Phase 7 autoTick windowing, and Phase 8 kiosk
 * "next serie" calculations all agree on what a shift is.
 *
 * <p>Known keys:</p>
 * <ul>
 *   <li>{@code mgcms.shift.durationMinutes} — length of a regular shift,
 *       default 480 minutes (8 h).</li>
 *   <li>{@code mgcms.shift.breakMinutes} — subtracted from the effective
 *       available minutes per shift (lunch + maintenance breaks).</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "mgcms.shift")
public class ShiftProperties {

    private int durationMinutes = 480;
    private int breakMinutes = 30;

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = Math.max(1, durationMinutes);
    }

    public int getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(int breakMinutes) {
        this.breakMinutes = Math.max(0, breakMinutes);
    }

    /** Effective working minutes per shift (duration minus breaks, never below 1). */
    public int effectiveMinutes() {
        return Math.max(1, durationMinutes - breakMinutes);
    }
}
