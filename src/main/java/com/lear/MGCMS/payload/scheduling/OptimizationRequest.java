package com.lear.MGCMS.payload.scheduling;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request payload for optimization
 */
public class OptimizationRequest {

    private List<String> sequenceIds;
    private List<String> machineNames;
    private String zoneName;
    private Integer maxBoxes;
    private List<String> lockedSeries;
    private OptimizationParams params;

    public static class OptimizationParams {
        private Double tolerancePercent = 10.0;
        private Integer maxIterations = 1000;
        private Integer timeoutSeconds = 30;
        private Boolean prioritizeSequenceCompletion = true;
        private Boolean groupByMaterial = true;
        private String priority = "balanced"; // balanced, speed, efficiency

        public Double getTolerancePercent() {
            return tolerancePercent;
        }

        public void setTolerancePercent(Double tolerancePercent) {
            this.tolerancePercent = tolerancePercent;
        }

        public Integer getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(Integer maxIterations) {
            this.maxIterations = maxIterations;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Boolean getPrioritizeSequenceCompletion() {
            return prioritizeSequenceCompletion;
        }

        public void setPrioritizeSequenceCompletion(Boolean prioritizeSequenceCompletion) {
            this.prioritizeSequenceCompletion = prioritizeSequenceCompletion;
        }

        public Boolean getGroupByMaterial() {
            return groupByMaterial;
        }

        public void setGroupByMaterial(Boolean groupByMaterial) {
            this.groupByMaterial = groupByMaterial;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }

    // Getters and Setters
    public List<String> getSequenceIds() {
        return sequenceIds;
    }

    public void setSequenceIds(List<String> sequenceIds) {
        this.sequenceIds = sequenceIds;
    }

    public List<String> getMachineNames() {
        return machineNames;
    }

    public void setMachineNames(List<String> machineNames) {
        this.machineNames = machineNames;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public Integer getMaxBoxes() {
        return maxBoxes;
    }

    public void setMaxBoxes(Integer maxBoxes) {
        this.maxBoxes = maxBoxes;
    }

    public List<String> getLockedSeries() {
        return lockedSeries;
    }

    public void setLockedSeries(List<String> lockedSeries) {
        this.lockedSeries = lockedSeries;
    }

    public OptimizationParams getParams() {
        return params;
    }

    public void setParams(OptimizationParams params) {
        this.params = params;
    }
}
