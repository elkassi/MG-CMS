package com.lear.MGCMS.services.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Phase 11 — configuration for the audit-table retention sweep.
 *
 * <p>Bound to {@code mgcms.retention.*}. Governs how long rows live in
 * {@code unassignable_serie} and {@code admission_blocked_audit} before
 * the daily cron deletes them. Defaults keep 7 days and the job itself
 * is off until ops flips {@code enabled=true} (and confirms both audit
 * tables exist — the dispatcher flag alone does not imply retention).</p>
 */
@Component
@ConfigurationProperties(prefix = "mgcms.retention")
public class RetentionProperties {

    /** Master kill-switch for {@link RetentionCronService}. */
    private boolean enabled = false;

    /** Days of history to keep. Rows older than {@code now() - days} are deleted. */
    private int days = 7;

    /** Daily cron expression used by {@link RetentionCronService#purgeAudits()}. */
    private String cron = "0 30 2 * * *";

    public boolean isEnabled()           { return enabled; }
    public void setEnabled(boolean e)    { this.enabled = e; }
    public int getDays()                 { return days; }
    public void setDays(int d)           { this.days = d; }
    public String getCron()              { return cron; }
    public void setCron(String c)        { this.cron = c; }
}
