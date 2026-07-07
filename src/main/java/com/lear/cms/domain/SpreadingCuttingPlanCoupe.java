package com.lear.cms.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "Spreading_Cutting_PlanCoupe")
public class SpreadingCuttingPlanCoupe {
	
	@Id
	@Column(name = "ID_Spreading_Cutting_PlanCoupe")
	private Long idSpreadingCuttingPlanCoupe;
	
	@Column(name = "ID_Spreading_Cutting_PlanForeign_PlanCoupe")
	private Long idSpreadingPlanForeignPlanCoupe;

	@Column(name = "ItemNumber_PlanCoupe")
	private String itemNumberPlanCoupe;

	@Column(name = "Description_ItemNumber_PlanCoupe")
	private String descriptionItemNumberPlanCoupe;

	@Column(name = "Laize_PlanCoupe")
	private Double laizePlanCoupe;

	@Column(name = "Category_PlanCoupe")
	private String categoryPlanCoupe;

	@Column(name = "MaxPlie_PlanCoupe")
	private Integer maxPliePlanCoupe;
	
	@Column(name = "Matelassage_Endroit_PlanCoupe")
	private String matelassageEndroitPlanCoupe;

	@Column(name = "Machine_PlanCoupe")
	private String machinePlanCoupe;

	@Column(name = "Placement_PlanCoupe")
	private String placementPlanCoupe;

	@Column(name = "Longueur_Placement_PlanCoupe")
	private Double longueurPlacementPlanCoupe;

	@Column(name = "Longueur_Matelas_PlanCoupe")
	private Double longueurMatelasPlanCoupe;
	
	@Column(name = "Type_Plaque_PlanCoupe")
	private String typePlaquePlanCoupe;

	@Column(name = "Quantity_PerLayer_PlanCoupe")
	private Integer quantityPerLayerPlanCoupe;

	@Column(name = "Overlap_PlanCoupe")
	private String overlapPlanCoupe;

	@Column(name = "Configuration_PlanCoupe")
	private String configurationPlanCoupe;

	@Column(name = "Perimiter_PlanCoupe")
	private Double perimiterPlanCoupe;

	@Column(name = "TempsCoupe_Theorique_PlanCoupe")
	private Double tempsCoupeTheoriquePlanCoupe;

	@Column(name = "TempsCoupe_Real_PlanCoupe")
	private Double tempsCoupeRealPlanCoupe;

	@Column(name = "Usage_Theorique_PlanCoupe")
	private Double usageTheoriquePlanCoupe;

	@Column(name = "Usage_QAD_PlanCoupe")
	private Double usageQADPlanCoupe;

	@Column(name = "Gap_QAD_PlanCoupe")
	private Double gapQADPlanCoupe;

	@Column(name = "Percent_Gap_QAD_PlanCoupe")
	private Double percentGapPlanCoupe;

	@Column(name = "Taux_Scrap_PlanCoupe")
	private Double tauxScrapPlanCoupe;

	@Column(name = "Default_SpreadingCutting_PlanCoupe")
	private Boolean defaultSpreadingCuttingPlanCoupe;
	
	@Column(name = "ID_SpreadingCutting_Parent_PlanCoupe")
	private Long idSpreadingCuttingParentPlanCoupe;

	/*
	[Seuil_Drill_PlanCoupe]
	 */
	@Column(name = "Seuil_Drill_PlanCoupe")
	private Double seuilDrillPlanCoupe;
	
	@Transient
	private List<DrillPlanCoupe> drillPlanCoupes = new ArrayList<DrillPlanCoupe>();

	public Double getSeuilDrillPlanCoupe() {
		return seuilDrillPlanCoupe;
	}

	public void setSeuilDrillPlanCoupe(Double seuilDrillPlanCoupe) {
		this.seuilDrillPlanCoupe = seuilDrillPlanCoupe;
	}

