package com.lear.MGCMS.domain.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.Zone;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity to store sequence scheduling information
 * Includes priority, zone assignment, and status tracking
 */
@Entity
@Table(name = "SequenceSchedule")
public class SequenceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sequenceId;
    
    @ManyToOne
    private Zone assignedZone;

    // Priority (lower number = higher priority)
    private Integer priority;

    // Status: NOT_STARTED, IN_PROGRESS, COMPLETED, EXCLUDED
    private String status;

    // Scheduling metadata
    private LocalDate scheduledDate;
    private Integer scheduledShift;

    // Estimated times
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedEndTime;

    // Actual times
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualEndTime;

    // Progress tracking
    private Integer totalSeries;
    private Integer completedSeries;
    private Double completionPercentage;

    // Notes
    @Column(length = 1000)
    private String notes;

    // Excluded flag
    private Boolean excluded = false;

    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private String updatedBy;

    public SequenceSchedule() {
        this.createdAt = LocalDateTime.now();
        this.status = "NOT_STARTED";
        this.excluded = false;
        this.priority = 999;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public Zone getAssignedZone() {
        return assignedZone;
    }

    public void setAssignedZone(Zone assignedZone) {
        this.assignedZone = assignedZone;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Integer getScheduledShift() {
        return scheduledShift;
    }

    public void setScheduledShift(Integer scheduledShift) {
        this.scheduledShift = scheduledShift;
    }

    public LocalDateTime getEstimatedStartTime() {
        return estimatedStartTime;
    }

    public void setEstimatedStartTime(LocalDateTime estimatedStartTime) {
        this.estimatedStartTime = estimatedStartTime;
    }

    public LocalDateTime getEstimatedEndTime() {
        return estimatedEndTime;
    }

    public void setEstimatedEndTime(LocalDateTime estimatedEndTime) {
        this.estimatedEndTime = estimatedEndTime;
    }

    public LocalDateTime getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(LocalDateTime actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(LocalDateTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public Integer getTotalSeries() {
        return totalSeries;
    }

    public void setTotalSeries(Integer totalSeries) {
        this.totalSeries = totalSeries;
    }

    public Integer getCompletedSeries() {
        return completedSeries;
    }

    public void setCompletedSeries(Integer completedSeries) {
        this.completedSeries = completedSeries;
    }

    public Double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getExcluded() {
        return excluded;
    }

    public void setExcluded(Boolean excluded) {
        this.excluded = excluded;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

