package com.lear.MGCMS.services.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bound to {@code mgcms.admission.*}. The enforce flag decides whether
 * {@code AdmissionService} returns 409 CONFLICT on a block or merely
 * logs-and-allows. Ops can stage the rollout: run in shadow mode for
 * a week, inspect {@code admission_blocked_audit}, then flip to enforce.
 */
@Component
@ConfigurationProperties(prefix = "mgcms.admission")
public class AdmissionProperties {

    private boolean enforce = false;

    public boolean isEnforce() { return enforce; }
    public void setEnforce(boolean enforce) { this.enforce = enforce; }
}
