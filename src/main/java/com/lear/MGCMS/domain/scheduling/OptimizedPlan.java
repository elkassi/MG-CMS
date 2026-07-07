package com.lear.MGCMS.domain.scheduling;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;

@Entity
@Table(name = "optimized_plan")
public class OptimizedPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String planId;

    // JSON array of sequence IDs included in this plan
    @Lob
    @Column(columnDefinition = "TEXT")
    private String sequenceIds;

    // JSON object mapping series to machines with start/end times
    @Lob
    @Column(columnDefinition = "TEXT")
    private String machineAssignments;

    // JSON object for optimization parameters/prompt
    @Lob
    @Column(columnDefinition = "TEXT")
    private String optimizationParams;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime minStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime maxEndDate;

    // Maximum duration in minutes (max end - min start)
    private Double maxDurationMinutes;

    // Total cutting time in minutes
    private Double totalCuttingTime;

    // Number of iterations run
    private Integer iterationCount;

    // Current optimization score (lower is better)
    private Double optimizationScore;

    // Status: PENDING, RUNNING, COMPLETED, FAILED
    private String status;

    // Maximum boxes constraint (default 16 * number of machines)
    private Integer maxBoxes;

    // Number of machines used
    private Integer machineCount;

    // Comma-separated list of machine names
    private String machineNames;

    // Zone name
    private String zoneName;

    @ManyToOne
    private Zone zone;

    @ManyToOne
    private User createdBy;

    // Flag to indicate if this is the active plan for the zone
    private Boolean isActive = false;

    // Progress percentage during optimization
    private Integer progress = 0;

    // Error message if optimization failed
    @Column(length = 1000)
    private String errorMessage;

    public OptimizedPlan() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING";
        this.iterationCount = 0;
        this.progress = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getSequenceIds() {
        return sequenceIds;
    }

    public void setSequenceIds(String sequenceIds) {
        this.sequenceIds = sequenceIds;
    }

    public String getMachineAssignments() {
        return machineAssignments;
    }

    public void setMachineAssignments(String machineAssignments) {
        this.machineAssignments = machineAssignments;
    }

    public String getOptimizationParams() {
        return optimizationParams;
    }

    public void setOptimizationParams(String optimizationParams) {
        this.optimizationParams = optimizationParams;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public Double getTotalCuttingTime() {
        return totalCuttingTime;
    }

    public void setTotalCuttingTime(Double totalCuttingTime) {
        this.totalCuttingTime = totalCuttingTime;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMaxBoxes() {
        return maxBoxes;
    }

    public void setMaxBoxes(Integer maxBoxes) {
        this.maxBoxes = maxBoxes;
    }

    public Integer getMachineCount() {
        return machineCount;
    }

    public void setMachineCount(Integer machineCount) {
        this.machineCount = machineCount;
    }

    public String getMachineNames() {
        return machineNames;
    }

    public void setMachineNames(String machineNames) {
        this.machineNames = machineNames;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
