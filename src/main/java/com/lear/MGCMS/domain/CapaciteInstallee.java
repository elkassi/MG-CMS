package com.lear.MGCMS.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "capacite_installee")
public class CapaciteInstallee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_production")
    private LocalDate dateProduction;

    @Column(name = "shift_number")
    private Integer shiftNumber;

    @Column(name = "groupe", nullable = false, length = 50)
    private String groupe;

    @Column(name = "capacite_installee", nullable = false)
    private Integer capaciteInstallee;

    @Column(name = "temps_total_par_machine", nullable = false)
    private Double tempsTotalParMachine = 460.0;

    @Column(name = "efficience_target", nullable = false)
    private Double efficienceTarget = 90.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CapaciteInstallee() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateProduction() {
        return dateProduction;
    }

    public void setDateProduction(LocalDate dateProduction) {
        this.dateProduction = dateProduction;
    }

    public Integer getShiftNumber() {
        return shiftNumber;
    }

    public void setShiftNumber(Integer shiftNumber) {
        this.shiftNumber = shiftNumber;
    }

    public String getGroupe() {
        return groupe;
    }

    public void setGroupe(String groupe) {
        this.groupe = groupe;
    }

    public Integer getCapaciteInstallee() {
        return capaciteInstallee;
    }

    public void setCapaciteInstallee(Integer capaciteInstallee) {
        this.capaciteInstallee = capaciteInstallee;
    }

    public Double getTempsTotalParMachine() {
        return tempsTotalParMachine;
    }

    public void setTempsTotalParMachine(Double tempsTotalParMachine) {
        this.tempsTotalParMachine = tempsTotalParMachine;
    }

    public Double getEfficienceTarget() {
        return efficienceTarget;
    }

    public void setEfficienceTarget(Double efficienceTarget) {
        this.efficienceTarget = efficienceTarget;
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
}
