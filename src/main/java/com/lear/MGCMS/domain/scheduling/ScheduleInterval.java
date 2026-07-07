package com.lear.MGCMS.domain.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.Zone;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to store schedule intervals (pauses, stops, breaks)
 * Can be applied to specific machines, zones, or globally
 */
@Entity
@Table(name = "ScheduleInterval")
public class ScheduleInterval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // Type: PAUSE (extends cutting time), STRICT (no cutting allowed), BREAK, MAINTENANCE
    private String intervalType;

    // Can be null (applies to all), or specific machine
    @ManyToOne
    private ProductionTable machine;
    private String machineName; // Alternative to object reference

    // Can be null (applies to all zones), or specific zone
    @ManyToOne
    private Zone zone;

    // Description/reason
    @Column(length = 500)
    private String description;

    // Recurrence (optional)
    private Boolean recurring = false;
    private String recurrencePattern; // DAILY, WEEKLY, SHIFT

    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private String createdBy;

    public ScheduleInterval() {
        this.createdAt = LocalDateTime.now();
        this.recurring = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }

    public ProductionTable getMachine() {
        return machine;
    }

    public void setMachine(ProductionTable machine) {
        this.machine = machine;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getRecurring() {
        return recurring;
    }

    public void setRecurring(Boolean recurring) {
        this.recurring = recurring;
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
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
}

