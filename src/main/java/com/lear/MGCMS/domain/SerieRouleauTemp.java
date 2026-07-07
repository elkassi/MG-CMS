package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class SerieRouleauTemp {

    @Id
    private String tableMatelassage;

    private String idRouleau;
    private String lot;
    private String reftissu;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime date;
    private Double quantiteInitiale;
    private Double estimationRest;

    public Double getQuantiteInitiale() {
        return quantiteInitiale;
    }

    public void setQuantiteInitiale(Double quantiteInitiale) {
        this.quantiteInitiale = quantiteInitiale;
    }

    public Double getEstimationRest() {
        return estimationRest;
    }

    public void setEstimationRest(Double estimationRest) {
        this.estimationRest = estimationRest;
    }

    public String getTableMatelassage() {
        return tableMatelassage;
    }

    public void setTableMatelassage(String tableMatelassage) {
        this.tableMatelassage = tableMatelassage;
    }

    public String getIdRouleau() {
        return idRouleau;
    }

    public void setIdRouleau(String idRouleau) {
        this.idRouleau = idRouleau;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public String getReftissu() {
        return reftissu;
    }

    public void setReftissu(String reftissu) {
        this.reftissu = reftissu;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
