package com.lear.MGCMS.services.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Phase 11 — flags for the self-heal sweep ({@link SelfHealService}).
 *
 * <p>Bound to {@code mgcms.engine.selfHeal.*}. Off by default — the
 * feature is intentionally dormant until ops has watched shadow logs
 * long enough to trust the heuristics. The thresholds below are the
 * tuning knobs ops may change without redeploying.</p>
 */
@Component
@ConfigurationProperties(prefix = "mgcms.engine.self-heal")
public class SelfHealProperties {

    /** Master kill-switch for {@link SelfHealService}. */
    private boolean enabled = false;

    /** Cron firing cadence. Defaults to every 10 minutes during work hours. */
    private String cron = "0 */10 6-22 * * *";

    /**
     * A {@link com.lear.MGCMS.domain.CuttingRequest.CuttingRequest} whose
     * {@code zoneAcceptanceStatus=PENDING} for longer than this is
     * considered stuck — the chef hasn't clicked Accept/Reject on
     * {@code ChefDeZonePage}. The sweep logs a warning; no destructive
     * action is taken until a separate enforce flag is added.
     */
    private int stuckPendingMinutes = 45;

    public boolean isEnabled()                 { return enabled; }
    public void setEnabled(boolean e)          { this.enabled = e; }
    public String getCron()                    { return cron; }
    public void setCron(String c)              { this.cron = c; }
    public int getStuckPendingMinutes()        { return stuckPendingMinutes; }
    public void setStuckPendingMinutes(int m)  { this.stuckPendingMinutes = m; }
}
