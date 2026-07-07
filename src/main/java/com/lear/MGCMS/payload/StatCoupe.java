package com.lear.MGCMS.payload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StatCoupe {

    private String sequence;
    private Long cuttingPlanId;
    private String modele;
    private String zone_nom;

    private LocalDate planningDate;
    private String shift;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime dateDebutMatelassage;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime dateFinMatelassage;
    private int waitingMatelassage;
    private int inProgressMatelassage;
    private int completeMatelassage;
    private int incompleteMatelassage;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime dateDebutCoupe;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime dateFinCoupe;
    private int waitingCoupe;
    private int inProgressCoupe;
    private int completeCoupe;
    private int incompleteCoupe;

    public StatCoupe() {
        super();
    }

    public StatCoupe(String sequence, Long cuttingPlanId, String modele, String zone_nom, LocalDate planningDate, String shift, LocalDateTime dateDebutMatelassage, LocalDateTime dateFinMatelassage, int waitingMatelassage, int inProgressMatelassage, int completeMatelassage, int incompleteMatelassage, LocalDateTime dateDebutCoupe, LocalDateTime dateFinCoupe, int waitingCoupe, int inProgressCoupe, int completeCoupe, int incompleteCoupe) {
        this.sequence = sequence;
        this.cuttingPlanId = cuttingPlanId;
        this.modele = modele;
        this.zone_nom = zone_nom;
        this.planningDate = planningDate;
        this.shift = shift;
        this.dateDebutMatelassage = dateDebutMatelassage;
        this.dateFinMatelassage = dateFinMatelassage;
        this.waitingMatelassage = waitingMatelassage;
        this.inProgressMatelassage = inProgressMatelassage;
        this.completeMatelassage = completeMatelassage;
        this.incompleteMatelassage = incompleteMatelassage;
        this.dateDebutCoupe = dateDebutCoupe;
        this.dateFinCoupe = dateFinCoupe;
        this.waitingCoupe = waitingCoupe;
        this.inProgressCoupe = inProgressCoupe;
        this.completeCoupe = completeCoupe;
        this.incompleteCoupe = incompleteCoupe;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }


    public Long getCuttingPlanId() {
        return cuttingPlanId;
    }

    public void setCuttingPlanId(Long cuttingPlanId) {
        this.cuttingPlanId = cuttingPlanId;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public LocalDate getPlanningDate() {
        return planningDate;
    }

    public void setPlanningDate(LocalDate planningDate) {
        this.planningDate = planningDate;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getZone_nom() {
        return zone_nom;
    }

    public void setZone_nom(String zone_nom) {
        this.zone_nom = zone_nom;
    }

    public LocalDateTime getDateDebutMatelassage() {
        return dateDebutMatelassage;
    }

    public void setDateDebutMatelassage(LocalDateTime dateDebutMatelassage) {
        this.dateDebutMatelassage = dateDebutMatelassage;
    }

    public LocalDateTime getDateFinMatelassage() {
        return dateFinMatelassage;
    }

    public void setDateFinMatelassage(LocalDateTime dateFinMatelassage) {
        this.dateFinMatelassage = dateFinMatelassage;
    }

    public int getWaitingMatelassage() {
        return waitingMatelassage;
    }

    public void setWaitingMatelassage(int waitingMatelassage) {
        this.waitingMatelassage = waitingMatelassage;
    }

    public int getInProgressMatelassage() {
        return inProgressMatelassage;
    }

    public void setInProgressMatelassage(int inProgressMatelassage) {
        this.inProgressMatelassage = inProgressMatelassage;
    }

    public int getCompleteMatelassage() {
        return completeMatelassage;
    }

    public void setCompleteMatelassage(int completeMatelassage) {
        this.completeMatelassage = completeMatelassage;
    }

    public int getIncompleteMatelassage() {
        return incompleteMatelassage;
    }

    public void setIncompleteMatelassage(int incompleteMatelassage) {
        this.incompleteMatelassage = incompleteMatelassage;
    }

    public LocalDateTime getDateDebutCoupe() {
        return dateDebutCoupe;
    }

    public void setDateDebutCoupe(LocalDateTime dateDebutCoupe) {
        this.dateDebutCoupe = dateDebutCoupe;
    }

    public LocalDateTime getDateFinCoupe() {
        return dateFinCoupe;
    }

    public void setDateFinCoupe(LocalDateTime dateFinCoupe) {
        this.dateFinCoupe = dateFinCoupe;
    }

    public int getWaitingCoupe() {
        return waitingCoupe;
    }

    public void setWaitingCoupe(int waitingCoupe) {
        this.waitingCoupe = waitingCoupe;
    }

    public int getInProgressCoupe() {
        return inProgressCoupe;
    }

    public void setInProgressCoupe(int inProgressCoupe) {
        this.inProgressCoupe = inProgressCoupe;
    }

    public int getCompleteCoupe() {
        return completeCoupe;
    }

    public void setCompleteCoupe(int completeCoupe) {
        this.completeCoupe = completeCoupe;
    }

    public int getIncompleteCoupe() {
        return incompleteCoupe;
    }

    public void setIncompleteCoupe(int incompleteCoupe) {
        this.incompleteCoupe = incompleteCoupe;
    }
}
