package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class ValidationQLaize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String itemNumber;
    private String um;
    private String abc;
    private String site;
    private LocalDate lastCnt;
    private String location;
    private String ref;
    private Double qtyOnHand;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validationDate;
    private String validatedBy;
    private String fournisseur;
    private Double laizeContractuel;
    private Double laizeReel;

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getUm() {
        return um;
    }

    public void setUm(String um) {
        this.um = um;
    }

    public String getAbc() {
        return abc;
    }

    public void setAbc(String abc) {
        this.abc = abc;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public LocalDate getLastCnt() {
        return lastCnt;
    }

    public void setLastCnt(LocalDate lastCnt) {
        this.lastCnt = lastCnt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Double getQtyOnHand() {
        return qtyOnHand;
    }

    public void setQtyOnHand(Double qtyOnHand) {
        this.qtyOnHand = qtyOnHand;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getValidationDate() {
        return validationDate;
    }

    public void setValidationDate(LocalDateTime validationDate) {
        this.validationDate = validationDate;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(String validatedBy) {
        this.validatedBy = validatedBy;
    }

    public Double getLaizeContractuel() {
        return laizeContractuel;
    }

    public void setLaizeContractuel(Double laizeContractuel) {
        this.laizeContractuel = laizeContractuel;
    }

    public Double getLaizeReel() {
        return laizeReel;
    }

    public void setLaizeReel(Double laizeReel) {
        this.laizeReel = laizeReel;
    }
}
