package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Drill_PlanCoupe")
public class DrillPlanCoupe {
	
	@Id
	@Column(name = "ID_Drill_PlanCoupe")
	private Long idDrillPlanCoupe;
	@Column(name = "ID_CuttingForeign_PlanCoupe")
	private Long idCuttingForeignPlanCoupe;
	@Column(name = "Drill_Plan")
	private String drillPlan;
	public Long getIdDrillPlanCoupe() {
		return idDrillPlanCoupe;
	}
	public void setIdDrillPlanCoupe(Long idDrillPlanCoupe) {
		this.idDrillPlanCoupe = idDrillPlanCoupe;
	}
	public Long getIdCuttingForeignPlanCoupe() {
		return idCuttingForeignPlanCoupe;
	}
	public void setIdCuttingForeignPlanCoupe(Long idCuttingForeignPlanCoupe) {
		this.idCuttingForeignPlanCoupe = idCuttingForeignPlanCoupe;
	}
	public String getDrillPlan() {
		return drillPlan;
	}
	public void setDrillPlan(String drillPlan) {
		this.drillPlan = drillPlan;
	}
	
	

}
