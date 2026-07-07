package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(CuttingPlanPartNumberId.class)
public class CuttingPlanPartNumber {
	
	@Id
	private String partNumber;
	
	@Id
	@ManyToOne
	@JsonIgnore
	private CuttingPlan cuttingPlan;
	private String description;
	private String item;
	private Double quantityPer;
	private Integer quantity;
	private Double perimetre; // cached per-PN cut perimeter (D,6 piece perimeters summed across the plan's placements)
	
	public CuttingPlanPartNumber() {
		super();
	}
	
	public Double getQuantityPer() {
		return quantityPer;
	}

	public void setQuantityPer(Double quantityPer) {
		this.quantityPer = quantityPer;
	}

	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public CuttingPlan getCuttingPlan() {
		return cuttingPlan;
	}
	public void setCuttingPlan(CuttingPlan cuttingPlan) {
		this.cuttingPlan = cuttingPlan;
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
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Double getPerimetre() {
		return perimetre;
	}

	public void setPerimetre(Double perimetre) {
		this.perimetre = perimetre;
	}

	
	
}
