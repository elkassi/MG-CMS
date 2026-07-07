package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ItemMachine_PlanCoupe")
public class ItemMachinePlanCoupe {
	
	@Id
	@Column(name = "ID_ItemMachine_Plan")
	private int idItemMachinePlan;
	
	@Column(name = "ID_ItemForeign_Plan")
	private Integer idItemForeignPlan;
	
	@Column(name = "ID_MachineForeign_Plan")
	private Integer idMachineForeignPlan;
	
	@Column(name = "MaxPlieTotal_Plan")
	private Integer maxPlieTotalPlan;
	@Column(name = "MaxPlieDrill_Plan")
	private Integer maxPlieDrillPlan;
	@Column(name = "Default_ItemMachine_Plan")
	private Boolean defaultItemMachinePlan;
	@Column(name = "Remarque_ItemMachine_Plan")
	private String remarqueItemMachinePlan;
	@Column(name = "Seuil_Drill_Plan")
	private Double seuilDrillPlan;

	public int getIdItemMachinePlan() {
		return idItemMachinePlan;
	}
	public void setIdItemMachinePlan(int idItemMachinePlan) {
		this.idItemMachinePlan = idItemMachinePlan;
	}
	public Integer getIdItemForeignPlan() {
		return idItemForeignPlan;
	}
	public void setIdItemForeignPlan(Integer idItemForeignPlan) {
		this.idItemForeignPlan = idItemForeignPlan;
	}
	public Integer getIdMachineForeignPlan() {
		return idMachineForeignPlan;
	}
	public void setIdMachineForeignPlan(Integer idMachineForeignPlan) {
		this.idMachineForeignPlan = idMachineForeignPlan;
	}
	public Integer getMaxPlieTotalPlan() {
		return maxPlieTotalPlan;
	}
	public void setMaxPlieTotalPlan(Integer maxPlieTotalPlan) {
		this.maxPlieTotalPlan = maxPlieTotalPlan;
	}
	public Integer getMaxPlieDrillPlan() {
		return maxPlieDrillPlan;
	}
	public void setMaxPlieDrillPlan(Integer maxPlieDrillPlan) {
		this.maxPlieDrillPlan = maxPlieDrillPlan;
	}
	public Boolean getDefaultItemMachinePlan() {
		return defaultItemMachinePlan;
	}
	public void setDefaultItemMachinePlan(Boolean defaultItemMachinePlan) {
		this.defaultItemMachinePlan = defaultItemMachinePlan;
	}
	public String getRemarqueItemMachinePlan() {
		return remarqueItemMachinePlan;
	}
	public void setRemarqueItemMachinePlan(String remarqueItemMachinePlan) {
		this.remarqueItemMachinePlan = remarqueItemMachinePlan;
	}
	public Double getSeuilDrillPlan() {
		return seuilDrillPlan;
	}
	public void setSeuilDrillPlan(Double seuilDrillPlan) {
		this.seuilDrillPlan = seuilDrillPlan;
	}
	
	
	
}
