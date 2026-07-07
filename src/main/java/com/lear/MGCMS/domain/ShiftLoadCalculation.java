package com.lear.MGCMS.domain;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_load_calculation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"shift_date", "shift_number", "machine_type"})
})
public class ShiftLoadCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "shift_number", nullable = false)
    private Integer shiftNumber;

    @Column(name = "machine_type", length = 50)
    private String machineType;

    @Column(name = "total_planned_time", precision = 10, scale = 2)
    private BigDecimal totalPlannedTime;

    @Column(name = "total_actual_time", precision = 10, scale = 2)
    private BigDecimal totalActualTime;

    @Column(name = "available_time", precision = 10, scale = 2)
    private BigDecimal availableTime;

    @Column(name = "load_percentage", precision = 5, scale = 2)
    private BigDecimal loadPercentage;

    @Column(name = "efficiency_percentage", precision = 5, scale = 2)
    private BigDecimal efficiencyPercentage;

    @Column(name = "carryover_time", precision = 10, scale = 2)
    private BigDecimal carryoverTime;

    @Column(name = "machines_count")
    private Integer machinesCount;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "calculated_by", length = 100)
    private String calculatedBy;

    public ShiftLoadCalculation() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public BigDecimal getTotalPlannedTime() {
        return totalPlannedTime;
    }

    public void setTotalPlannedTime(BigDecimal totalPlannedTime) {
        this.totalPlannedTime = totalPlannedTime;
    }

    public BigDecimal getTotalActualTime() {
        return totalActualTime;
    }

    public void setTotalActualTime(BigDecimal totalActualTime) {
        this.totalActualTime = totalActualTime;
    }

    public BigDecimal getAvailableTime() {
        return availableTime;
    }

    public void setAvailableTime(BigDecimal availableTime) {
        this.availableTime = availableTime;
    }

    public BigDecimal getLoadPercentage() {
        return loadPercentage;
    }

    public void setLoadPercentage(BigDecimal loadPercentage) {
        this.loadPercentage = loadPercentage;
    }

    public BigDecimal getEfficiencyPercentage() {
        return efficiencyPercentage;
    }

    public void setEfficiencyPercentage(BigDecimal efficiencyPercentage) {
        this.efficiencyPercentage = efficiencyPercentage;
    }

    public BigDecimal getCarryoverTime() {
        return carryoverTime;
    }

    public void setCarryoverTime(BigDecimal carryoverTime) {
        this.carryoverTime = carryoverTime;
    }

    public Integer getMachinesCount() {
        return machinesCount;
    }

    public void setMachinesCount(Integer machinesCount) {
        this.machinesCount = machinesCount;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public String getCalculatedBy() {
        return calculatedBy;
    }

    public void setCalculatedBy(String calculatedBy) {
        this.calculatedBy = calculatedBy;
    }
}
