package com.lear.MGCMS.payload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class RapportUsage {
    /*
    SELECT
crs.cuttingRequest_sequence,
Min(
	CASE
        WHEN crs.dateDebutMatelassage IS NOT NULL THEN crs.dateDebutMatelassage
        ELSE NULL
    END
) as dateDebutMatelassage,
MAX(crs.dateFinMatelassage) as dateFinMatelassage,
Min(
	CASE
        WHEN crs.dateDebutCoupe IS NOT NULL THEN crs.dateDebutCoupe
        ELSE NULL
    END
) as dateDebutCoupe,
MAX(crs.dateFinCoupe) as dateFinCoupe,
confirmReftissu,
MAX(crs.description) as description,
Sum(crsr.nbrCouche * crsr.longueurPremierCouche + COALESCE(crsr.longueurCoucheOverlap, 0)) as totalConsommationPlan,
SUM(
    COALESCE(overlap1, 0) +
    COALESCE(overlap2, 0) +
    COALESCE(overlap3, 0) +
    COALESCE(overlap4, 0) +
    COALESCE(overlap5, 0) +
    COALESCE(overlap6, 0) +
    COALESCE(overlap7, 0) +
    COALESCE(overlap8, 0)
) AS Overlap,
Sum(COALESCE(crsr.nonUtitlse, 0)) as nonUtitlse,
Sum(COALESCE(crsr.defaut, 0)) as defaut,
SUM(COALESCE(crsr.totalUsage, 0)) as totalUsage,
SUM(COALESCE(crsr.excess, 0)) as excess,
MAX(cr.cuttingPlanId) as cuttingPlanId,
MAX(cpm.qadUsage) as qadUsage
FROM [dbo].CuttingRequestSerieRouleau as crsr
JOIN dbo.CuttingRequestSerie as crs on crs.serie = crsr.cuttingRequestSerie_serie
JOIN dbo.CuttingRequest as cr on cr.sequence = crs.cuttingRequest_sequence
JOIN [dbo].[CuttingPlanMaterial] as cpm on cpm.cuttingPlan_id = cr.cuttingPlanId and cpm.[partNumberMaterial] = confirmReftissu
WHERE crs.planningDate = '2024-10-14' and crs.shift = 2
GROUP BY crs.cuttingRequest_sequence, confirmReftissu
     */
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

    public String getStatusMatelassage() {
        return statusMatelassage;
    }

    public void setStatusMatelassage(String statusMatelassage) {
        this.statusMatelassage = statusMatelassage;
    }

    public Double getFinalUsage() {
        return finalUsage;
    }

    public void setFinalUsage(Double finalUsage) {
        this.finalUsage = finalUsage;
    }

    public String getCuttingRequest_sequence() {
        return cuttingRequest_sequence;
    }

    public void setCuttingRequest_sequence(String cuttingRequest_sequence) {
        this.cuttingRequest_sequence = cuttingRequest_sequence;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getDateDebutMatelassage() {
        return dateDebutMatelassage;
    }
    public LocalDateTime getDateFinMatelassage() {
        return dateFinMatelassage;
    }

    public void setDateDebutMatelassage(LocalDateTime dateDebutMatelassage) {
        this.dateDebutMatelassage = dateDebutMatelassage;
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
}
