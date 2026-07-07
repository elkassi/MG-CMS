package com.lear.MGCMS.payload;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RapportShortageUrgent {

    private String idRouleau;
    private Double totalUsage;
    private Double metrageInitial;
    private Double shortage;
    private LocalDateTime date;
    private String reftissu;
    private String fournisseur;

    public Double getTotalUsage() {
        return totalUsage;
    }

    public void setTotalUsage(Double totalUsage) {
        this.totalUsage = totalUsage;
    }

    public Double getMetrageInitial() {
        return metrageInitial;
    }

    public void setMetrageInitial(Double metrageInitial) {
        this.metrageInitial = metrageInitial;
    }

    public Double getShortage() {
        return shortage;
    }

    public void setShortage(Double shortage) {
        this.shortage = shortage;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getReftissu() {
        return reftissu;
    }

    public void setReftissu(String reftissu) {
        this.reftissu = reftissu;
    }

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
    }

    public String getIdRouleau() {
        return idRouleau;
    }

    public void setIdRouleau(String idRouleau) {
        this.idRouleau = idRouleau;
    }
}
