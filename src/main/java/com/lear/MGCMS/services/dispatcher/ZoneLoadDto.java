package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Wire shape for the {@code /api/zoneLoad/matrix} response. Holds the
 * (zone, machineType) cells plus zone aggregates plus the equilibre
 * summary computed in one pass by {@link ZoneLoadService}.
 */
public final class ZoneLoadDto {

    private final LocalDate date;
    private final int shift;
    /** Distinct machine types observed (column order on the heatmap). */
    private final List<String> machineTypes;
    /** All (zone, type) cells. STRICT first, SHARED last; types preserve insertion order. */
    private final List<ZoneLoadCellDto> cells;
    /** Per-zone aggregates (sum of cells in the row). */
    private final List<ZoneLoadRowDto> rows;
    /** Header summary used by the equilibre badge at the top of the page. */
    private final EquilibreSummaryDto equilibre;
    /** Echo of the thresholds in use so the UI colours match server-side decisions. */
    private final ThresholdsDto thresholds;

    public ZoneLoadDto(LocalDate date, int shift, List<String> machineTypes,
                       List<ZoneLoadCellDto> cells, List<ZoneLoadRowDto> rows,
                       EquilibreSummaryDto equilibre, ThresholdsDto thresholds) {
        this.date = date;
        this.shift = shift;
        this.machineTypes = machineTypes;
        this.cells = cells;
        this.rows = rows;
        this.equilibre = equilibre;
        this.thresholds = thresholds;
    }

    public LocalDate getDate() { return date; }
    public int getShift() { return shift; }
    public List<String> getMachineTypes() { return machineTypes; }
    public List<ZoneLoadCellDto> getCells() { return cells; }
    public List<ZoneLoadRowDto> getRows() { return rows; }
    public EquilibreSummaryDto getEquilibre() { return equilibre; }
    public ThresholdsDto getThresholds() { return thresholds; }

    // -------------------------------------------------------------------

    /** One cell on the heatmap = (zone, machineType). */
    public static final class ZoneLoadCellDto {
        private final String zoneNom;
        private final String zoneCategory;     // STRICT | SHARED
        private final String machineType;
        private final double plannedMinutes;
        private final double baselineMinutes;
        private final double pendingMinutes;
        private final double capacityMinutes;
        private final double loadPct;
        private final int activeMachines;
        private final int sequencesCount;
        /** Why this cell is grey (no machines of that type in this zone). */
        private final boolean machinePresent;

        public ZoneLoadCellDto(String zoneNom, String zoneCategory, String machineType,
                               double plannedMinutes, double baselineMinutes, double pendingMinutes,
                               double capacityMinutes, double loadPct,
                               int activeMachines, int sequencesCount, boolean machinePresent) {
            this.zoneNom = zoneNom;
            this.zoneCategory = zoneCategory;
            this.machineType = machineType;
            this.plannedMinutes = plannedMinutes;
            this.baselineMinutes = baselineMinutes;
            this.pendingMinutes = pendingMinutes;
            this.capacityMinutes = capacityMinutes;
            this.loadPct = loadPct;
            this.activeMachines = activeMachines;
            this.sequencesCount = sequencesCount;
            this.machinePresent = machinePresent;
        }

        public String getZoneNom() { return zoneNom; }
        public String getZoneCategory() { return zoneCategory; }
        public String getMachineType() { return machineType; }
        public double getPlannedMinutes() { return plannedMinutes; }
        public double getBaselineMinutes() { return baselineMinutes; }
        public double getPendingMinutes() { return pendingMinutes; }
        public double getCapacityMinutes() { return capacityMinutes; }
        public double getLoadPct() { return loadPct; }
        public int getActiveMachines() { return activeMachines; }
        public int getSequencesCount() { return sequencesCount; }
        public boolean isMachinePresent() { return machinePresent; }
    }

    /** Per-zone aggregate row (sum across types). */
    public static final class ZoneLoadRowDto {
        private final String zoneNom;
        private final String zoneCategory;
        private final double plannedMinutes;
        private final double capacityMinutes;
        private final double loadPct;
        private final int activeMachines;
        private final int sequencesCount;
        /** Highest minus lowest loadPct across the cells in this row (intra-zone spread). */
        private final double intraSpreadPct;

        public ZoneLoadRowDto(String zoneNom, String zoneCategory,
                              double plannedMinutes, double capacityMinutes, double loadPct,
                              int activeMachines, int sequencesCount, double intraSpreadPct) {
            this.zoneNom = zoneNom;
            this.zoneCategory = zoneCategory;
            this.plannedMinutes = plannedMinutes;
            this.capacityMinutes = capacityMinutes;
            this.loadPct = loadPct;
            this.activeMachines = activeMachines;
            this.sequencesCount = sequencesCount;
            this.intraSpreadPct = intraSpreadPct;
        }

