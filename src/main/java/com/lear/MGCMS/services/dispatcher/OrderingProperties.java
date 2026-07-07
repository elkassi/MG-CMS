package com.lear.MGCMS.services.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Feature flag for the in-zone serie ordering strategy.
 *
 * <ul>
 *   <li>{@code legacy} — preserves the original dueDate → dueShift → serie sort.</li>
 *   <li>{@code v2} — box-duration-aware ordering that respects
 *       matelassage→coupe precedence and machine-type capacity constraints.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "cms.dispatcher")
public class OrderingProperties {

    private String ordering = "v2";

    public String getOrdering() {
        return ordering;
    }

    public void setOrdering(String ordering) {
        this.ordering = ordering;
    }

    public boolean isLegacy() {
        return "legacy".equalsIgnoreCase(ordering);
    }

    public boolean isV2() {
        return !isLegacy();
    }
}
