package com.lear.MGCMS.payload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class RapportOverlap {

    /*
    creaate properties for this query :
    SELECT TOP (1000)
    crs.cuttingRequest_sequence
	   ,crsr.cuttingRequestSerie_serie
	   ,crs.quantite
		,crs.partNumbers
		,crs.placement
		,crsr.longueurPremierCouche
		,crsr.nbrCouche
		,crs.laize
		,crsr.laize
		,crsr.confirmReftissu
		,crs.description
		,crsr.createdAt
		,crs.tableMatelassage
		,crs.tableCoupe
		,crs.matelasseur1 + '/' +crs.matelasseur2
		,(select SUM(cast(crs2.nbrCouche as int)) from [dbo].[CuttingRequestSerie] as crs2 where crs2.cuttingRequest_sequence = crs.cuttingRequest_sequence and crs2.placement = crs.placement )
	  ,[overlap1]
            ,[overlap2]
            ,[overlap3]
            ,[overlap4]
            ,[overlap5]
            ,[overlap6]
            ,[overlap7]
            ,[overlap8]
            ,[excess]
            ,[retour]
            ,[totalUsage]

    FROM [dbo].[CuttingRequestSerieRouleau] crsr
    Join [dbo].[CuttingRequestSerie] crs on crs.serie = crsr.cuttingRequestSerie_serie

    order by serie desc
     */
    private String cuttingRequest_sequence;
    private String cuttingRequestSerie_serie;
    private String quantite;
    private String partNumbers;
    private String placement;
    private Double longueurPremierCouche;
    private Integer nbrCouche;
    private Double laize;
    private Double laizeMesure;
    private String confirmReftissu;
    private String description;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    private String tableMatelassage;
    private String tableCoupe;
    private String matelasseur;
    private Long nbrCoucheTotal;
    private Double overlap1;
    private Double overlap2;
    private Double overlap3;
    private Double overlap4;
    private Double overlap5;
    private Double overlap6;
    private Double overlap7;
    private Double overlap8;
    private Double excess;
    private Double retour;
    private Double totalUsage;
    private String drill1;
    private String drill2;
    private String matelassageEndroit;
    private Long cuttingPlanId;
    private Long cmsId;

    public Double getLaizeMesure() {
        return laizeMesure;
    }

    public void setLaizeMesure(Double laizeMesure) {
        this.laizeMesure = laizeMesure;
    }

    public String getDrill1() {
        return drill1;
    }

    public void setDrill1(String drill1) {
        this.drill1 = drill1;
    }

    public String getDrill2() {
        return drill2;
    }

    public void setDrill2(String drill2) {
        this.drill2 = drill2;
    }

    public String getMatelassageEndroit() {
        return matelassageEndroit;
    }

    public void setMatelassageEndroit(String matelassageEndroit) {
        this.matelassageEndroit = matelassageEndroit;
    }

    public String getQuantite() {
        return quantite;
    }

    public void setQuantite(String quantite) {
        this.quantite = quantite;
    }

    public String getCuttingRequest_sequence() {
        return cuttingRequest_sequence;
    }

    public void setCuttingRequest_sequence(String cuttingRequest_sequence) {
        this.cuttingRequest_sequence = cuttingRequest_sequence;
    }

    public String getCuttingRequestSerie_serie() {
        return cuttingRequestSerie_serie;
    }

    public void setCuttingRequestSerie_serie(String cuttingRequestSerie_serie) {
        this.cuttingRequestSerie_serie = cuttingRequestSerie_serie;
    }

    public String getPartNumbers() {
        return partNumbers;
    }

    public void setPartNumbers(String partNumbers) {
        this.partNumbers = partNumbers;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public Double getLongueurPremierCouche() {
        return longueurPremierCouche;
    }

    public void setLongueurPremierCouche(Double longueurPremierCouche) {
        this.longueurPremierCouche = longueurPremierCouche;
    }

    public Integer getNbrCouche() {
        return nbrCouche;
    }

    public void setNbrCouche(Integer nbrCouche) {
        this.nbrCouche = nbrCouche;
    }

    public Double getLaize() {
        return laize;
    }

    public void setLaize(Double laize) {
        this.laize = laize;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTableMatelassage() {
        return tableMatelassage;
    }

    public void setTableMatelassage(String tableMatelassage) {
        this.tableMatelassage = tableMatelassage;
    }

    public String getTableCoupe() {
        return tableCoupe;
    }

    public void setTableCoupe(String tableCoupe) {
        this.tableCoupe = tableCoupe;
    }

    public String getMatelasseur() {
        return matelasseur;
    }

    public void setMatelasseur(String matelasseur) {
        this.matelasseur = matelasseur;
    }

    public Long getNbrCoucheTotal() {
        return nbrCoucheTotal;
    }

    public void setNbrCoucheTotal(Long nbrCoucheTotal) {
        this.nbrCoucheTotal = nbrCoucheTotal;
    }

    public Double getOverlap1() {
        return overlap1;
    }

    public void setOverlap1(Double overlap1) {
        this.overlap1 = overlap1;
    }

    public Double getOverlap2() {
        return overlap2;
    }

    public void setOverlap2(Double overlap2) {
        this.overlap2 = overlap2;
    }

    public Double getOverlap3() {
        return overlap3;
    }

    public void setOverlap3(Double overlap3) {
        this.overlap3 = overlap3;
    }

    public Double getOverlap4() {
        return overlap4;
    }

    public void setOverlap4(Double overlap4) {
        this.overlap4 = overlap4;
    }

    public Double getOverlap5() {
        return overlap5;
    }

    public void setOverlap5(Double overlap5) {
        this.overlap5 = overlap5;
    }

    public Double getOverlap6() {
        return overlap6;
    }

    public void setOverlap6(Double overlap6) {
        this.overlap6 = overlap6;
    }

    public Double getOverlap7() {
        return overlap7;
    }

    public void setOverlap7(Double overlap7) {
        this.overlap7 = overlap7;
    }

    public Double getOverlap8() {
        return overlap8;
    }

    public void setOverlap8(Double overlap8) {
        this.overlap8 = overlap8;
    }

    public Double getExcess() {
        return excess;
    }

    public void setExcess(Double excess) {
        this.excess = excess;
    }

    public Double getRetour() {
        return retour;
    }

    public void setRetour(Double retour) {
        this.retour = retour;
    }

    public Double getTotalUsage() {
        return totalUsage;
    }

    public void setTotalUsage(Double totalUsage) {
        this.totalUsage = totalUsage;
    }

    public Long getCuttingPlanId() {
        return cuttingPlanId;
    }

    public void setCuttingPlanId(Long cuttingPlanId) {
        this.cuttingPlanId = cuttingPlanId;
    }

    public Long getCmsId() {
        return cmsId;
    }

    public void setCmsId(Long cmsId) {
        this.cmsId = cmsId;
    }
}
