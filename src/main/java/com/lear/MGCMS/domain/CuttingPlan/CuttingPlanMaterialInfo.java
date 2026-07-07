package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity
@Table(name = "CuttingPlanMaterial")
@IdClass(CuttingPlanMaterialId.class)
public class CuttingPlanMaterialInfo {
	
	@Id
	@Column(name = "cuttingPlan_id")
	private Long cuttingPlan;

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

	public Long getCuttingPlan() {
		return cuttingPlan;
	}

	public void setCuttingPlan(Long cuttingPlan) {
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

	public Integer getVitesse() {
		return vitesse;
	}

	public void setVitesse(Integer vitesse) {
		this.vitesse = vitesse;
	}

	public String getRotation() {
		return rotation;
	}

	public void setRotation(String rotation) {
		this.rotation = rotation;
	}

	public Double getPlaque() {
		return plaque;
	}

	public void setPlaque(Double plaque) {
		this.plaque = plaque;
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

	public String getPartNumbers() {
		return partNumbers;
	}

	public void setPartNumbers(String partNumbers) {
		this.partNumbers = partNumbers;
	}

	public Double getQadUsage() {
		return qadUsage;
	}

	public void setQadUsage(Double qadUsage) {
		this.qadUsage = qadUsage;
	}
	
	

}
