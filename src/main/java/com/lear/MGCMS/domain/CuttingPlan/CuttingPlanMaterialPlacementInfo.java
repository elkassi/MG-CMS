package com.lear.MGCMS.domain.CuttingPlan;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity
@Table(name = "CuttingPlanMaterialPlacement")
@IdClass(CuttingPlanMaterialPlacementInfoId.class)
public class CuttingPlanMaterialPlacementInfo {
	
	@Id
	private String placement;
	 
	@Id
	@Column(name = "cuttingPlanMaterial_partNumberMaterial")
	private String cuttingPlanMaterial;
	
	@Id
	@Column(name = "cuttingPlanMaterial_cuttingPlan_id")
	private Long cuttingPlan;
	
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
	private String verifEndroit;
	private String rotation;

	public String getRotation() {
		return rotation;
	}

	public void setRotation(String rotation) {
		this.rotation = rotation;
	}

	public String getVerifEndroit() {
		return verifEndroit;
	}

	public void setVerifEndroit(String verifEndroit) {
		this.verifEndroit = verifEndroit;
	}

	public Double getPerimetre() {
		return perimetre;
	}
	public void setPerimetre(Double perimetre) {
		this.perimetre = perimetre;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public String getCuttingPlanMaterial() {
		return cuttingPlanMaterial;
	}
	public void setCuttingPlanMaterial(String cuttingPlanMaterial) {
		this.cuttingPlanMaterial = cuttingPlanMaterial;
	}
	public Long getCuttingPlan() {
		return cuttingPlan;
	}
	public void setCuttingPlan(Long cuttingPlan) {
		this.cuttingPlan = cuttingPlan;
	}
	public String getPartNumbers() {
		return partNumbers;
	}
	public void setPartNumbers(String partNumbers) {
		this.partNumbers = partNumbers;
	}
	public Integer getGroupPlacement() {
		return groupPlacement;
	}
	public void setGroupPlacement(Integer groupPlacement) {
		this.groupPlacement = groupPlacement;
	}
	public Boolean getActivated() {
		return activated;
	}
	public void setActivated(Boolean activated) {
		this.activated = activated;
	}
	public String getMachine() {
		return machine;
	}
	public void setMachine(String machine) {
		this.machine = machine;
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
	public Integer getNbrCouche() {
		return nbrCouche;
	}
	public void setNbrCouche(Integer nbrCouche) {
		this.nbrCouche = nbrCouche;
	}
	public String getConfig() {
		return config;
	}
	public void setConfig(String config) {
		this.config = config;
	}
	public String getDrill() {
		return drill;
	}
	public void setDrill(String drill) {
		this.drill = drill;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public Double getLaize() {
		return laize;
	}
	public void setLaize(Double laize) {
		this.laize = laize;
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
	public Double getTempsDeCoupe() {
		return tempsDeCoupe;
	}
	public void setTempsDeCoupe(Double tempsDeCoupe) {
		this.tempsDeCoupe = tempsDeCoupe;
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
	
	

}
