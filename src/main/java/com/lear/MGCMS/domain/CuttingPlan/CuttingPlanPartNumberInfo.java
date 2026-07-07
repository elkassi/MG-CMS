package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "CuttingPlanPartNumber")
@IdClass(CuttingPlanPartNumberId.class)
public class CuttingPlanPartNumberInfo {
	
	@Id
	private String partNumber;
	
	@Id
	@Column(name = "cuttingPlan_id")
	private Long cuttingPlan;
	private String description;
	private String item;
	private Double quantityPer;
	private Integer quantity;
	@Transient
	private Long cmsId;
	
	public Long getCmsId() {
		return cmsId;
	}
	public void setCmsId(Long cmsId) {
		this.cmsId = cmsId;
	}
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
