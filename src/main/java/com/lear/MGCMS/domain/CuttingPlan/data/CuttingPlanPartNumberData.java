package com.lear.MGCMS.domain.CuttingPlan.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberId;

@Entity
@Table(name = "CuttingPlanPartNumber")
@IdClass(CuttingPlanPartNumberId.class)
public class CuttingPlanPartNumberData {
	
	@Id
	private String partNumber;
	
	@Id
	@Column(name = "cuttingPlan_id")
	private Long cuttingPlan;
	
	private String description;
	private String item;
	private Double quantityPer;
	private Integer quantity;
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public Long getCuttingPlan() {
		return cuttingPlan;
	}
	public void setCuttingPlan(Long cuttingPlan) {
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
	public Double getQuantityPer() {
		return quantityPer;
	}
	public void setQuantityPer(Double quantityPer) {
		this.quantityPer = quantityPer;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	
	

}
