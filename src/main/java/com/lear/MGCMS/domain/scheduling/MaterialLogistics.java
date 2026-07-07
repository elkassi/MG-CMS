package com.lear.MGCMS.domain.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.Zone;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track material logistics requirements
 * Used by logistics team to know when materials need to be moved
 */
@Entity
@Table(name = "MaterialLogistics")
public class MaterialLogistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partNumberMaterial;
    
    @ManyToOne
    private Zone zone;

    // Required quantity
    private Double requiredQuantity;
    private Double availableQuantity;
    private Double deficit;

    // Timing
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime neededBy;
    
    // Priority: URGENT (< 1 hour), HIGH (< 2 hours), MEDIUM (< 4 hours), LOW (> 4 hours)
    private String priority;

    // Status: PENDING, IN_TRANSIT, DELIVERED, CANCELLED
    private String status;

    // Source location
    private String sourceLocation;
    
    // Destination location (zone emplacements)
    private String destinationLocation;

    // Roll details (comma-separated serial IDs if specific rolls assigned)
    @Column(length = 2000)
    private String assignedRolls;

    // Notes
    @Column(length = 1000)
    private String notes;

    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private String updatedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
    private String completedBy;

    public MaterialLogistics() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartNumberMaterial() {
        return partNumberMaterial;
    }

    public void setPartNumberMaterial(String partNumberMaterial) {
        this.partNumberMaterial = partNumberMaterial;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public Double getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(Double requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public Double getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Double availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Double getDeficit() {
        return deficit;
    }

    public void setDeficit(Double deficit) {
        this.deficit = deficit;
    }

    public LocalDateTime getNeededBy() {
        return neededBy;
    }

    public void setNeededBy(LocalDateTime neededBy) {
        this.neededBy = neededBy;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getAssignedRolls() {
        return assignedRolls;
    }

    public void setAssignedRolls(String assignedRolls) {
        this.assignedRolls = assignedRolls;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }
}

