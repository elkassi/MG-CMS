package com.lear.MGCMS.domain.CuttingPlan;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(CuttingPlanMaterialId.class)
public class CuttingPlanMaterial {
	@Id
	@ManyToOne
	@JsonIgnore
	private CuttingPlan cuttingPlan;

	@Id
	private String partNumberMaterial;
	private String description;
	private Integer vitesse;
	private String rotation;
	private Double plaque;
	private String tauxScrap;
	private String matelassageEndroit;
	
	private String partNumbers;
	
	private Double qadUsage;
	
	@OneToMany(mappedBy="cuttingPlanMaterial", cascade = CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<CuttingPlanMaterialPlacement> cuttingPlanMaterialPlacement = new ArrayList<CuttingPlanMaterialPlacement>();

	

	public Double getQadUsage() {
		return qadUsage;
	}

	public void setQadUsage(Double qadUsage) {
		this.qadUsage = qadUsage;
	}

	public String getPartNumbers() {
		return partNumbers;
	}

	public void setPartNumbers(String partNumbers) {
		this.partNumbers = partNumbers;
	}

	public Integer getVitesse() {
		return vitesse;
	}

	public void setVitesse(Integer vitesse) {
		this.vitesse = vitesse;
	}

	public Double getPlaque() {
		return plaque;
	}

	public void setPlaque(Double plaque) {
		this.plaque = plaque;
	}

	public String getRotation() {
		return rotation;
	}

	public void setRotation(String rotation) {
		this.rotation = rotation;
	}

	public String getTauxScrap() {
		return tauxScrap;
	}

	public void setTauxScrap(String tauxScrap) {
		this.tauxScrap = tauxScrap;
	}

	public String getMatelassageEndroit() {
		return matelassageEndroit;
	}

	public void setMatelassageEndroit(String matelassageEndroit) {
		this.matelassageEndroit = matelassageEndroit;
	}

	public CuttingPlan getCuttingPlan() {
		return cuttingPlan;
	}

	public void setCuttingPlan(CuttingPlan cuttingPlan) {
		this.cuttingPlan = cuttingPlan;
	}

	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}

	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<CuttingPlanMaterialPlacement> getCuttingPlanMaterialPlacement() {
		return cuttingPlanMaterialPlacement;
	}

	public void setCuttingPlanMaterialPlacement(List<CuttingPlanMaterialPlacement> cuttingPlanMaterialPlacement) {
		this.cuttingPlanMaterialPlacement = cuttingPlanMaterialPlacement;
	}
	
	
	
	
	
}