	public List<DrillPlanCoupe> getDrillPlanCoupes() {
		return drillPlanCoupes;
	}

	public void setDrillPlanCoupes(List<DrillPlanCoupe> drillPlanCoupes) {
		this.drillPlanCoupes = drillPlanCoupes;
	}

	public Long getIdSpreadingCuttingPlanCoupe() {
		return idSpreadingCuttingPlanCoupe;
	}

	public void setIdSpreadingCuttingPlanCoupe(Long idSpreadingCuttingPlanCoupe) {
		this.idSpreadingCuttingPlanCoupe = idSpreadingCuttingPlanCoupe;
	}

	public Long getIdSpreadingPlanForeignPlanCoupe() {
		return idSpreadingPlanForeignPlanCoupe;
	}

	public void setIdSpreadingPlanForeignPlanCoupe(Long idSpreadingPlanForeignPlanCoupe) {
		this.idSpreadingPlanForeignPlanCoupe = idSpreadingPlanForeignPlanCoupe;
	}

	public String getItemNumberPlanCoupe() {
		return itemNumberPlanCoupe;
	}

	public void setItemNumberPlanCoupe(String itemNumberPlanCoupe) {
		this.itemNumberPlanCoupe = itemNumberPlanCoupe;
	}

	public String getDescriptionItemNumberPlanCoupe() {
		return descriptionItemNumberPlanCoupe;
	}

	public void setDescriptionItemNumberPlanCoupe(String descriptionItemNumberPlanCoupe) {
		this.descriptionItemNumberPlanCoupe = descriptionItemNumberPlanCoupe;
	}

	public Double getLaizePlanCoupe() {
		return laizePlanCoupe;
	}

	public void setLaizePlanCoupe(Double laizePlanCoupe) {
		this.laizePlanCoupe = laizePlanCoupe;
	}

	public String getCategoryPlanCoupe() {
		return categoryPlanCoupe;
	}

	public void setCategoryPlanCoupe(String categoryPlanCoupe) {
		this.categoryPlanCoupe = categoryPlanCoupe;
	}

	public Integer getMaxPliePlanCoupe() {
		return maxPliePlanCoupe;
	}

	public void setMaxPliePlanCoupe(Integer maxPliePlanCoupe) {
		this.maxPliePlanCoupe = maxPliePlanCoupe;
	}

	public String getMatelassageEndroitPlanCoupe() {
		return matelassageEndroitPlanCoupe;
	}

	public void setMatelassageEndroitPlanCoupe(String matelassageEndroitPlanCoupe) {
		this.matelassageEndroitPlanCoupe = matelassageEndroitPlanCoupe;
	}

	public String getMachinePlanCoupe() {
		return machinePlanCoupe;
	}

	public void setMachinePlanCoupe(String machinePlanCoupe) {
		this.machinePlanCoupe = machinePlanCoupe;
	}

	public String getPlacementPlanCoupe() {
		return placementPlanCoupe;
	}

	public void setPlacementPlanCoupe(String placementPlanCoupe) {
		this.placementPlanCoupe = placementPlanCoupe;
	}

	public Double getLongueurPlacementPlanCoupe() {
		return longueurPlacementPlanCoupe;
	}

	public void setLongueurPlacementPlanCoupe(Double longueurPlacementPlanCoupe) {
		this.longueurPlacementPlanCoupe = longueurPlacementPlanCoupe;
	}

	public Double getLongueurMatelasPlanCoupe() {
		return longueurMatelasPlanCoupe;
	}

	public void setLongueurMatelasPlanCoupe(Double longueurMatelasPlanCoupe) {
		this.longueurMatelasPlanCoupe = longueurMatelasPlanCoupe;
	}

	public String getTypePlaquePlanCoupe() {
		return typePlaquePlanCoupe;
	}

	public void setTypePlaquePlanCoupe(String typePlaquePlanCoupe) {
		this.typePlaquePlanCoupe = typePlaquePlanCoupe;
	}

