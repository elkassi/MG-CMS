package com.lear.MGCMS.services.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable thresholds for the (machineType, zone) load heatmap and
 * the equilibre summary (Contract C7 — MASTER_SCHEDULING_VISION_v3.md
 * §4.1, §19.2).
 *
 * <p>Drives:</p>
 * <ul>
 *   <li>Heatmap cell colours (green &lt; warning, amber &lt; danger,
 *       red ≥ danger).</li>
 *   <li>Equilibre badges: intra-zone spread (max - min loadPct across
 *       machine types inside one STRICT zone) and inter-zone spread
 *       (max - min loadPct across STRICT zones).</li>
 * </ul>
 *
 * <p>Defaults are mild — Process can tighten via
 * {@code application.properties} without redeploy.</p>
 */
@Component
@ConfigurationProperties(prefix = "mgcms.zoneload")
public class ZoneLoadProperties {

    /** Below this %, cell renders green. */
    private double warningThresholdPct = 80.0;

    /** Below this %, cell renders amber; ≥ this %, red. */
    private double dangerThresholdPct = 100.0;

    /** Intra-zone spread (max - min loadPct across types inside a zone) ≤ this → green. */
    private double intraZoneSpreadTargetPct = 15.0;

    /** Intra-zone spread ≤ this → amber; otherwise red. */
    private double intraZoneSpreadWarningPct = 25.0;

    /** Inter-zone spread (max - min loadPct across STRICT zones) ≤ this → green. */
    private double interZoneSpreadTargetPct = 15.0;

    /** Inter-zone spread ≤ this → amber; otherwise red. */
    private double interZoneSpreadWarningPct = 30.0;

    /** Master flag — toggle the heatmap panel without removing the controller. */
    private boolean heatmapEnabled = true;

    public double getWarningThresholdPct() { return warningThresholdPct; }
    public void setWarningThresholdPct(double v) { this.warningThresholdPct = v; }

    public double getDangerThresholdPct() { return dangerThresholdPct; }
    public void setDangerThresholdPct(double v) { this.dangerThresholdPct = v; }

    public double getIntraZoneSpreadTargetPct() { return intraZoneSpreadTargetPct; }
    public void setIntraZoneSpreadTargetPct(double v) { this.intraZoneSpreadTargetPct = v; }

    public double getIntraZoneSpreadWarningPct() { return intraZoneSpreadWarningPct; }
    public void setIntraZoneSpreadWarningPct(double v) { this.intraZoneSpreadWarningPct = v; }

    public double getInterZoneSpreadTargetPct() { return interZoneSpreadTargetPct; }
    public void setInterZoneSpreadTargetPct(double v) { this.interZoneSpreadTargetPct = v; }

    public double getInterZoneSpreadWarningPct() { return interZoneSpreadWarningPct; }
    public void setInterZoneSpreadWarningPct(double v) { this.interZoneSpreadWarningPct = v; }

    public boolean isHeatmapEnabled() { return heatmapEnabled; }
    public void setHeatmapEnabled(boolean v) { this.heatmapEnabled = v; }
}
