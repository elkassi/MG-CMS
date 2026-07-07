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

/**
 * Entity to store individual series assignments within an optimized plan.
 * This provides a more granular view of the scheduling.
 */
@Entity
@Table(name = "optimized_series_assignment")
public class OptimizedSeriesAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private OptimizedPlan optimizedPlan;

    private String serieId;
    private String sequenceId;
    private String machineName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEnd;

    // Cutting duration in minutes
    private Double cuttingDurationMinutes;

    // Whether this assignment is locked (cannot be changed by optimizer)
    private Boolean isLocked = false;

    // Order within the sequence
    private Integer orderInSequence;

    // Order on the machine
    private Integer orderOnMachine;

    // Movement note (e.g., "Move from AA1 to AA3")
    private String movementNote;

    // Part number material for grouping
    private String partNumberMaterial;

    // Placement name
    private String placement;

    public OptimizedSeriesAssignment() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OptimizedPlan getOptimizedPlan() {
        return optimizedPlan;
    }

    public void setOptimizedPlan(OptimizedPlan optimizedPlan) {
        this.optimizedPlan = optimizedPlan;
    }

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

    public Integer getOrderInSequence() {
        return orderInSequence;
    }

    public void setOrderInSequence(Integer orderInSequence) {
        this.orderInSequence = orderInSequence;
    }

    public Integer getOrderOnMachine() {
        return orderOnMachine;
    }

    public void setOrderOnMachine(Integer orderOnMachine) {
        this.orderOnMachine = orderOnMachine;
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
}
