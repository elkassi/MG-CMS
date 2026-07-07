package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * Persisted snapshot of one Rapport Usage / BOM comparison line (one
 * cuttingRequest sequence + confirmReftissu, aggregated across all dates).
 * Populated by {@link com.lear.MGCMS.services.RapportUsageReportService#refresh()}.
 * The id is {@code cuttingRequest_sequence + "-" + confirmReftissu} so a refresh
 * upserts in place rather than appending duplicates.
 */
@Entity
public class RapportUsageReport {

    @Id
    private String id;

    private String cuttingRequest_sequence;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime dateDebutMatelassage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime dateFinMatelassage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime dateDebutCoupe;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime dateFinCoupe;

    private String confirmReftissu;
    private String description;
    private Double totalConsommationPlan;
    private Double overlap;
    private Double nonUtitlse;
    private Double defaut;
    private Double totalUsage;
    private Double excess;
    private Double finalUsage;
    private Long cuttingPlanId;
    private Double qadUsage;
    private Double variance;
    private String statusMatelassage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;

    public RapportUsageReport() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCuttingRequest_sequence() {
        return cuttingRequest_sequence;
    }

    public void setCuttingRequest_sequence(String cuttingRequest_sequence) {
        this.cuttingRequest_sequence = cuttingRequest_sequence;
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

    public String getConfirmReftissu() {
        return confirmReftissu;
    }

    public void setConfirmReftissu(String confirmReftissu) {
        this.confirmReftissu = confirmReftissu;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getTotalConsommationPlan() {
        return totalConsommationPlan;
    }

    public void setTotalConsommationPlan(Double totalConsommationPlan) {
        this.totalConsommationPlan = totalConsommationPlan;
    }

    public Double getOverlap() {
        return overlap;
    }

    public void setOverlap(Double overlap) {
        this.overlap = overlap;
    }

    public Double getNonUtitlse() {
        return nonUtitlse;
    }

    public void setNonUtitlse(Double nonUtitlse) {
        this.nonUtitlse = nonUtitlse;
    }

    public Double getDefaut() {
        return defaut;
    }

    public void setDefaut(Double defaut) {
        this.defaut = defaut;
    }

    public Double getTotalUsage() {
        return totalUsage;
    }

    public void setTotalUsage(Double totalUsage) {
        this.totalUsage = totalUsage;
    }

    public Double getExcess() {
        return excess;
    }

    public void setExcess(Double excess) {
        this.excess = excess;
    }

    public Double getFinalUsage() {
        return finalUsage;
    }

    public void setFinalUsage(Double finalUsage) {
        this.finalUsage = finalUsage;
    }

    public Long getCuttingPlanId() {
        return cuttingPlanId;
    }

    public void setCuttingPlanId(Long cuttingPlanId) {
        this.cuttingPlanId = cuttingPlanId;
    }

    public Double getQadUsage() {
        return qadUsage;
    }

    public void setQadUsage(Double qadUsage) {
        this.qadUsage = qadUsage;
    }

    public Double getVariance() {
        return variance;
    }

    public void setVariance(Double variance) {
        this.variance = variance;
    }

    public String getStatusMatelassage() {
        return statusMatelassage;
    }

    public void setStatusMatelassage(String statusMatelassage) {
        this.statusMatelassage = statusMatelassage;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
