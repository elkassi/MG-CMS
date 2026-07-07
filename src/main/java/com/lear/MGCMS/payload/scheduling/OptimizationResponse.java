package com.lear.MGCMS.payload.scheduling;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response payload for optimization result
 */
public class OptimizationResponse {

    private String planId;
    private String status;
    private Integer progress;
    private Integer iterationCount;
    private Double optimizationScore;
    private LocalDateTime minStartDate;
    private LocalDateTime maxEndDate;
    private Double maxDurationMinutes;
    private Double maxDurationHours;
    private Double totalCuttingTime;
    private List<SequenceSummary> sequenceSummaries;
    private List<SeriesAssignment> assignments;
    private Map<String, List<SeriesAssignment>> assignmentsByMachine;
    private String errorMessage;

    public static class SequenceSummary {
        private String sequenceId;
        private String modele;
        private LocalDateTime minStartDate;
        private LocalDateTime maxEndDate;
        private Double durationHours;
        private Integer seriesCount;
        private Double totalCuttingMinutes;
        private Integer boxCount;
        private Double durationPerBox;
        private Double durationMinutes;

        public String getSequenceId() {
            return sequenceId;
        }

        public void setSequenceId(String sequenceId) {
            this.sequenceId = sequenceId;
        }

        public String getModele() {
            return modele;
        }

        public void setModele(String modele) {
            this.modele = modele;
        }

        public LocalDateTime getMinStartDate() {
            return minStartDate;
        }

        public void setMinStartDate(LocalDateTime minStartDate) {
            this.minStartDate = minStartDate;
        }

        public LocalDateTime getMaxEndDate() {
            return maxEndDate;
        }

        public void setMaxEndDate(LocalDateTime maxEndDate) {
            this.maxEndDate = maxEndDate;
        }

        public Double getDurationHours() {
            return durationHours;
        }

        public void setDurationHours(Double durationHours) {
            this.durationHours = durationHours;
        }

        public Integer getSeriesCount() {
            return seriesCount;
        }

        public void setSeriesCount(Integer seriesCount) {
            this.seriesCount = seriesCount;
        }

        public Double getTotalCuttingMinutes() {
            return totalCuttingMinutes;
        }

        public void setTotalCuttingMinutes(Double totalCuttingMinutes) {
            this.totalCuttingMinutes = totalCuttingMinutes;
        }

        public Integer getBoxCount() {
            return boxCount;
        }

        public void setBoxCount(Integer boxCount) {
            this.boxCount = boxCount;
        }

        public Double getDurationPerBox() {
            return durationPerBox;
        }

        public void setDurationPerBox(Double durationPerBox) {
            this.durationPerBox = durationPerBox;
        }

        public Double getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Double durationMinutes) {
            this.durationMinutes = durationMinutes;
        }
    }

    public static class SeriesAssignment {
        private String serieId;
        private String sequenceId;
        private String machineName;
        private LocalDateTime scheduledStart;
        private LocalDateTime scheduledEnd;
        private Double cuttingDurationMinutes;
        private Boolean isLocked;
        private String movementNote;
        private String partNumberMaterial;
        private String placement;
        private Integer orderOnMachine;

        public String getSerieId() {
            return serieId;
        }

        public void setSerieId(String serieId) {
            this.serieId = serieId;
        }

        public String getSequenceId() {
            return sequenceId;
        }

        public void setSequenceId(String sequenceId) {
            this.sequenceId = sequenceId;
        }

        public String getMachineName() {
            return machineName;
        }

        public void setMachineName(String machineName) {
            this.machineName = machineName;
        }

        public LocalDateTime getScheduledStart() {
            return scheduledStart;
        }

        public void setScheduledStart(LocalDateTime scheduledStart) {
            this.scheduledStart = scheduledStart;
        }

        public LocalDateTime getScheduledEnd() {
            return scheduledEnd;
        }

        public void setScheduledEnd(LocalDateTime scheduledEnd) {
            this.scheduledEnd = scheduledEnd;
        }

        public Double getCuttingDurationMinutes() {
            return cuttingDurationMinutes;
        }

        public void setCuttingDurationMinutes(Double cuttingDurationMinutes) {
            this.cuttingDurationMinutes = cuttingDurationMinutes;
        }

        public Boolean getIsLocked() {
            return isLocked;
        }

        public void setIsLocked(Boolean isLocked) {
            this.isLocked = isLocked;
        }

        public String getMovementNote() {
            return movementNote;
        }

        public void setMovementNote(String movementNote) {
            this.movementNote = movementNote;
        }

        public String getPartNumberMaterial() {
            return partNumberMaterial;
        }

        public void setPartNumberMaterial(String partNumberMaterial) {
            this.partNumberMaterial = partNumberMaterial;
        }

        public String getPlacement() {
            return placement;
        }

        public void setPlacement(String placement) {
            this.placement = placement;
        }

        public Integer getOrderOnMachine() {
            return orderOnMachine;
        }

        public void setOrderOnMachine(Integer orderOnMachine) {
            this.orderOnMachine = orderOnMachine;
        }
    }

    // Getters and Setters
    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(Integer iterationCount) {
        this.iterationCount = iterationCount;
    }

    public Double getOptimizationScore() {
        return optimizationScore;
    }

    public void setOptimizationScore(Double optimizationScore) {
        this.optimizationScore = optimizationScore;
    }

    public LocalDateTime getMinStartDate() {
        return minStartDate;
    }

    public void setMinStartDate(LocalDateTime minStartDate) {
        this.minStartDate = minStartDate;
    }

    public LocalDateTime getMaxEndDate() {
        return maxEndDate;
    }

    public void setMaxEndDate(LocalDateTime maxEndDate) {
        this.maxEndDate = maxEndDate;
    }

    public Double getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public void setMaxDurationMinutes(Double maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public Double getMaxDurationHours() {
        return maxDurationHours;
    }

    public void setMaxDurationHours(Double maxDurationHours) {
        this.maxDurationHours = maxDurationHours;
    }

    public Double getTotalCuttingTime() {
        return totalCuttingTime;
    }

    public void setTotalCuttingTime(Double totalCuttingTime) {
        this.totalCuttingTime = totalCuttingTime;
    }

    public List<SequenceSummary> getSequenceSummaries() {
        return sequenceSummaries;
    }

    public void setSequenceSummaries(List<SequenceSummary> sequenceSummaries) {
        this.sequenceSummaries = sequenceSummaries;
    }

    public List<SeriesAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<SeriesAssignment> assignments) {
        this.assignments = assignments;
    }

    public Map<String, List<SeriesAssignment>> getAssignmentsByMachine() {
        return assignmentsByMachine;
    }

    public void setAssignmentsByMachine(Map<String, List<SeriesAssignment>> assignmentsByMachine) {
        this.assignmentsByMachine = assignmentsByMachine;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
