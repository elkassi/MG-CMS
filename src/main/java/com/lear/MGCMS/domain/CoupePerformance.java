package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@IdClass(CoupePerformanceId.class)
public class CoupePerformance {

    @Id
    private String machine;
    @Id
    private String placement;
    @Id
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    private LocalDate date;
    private String shift;

    private String serie;
    private Double compteur;
    private Double nbrPieces;

    private Double running;
    private Double interruption;
    private Double reperage;
    private Double reperagePlus;
    private Double horsCycle;

    private String coupeur;

    public Double getNbrPieces() {
        return nbrPieces;
    }

    public void setNbrPieces(Double nbrPieces) {
        this.nbrPieces = nbrPieces;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public Double getCompteur() {
        return compteur;
    }

    public void setCompteur(Double compteur) {
        this.compteur = compteur;
    }

    public Double getRunning() {
        return running;
    }

    public void setRunning(Double running) {
        this.running = running;
    }

    public Double getInterruption() {
        return interruption;
    }

    public void setInterruption(Double interruption) {
        this.interruption = interruption;
    }

    public Double getReperage() {
        return reperage;
    }

    public void setReperage(Double reperage) {
        this.reperage = reperage;
    }

    public Double getReperagePlus() {
        return reperagePlus;
    }

    public void setReperagePlus(Double reperagePlus) {
        this.reperagePlus = reperagePlus;
    }

    public Double getHorsCycle() {
        return horsCycle;
    }

    public void setHorsCycle(Double horsCycle) {
        this.horsCycle = horsCycle;
    }

    public String getCoupeur() {
        return coupeur;
    }

    public void setCoupeur(String coupeur) {
        this.coupeur = coupeur;
    }
}
