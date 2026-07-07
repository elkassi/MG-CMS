package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(CuttingPlanMaterialPlacementId.class)
public class CuttingPlanMaterialPlacement {
	
	@Id
	private String placement;
	 
	@Id
	@ManyToOne
	@JsonIgnore
	private CuttingPlanMaterial cuttingPlanMaterial;
	
	@Column(name = "partNumbers")
	private String partNumbers;
	private Integer groupPlacement;
	private Boolean activated;
	
	private String machine;
	private Integer maxPlie;
	private Integer maxPlieDrill;
	private Integer maxDrill;
	private Integer nbrCouche;
	private String config;
	private String drill;
	
	private String category;
	private Double laize;
	
	private Double longueur;
	private Double longueurMatelas;//auto
	
	private Double perimetre;
	private Double tempsDeCoupe;
	
	private String pliesConfig;
	private String pliesConfigMarge;
	
	private String espaceRelarge;
	private String verifEndroit;
	private String rotation;

	public String getVerifEndroit() {
		return verifEndroit;
	}

	public void setVerifEndroit(String verifEndroit) {
		this.verifEndroit = verifEndroit;
	}

	public String getRotation() {
		return rotation;
	}

	public void setRotation(String rotation) {
		this.rotation = rotation;
	}

	public String getEspaceRelarge() {
		return espaceRelarge;
	}

	public void setEspaceRelarge(String espaceRelarge) {
		this.espaceRelarge = espaceRelarge;
	}

	public Double getPerimetre() {
		return perimetre;
	}

	public void setPerimetre(Double perimetre) {
		this.perimetre = perimetre;
	}

	public String getPliesConfig() {
		return pliesConfig;
	}

	public void setPliesConfig(String pliesConfig) {
		this.pliesConfig = pliesConfig;
	}

	public String getPliesConfigMarge() {
		return pliesConfigMarge;
	}

	public void setPliesConfigMarge(String pliesConfigMarge) {
		this.pliesConfigMarge = pliesConfigMarge;
	}

	public Double getTempsDeCoupe() {
		return tempsDeCoupe;
	}

	public void setTempsDeCoupe(Double tempsDeCoupe) {
		this.tempsDeCoupe = tempsDeCoupe;
	}

	public Boolean getActivated() {
		return activated;
	}

	public void setActivated(Boolean activated) {
		this.activated = activated;
	}

	public Integer getGroupPlacement() {
		return groupPlacement;
	}

	public void setGroupPlacement(Integer groupPlacement) {
		this.groupPlacement = groupPlacement;
	}

	public Integer getMaxPlie() {
		return maxPlie;
	}

	public void setMaxPlie(Integer maxPlie) {
		this.maxPlie = maxPlie;
	}

	public Integer getMaxPlieDrill() {
		return maxPlieDrill;
	}

	public void setMaxPlieDrill(Integer maxPlieDrill) {
		this.maxPlieDrill = maxPlieDrill;
	}

	public Integer getMaxDrill() {
		return maxDrill;
	}

	public void setMaxDrill(Integer maxDrill) {
		this.maxDrill = maxDrill;
	}

	public String getPartNumbers() {
		return partNumbers;
	}

	public void setPartNumbers(String partNumbers) {
		this.partNumbers = partNumbers;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public Double getLaize() {
		return laize;
	}

	public void setLaize(Double laize) {
		this.laize = laize;
	}

	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public Integer getNbrCouche() {
		return nbrCouche;
	}

	public void setNbrCouche(Integer nbrCouche) {
		this.nbrCouche = nbrCouche;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Double getLongueur() {
		return longueur;
	}

	public void setLongueur(Double longueur) {
		this.longueur = longueur;
	}

	public Double getLongueurMatelas() {
		return longueurMatelas;
	}

	public void setLongueurMatelas(Double longueurMatelas) {
		this.longueurMatelas = longueurMatelas;
	}

	public String getPlacement() {
		return placement;
	}

	public void setPlacement(String placement) {
		this.placement = placement;
	}

	

	public CuttingPlanMaterial getCuttingPlanMaterial() {
		return cuttingPlanMaterial;
	}

	public void setCuttingPlanMaterial(CuttingPlanMaterial cuttingPlanMaterial) {
		this.cuttingPlanMaterial = cuttingPlanMaterial;
	}

	public String getDrill() {
		return drill;
	}

	public void setDrill(String drill) {
		this.drill = drill;
	}
	
	

}
