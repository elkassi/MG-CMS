package com.lear.MGCMS.domain;


import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@IdClass(StockStatusReportId.class)
public class StockStatusReport {

    @Id private String itemNumber;
    private String um;
    private String abc;
    private String site;
    private LocalDate lastCnt;
    @Id private String location;
    @Id private String ref;
    @Id private Double qtyOnHand;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;
    private Boolean isDeleted;

    @Transient
    private String serie;


    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public StockStatusReport(String itemNumber, String um, String abc, String site, LocalDate lastCnt,
                             String location, String ref, Double qtyOnHand, String status, LocalDateTime lastUpdated, Boolean isDeleted) {
        this.itemNumber = itemNumber;
        this.um = um;
        this.abc = abc;
        this.site = site;
        this.lastCnt = lastCnt;
        this.location = location;
        this.ref = ref;
        this.qtyOnHand = qtyOnHand;
        this.status = status;
        this.lastUpdated = lastUpdated;
        this.isDeleted = isDeleted;
    }

    public StockStatusReport(String s, String s1, String s2, String s3, String s4, String s5, LocalDateTime now) {
    }

    @Override
    public String toString() {
        return "StockStatusReport{" +
                "itemNumber='" + itemNumber + '\'' +
                ", um='" + um + '\'' +
                ", abc='" + abc + '\'' +
                ", site='" + site + '\'' +
                ", lastCnt=" + lastCnt +
                ", location='" + location + '\'' +
                ", ref='" + ref + '\'' +
                ", qtyOnHand=" + qtyOnHand +
                ", status='" + status + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public StockStatusReport() {
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
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
}