        public String getZoneNom() { return zoneNom; }
        public String getZoneCategory() { return zoneCategory; }
        public double getPlannedMinutes() { return plannedMinutes; }
        public double getCapacityMinutes() { return capacityMinutes; }
        public double getLoadPct() { return loadPct; }
        public int getActiveMachines() { return activeMachines; }
        public int getSequencesCount() { return sequencesCount; }
        public double getIntraSpreadPct() { return intraSpreadPct; }
    }

    /**
     * The headline answer to the user's "is the plant balanced?" question.
     * The UI renders two pills (intra and inter) coloured by the
     * configurable thresholds.
     */
    public static final class EquilibreSummaryDto {
        /** Worst intra-zone spread observed across STRICT zones (max - min loadPct). */
        private final double worstIntraSpreadPct;
        /** Which zone owns that worst spread — for the UI tooltip. */
        private final String worstIntraZone;
        /** Average intra-zone spread across STRICT zones (gives a smoother trend). */
        private final double avgIntraSpreadPct;
        /** Inter-zone spread = max(zoneLoadPct) - min(zoneLoadPct) over STRICT zones. */
        private final double interZoneSpreadPct;
        /** Hottest STRICT zone by total loadPct. */
        private final String hottestZone;
        /** Coolest STRICT zone by total loadPct. */
        private final String coolestZone;
        /** Status code per category — GREEN | AMBER | RED. */
        private final String intraStatus;
        private final String interStatus;
        /** Per-zone intra spread map for the row tooltip. */
        private final Map<String, Double> intraByZone;

        public EquilibreSummaryDto(double worstIntraSpreadPct, String worstIntraZone,
                                   double avgIntraSpreadPct, double interZoneSpreadPct,
                                   String hottestZone, String coolestZone,
                                   String intraStatus, String interStatus,
                                   Map<String, Double> intraByZone) {
            this.worstIntraSpreadPct = worstIntraSpreadPct;
            this.worstIntraZone = worstIntraZone;
            this.avgIntraSpreadPct = avgIntraSpreadPct;
            this.interZoneSpreadPct = interZoneSpreadPct;
            this.hottestZone = hottestZone;
            this.coolestZone = coolestZone;
            this.intraStatus = intraStatus;
            this.interStatus = interStatus;
            this.intraByZone = intraByZone;
        }

        public double getWorstIntraSpreadPct() { return worstIntraSpreadPct; }
        public String getWorstIntraZone() { return worstIntraZone; }
        public double getAvgIntraSpreadPct() { return avgIntraSpreadPct; }
        public double getInterZoneSpreadPct() { return interZoneSpreadPct; }
        public String getHottestZone() { return hottestZone; }
        public String getCoolestZone() { return coolestZone; }
        public String getIntraStatus() { return intraStatus; }
        public String getInterStatus() { return interStatus; }
        public Map<String, Double> getIntraByZone() { return intraByZone; }
    }

    /** Echoed thresholds so client-side colours match server-side classification. */
    public static final class ThresholdsDto {
        private final double warningThresholdPct;
        private final double dangerThresholdPct;
        private final double intraZoneSpreadTargetPct;
        private final double intraZoneSpreadWarningPct;
        private final double interZoneSpreadTargetPct;
        private final double interZoneSpreadWarningPct;

        public ThresholdsDto(double warningThresholdPct, double dangerThresholdPct,
                             double intraZoneSpreadTargetPct, double intraZoneSpreadWarningPct,
                             double interZoneSpreadTargetPct, double interZoneSpreadWarningPct) {
            this.warningThresholdPct = warningThresholdPct;
            this.dangerThresholdPct = dangerThresholdPct;
            this.intraZoneSpreadTargetPct = intraZoneSpreadTargetPct;
            this.intraZoneSpreadWarningPct = intraZoneSpreadWarningPct;
            this.interZoneSpreadTargetPct = interZoneSpreadTargetPct;
            this.interZoneSpreadWarningPct = interZoneSpreadWarningPct;
        }

        public double getWarningThresholdPct() { return warningThresholdPct; }
        public double getDangerThresholdPct() { return dangerThresholdPct; }
        public double getIntraZoneSpreadTargetPct() { return intraZoneSpreadTargetPct; }
        public double getIntraZoneSpreadWarningPct() { return intraZoneSpreadWarningPct; }
        public double getInterZoneSpreadTargetPct() { return interZoneSpreadTargetPct; }
        public double getInterZoneSpreadWarningPct() { return interZoneSpreadWarningPct; }
    }
}
