package com.lear.MGCMS.domain.scanCoupe;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "Scan_Rouleau")
public class Rouleau {

    @Id
    private String serialId;
    private String reftissu;
    private String quantite;
    private String emplacement;
    private String lot;

    private Integer matricule;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    @Override
    public String toString() {
        return "Rouleau{" +
                "serialId='" + serialId + '\'' +
                ", reftissu='" + reftissu + '\'' +
                ", quantite=" + quantite +
                ", matricule=" + matricule +
                ", date=" + date +
                ", emplacement='" + emplacement + '\'' +
                ", lot='" + lot + '\'' +
                '}';
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public String getEmplacement() {
        return emplacement;
    }

    public void setEmplacement(String emplacement) {
        this.emplacement = emplacement;
    }

    public Integer getMatricule() {
        return matricule;
    }

    public void setMatricule(Integer matricule) {
        this.matricule = matricule;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(String serialId) {
        this.serialId = serialId;
    }

    public String getReftissu() {
        return reftissu;
    }

    public void setReftissu(String reftissu) {
        this.reftissu = reftissu;
    }

    public String getQuantite() {
        return quantite;
    }

    public void setQuantite(String quantite) {
        this.quantite = quantite;
    }
}
