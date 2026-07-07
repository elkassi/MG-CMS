package com.lear.MGCMS.domain.CuttingPlan.data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lear.MGCMS.controller.CuttingPlan.data.CuttingPlanCombinationPartNumberInfoId;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanCombination;

@Entity
@Table(name = "CuttingPlanCombinationPartNumberInfo")
@IdClass(CuttingPlanCombinationPartNumberInfoId.class)
public class CuttingPlanCombinationPartNumberInfo {

	@Id
	private String partNumber;
	@Id
	private Long cuttingPlanCombination;
	private Integer num;
	private String modele;
	private String description;
	private String item;
	private String combination;
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public Long getCuttingPlanCombination() {
		return cuttingPlanCombination;
	}
	public void setCuttingPlanCombination(Long cuttingPlanCombination) {
		this.cuttingPlanCombination = cuttingPlanCombination;
	}
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
