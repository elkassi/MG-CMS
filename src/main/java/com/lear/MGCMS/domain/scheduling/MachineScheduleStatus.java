package com.lear.MGCMS.domain.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.Zone;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity to track machine availability and status for scheduling
 * Includes PM status, maintenance status, zone overrides, and load calculations
 */
@Entity
@Table(name = "MachineScheduleStatus")
public class MachineScheduleStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ProductionTable machine;

    private LocalDate effectiveDate;
    private Integer shiftNumber;

    // Availability
    private Boolean available = true;
    private String unavailableReason; // PM, MAINTENANCE, NO_PERSONNEL, BROKEN, OTHER
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime unavailableUntil;

    // Zone override (for temporary zone reassignment)
    @ManyToOne
    private Zone originalZone;
    @ManyToOne
    private Zone overrideZone;

    // Load calculation (calculated, not stored long-term)
    private Double scheduledHours;
    private Double availableHours;
    private Double loadPercentage;

    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private String updatedBy;

    public MachineScheduleStatus() {
        this.createdAt = LocalDateTime.now();
        this.available = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductionTable getMachine() {
        return machine;
    }

    public void setMachine(ProductionTable machine) {
        this.machine = machine;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Integer getShiftNumber() {
        return shiftNumber;
    }

    public void setShiftNumber(Integer shiftNumber) {
        this.shiftNumber = shiftNumber;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public void setUnavailableReason(String unavailableReason) {
        this.unavailableReason = unavailableReason;
    }

    public LocalDateTime getUnavailableUntil() {
        return unavailableUntil;
    }

    public void setUnavailableUntil(LocalDateTime unavailableUntil) {
        this.unavailableUntil = unavailableUntil;
    }

    public Zone getOriginalZone() {
        return originalZone;
    }

    public void setOriginalZone(Zone originalZone) {
        this.originalZone = originalZone;
    }

    public Zone getOverrideZone() {
        return overrideZone;
    }

    public void setOverrideZone(Zone overrideZone) {
        this.overrideZone = overrideZone;
    }

    public Double getScheduledHours() {
        return scheduledHours;
    }

    public void setScheduledHours(Double scheduledHours) {
        this.scheduledHours = scheduledHours;
    }

    public Double getAvailableHours() {
        return availableHours;
    }

    public void setAvailableHours(Double availableHours) {
        this.availableHours = availableHours;
    }

    public Double getLoadPercentage() {
        return loadPercentage;
    }

    public void setLoadPercentage(Double loadPercentage) {
        this.loadPercentage = loadPercentage;
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

