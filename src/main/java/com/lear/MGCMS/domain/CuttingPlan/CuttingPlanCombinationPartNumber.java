package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(CuttingPlanCombinationPartNumberId.class)
public class CuttingPlanCombinationPartNumber {
	
	@Id
	private String partNumber;
	@Id
	@ManyToOne
	@JsonIgnore
	private CuttingPlanCombination cuttingPlanCombination;
	private Integer num;
	private String modele;
	private String description;
	private String item;
	private String combination;
	
	public Integer getNum() {
		return num;
	}
	public void setNum(Integer num) {
		this.num = num;
	}
	public String getModele() {
		return modele;
	}
	public void setModele(String modele) {
		this.modele = modele;
	}
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public CuttingPlanCombination getCuttingPlanCombination() {
		return cuttingPlanCombination;
	}
	public void setCuttingPlanCombination(CuttingPlanCombination cuttingPlanCombination) {
		this.cuttingPlanCombination = cuttingPlanCombination;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public String getCombination() {
		return combination;
	}
	public void setCombination(String combination) {
		this.combination = combination;
	}
	
	

}
