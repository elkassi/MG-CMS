package com.lear.MGCMS.services.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bound to the {@code mgcms.dispatcher.*} block in
 * {@code application.properties}. Exposed as a bean so controllers and
 * future listeners can gate on {@link #isEnabled()} without having to
 * read the property lazily.
 *
 * <p>Known keys (kept in sync with Phase 11's feature-flag inventory):</p>
 * <ul>
 *   <li>{@code mgcms.dispatcher.enabled} — master switch for the whole
 *       dispatcher subsystem (controller, listeners). Defaults to
 *       {@code false} so new deployments are silent until ops flips it.</li>
 *   <li>{@code mgcms.dispatcher.allow-unconfirmed-zones} — when true, the
 *       dispatcher can preview/publish against the plant's zone/machine
 *       master even if chef confirmations have not been captured yet.
 *       Useful for local shadow routing on historical copies.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "mgcms.dispatcher")
public class DispatcherProperties {

    private boolean enabled = false;
    private boolean allowUnconfirmedZones = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowUnconfirmedZones() {
        return allowUnconfirmedZones;
    }

    public void setAllowUnconfirmedZones(boolean allowUnconfirmedZones) {
        this.allowUnconfirmedZones = allowUnconfirmedZones;
    }
}