	public Integer getQuantityPerLayerPlanCoupe() {
		return quantityPerLayerPlanCoupe;
	}

	public void setQuantityPerLayerPlanCoupe(Integer quantityPerLayerPlanCoupe) {
		this.quantityPerLayerPlanCoupe = quantityPerLayerPlanCoupe;
	}

	public String getOverlapPlanCoupe() {
		return overlapPlanCoupe;
	}

	public void setOverlapPlanCoupe(String overlapPlanCoupe) {
		this.overlapPlanCoupe = overlapPlanCoupe;
	}

	public String getConfigurationPlanCoupe() {
		return configurationPlanCoupe;
	}

	public void setConfigurationPlanCoupe(String configurationPlanCoupe) {
		this.configurationPlanCoupe = configurationPlanCoupe;
	}

	public Double getPerimiterPlanCoupe() {
		return perimiterPlanCoupe;
	}

	public void setPerimiterPlanCoupe(Double perimiterPlanCoupe) {
		this.perimiterPlanCoupe = perimiterPlanCoupe;
	}

	public Double getTempsCoupeTheoriquePlanCoupe() {
		return tempsCoupeTheoriquePlanCoupe;
	}

	public void setTempsCoupeTheoriquePlanCoupe(Double tempsCoupeTheoriquePlanCoupe) {
		this.tempsCoupeTheoriquePlanCoupe = tempsCoupeTheoriquePlanCoupe;
	}

	public Double getTempsCoupeRealPlanCoupe() {
		return tempsCoupeRealPlanCoupe;
	}

	public void setTempsCoupeRealPlanCoupe(Double tempsCoupeRealPlanCoupe) {
		this.tempsCoupeRealPlanCoupe = tempsCoupeRealPlanCoupe;
	}

	public Double getUsageTheoriquePlanCoupe() {
		return usageTheoriquePlanCoupe;
	}

	public void setUsageTheoriquePlanCoupe(Double usageTheoriquePlanCoupe) {
		this.usageTheoriquePlanCoupe = usageTheoriquePlanCoupe;
	}

	public Double getUsageQADPlanCoupe() {
		return usageQADPlanCoupe;
	}

	public void setUsageQADPlanCoupe(Double usageQADPlanCoupe) {
		this.usageQADPlanCoupe = usageQADPlanCoupe;
	}

	public Double getGapQADPlanCoupe() {
		return gapQADPlanCoupe;
	}

	public void setGapQADPlanCoupe(Double gapQADPlanCoupe) {
		this.gapQADPlanCoupe = gapQADPlanCoupe;
	}

	public Double getPercentGapPlanCoupe() {
		return percentGapPlanCoupe;
	}

	public void setPercentGapPlanCoupe(Double percentGapPlanCoupe) {
		this.percentGapPlanCoupe = percentGapPlanCoupe;
	}

	public Double getTauxScrapPlanCoupe() {
		return tauxScrapPlanCoupe;
	}

	public void setTauxScrapPlanCoupe(Double tauxScrapPlanCoupe) {
		this.tauxScrapPlanCoupe = tauxScrapPlanCoupe;
	}

	public Boolean getDefaultSpreadingCuttingPlanCoupe() {
		return defaultSpreadingCuttingPlanCoupe;
	}

	public void setDefaultSpreadingCuttingPlanCoupe(Boolean defaultSpreadingCuttingPlanCoupe) {
		this.defaultSpreadingCuttingPlanCoupe = defaultSpreadingCuttingPlanCoupe;
	}

	public Long getIdSpreadingCuttingParentPlanCoupe() {
		return idSpreadingCuttingParentPlanCoupe;
	}

	public void setIdSpreadingCuttingParentPlanCoupe(Long idSpreadingCuttingParentPlanCoupe) {
		this.idSpreadingCuttingParentPlanCoupe = idSpreadingCuttingParentPlanCoupe;
	}

	
}
