package com.lear.MGCMS.domain.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.Zone;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity to store shift schedule configuration
 * Tracks which zone is being scheduled, shift number, and creation metadata
 */
@Entity
@Table(name = "ShiftSchedule")
public class ShiftSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Zone zone;

    private LocalDate shiftDate;
    private Integer shiftNumber; // 1, 2, or 3

    // Personnel counts for this shift
    private Integer spreadingPersonnel;
    private Integer cuttingPersonnel;
    private Integer laserDxfPersonnel;
    private Integer pressDiePersonnel;
    private Integer qualityControlPersonnel;

    // Status
    private String status; // PLANNING, ACTIVE, COMPLETED

    // Notes/comments
    @Column(length = 1000)
    private String notes;

    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private String updatedBy;

    public ShiftSchedule() {
        this.createdAt = LocalDateTime.now();
        this.status = "PLANNING";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public Integer getShiftNumber() {
        return shiftNumber;
    }

    public void setShiftNumber(Integer shiftNumber) {
        this.shiftNumber = shiftNumber;
    }

    public Integer getSpreadingPersonnel() {
        return spreadingPersonnel;
    }

    public void setSpreadingPersonnel(Integer spreadingPersonnel) {
        this.spreadingPersonnel = spreadingPersonnel;
    }

    public Integer getCuttingPersonnel() {
        return cuttingPersonnel;
    }

    public void setCuttingPersonnel(Integer cuttingPersonnel) {
        this.cuttingPersonnel = cuttingPersonnel;
    }

    public Integer getLaserDxfPersonnel() {
        return laserDxfPersonnel;
    }

    public void setLaserDxfPersonnel(Integer laserDxfPersonnel) {
        this.laserDxfPersonnel = laserDxfPersonnel;
    }

    public Integer getPressDiePersonnel() {
        return pressDiePersonnel;
    }

    public void setPressDiePersonnel(Integer pressDiePersonnel) {
        this.pressDiePersonnel = pressDiePersonnel;
    }

    public Integer getQualityControlPersonnel() {
        return qualityControlPersonnel;
    }

    public void setQualityControlPersonnel(Integer qualityControlPersonnel) {
        this.qualityControlPersonnel = qualityControlPersonnel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

