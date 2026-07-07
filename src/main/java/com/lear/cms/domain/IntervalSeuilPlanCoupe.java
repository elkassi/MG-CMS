package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Interval_Seuil_PlanCoupe")
public class IntervalSeuilPlanCoupe {
	
	@Id
	@Column(name = "ID_Interval_Seuil_Plan")
	private int idIntervalSeuilPlan;
	@Column(name = "ID_SeuilForeign_Plan")
	private Integer idSeuilForeignPlan;
	@Column(name = "MinPlie_Seuil_Plan")
	private Double minPlieSeuilPlan;
	@Column(name = "MaxPlie_Seuil_Plan")
	private Double maxPlieSeuilPlan;
	@Column(name = "LongueurPlus_Seuil_Plan")
	private Double longueurPlusSeuilPlan;
	public IntervalSeuilPlanCoupe() {
		super();
	}
	public int getIdIntervalSeuilPlan() {
		return idIntervalSeuilPlan;
	}
	public void setIdIntervalSeuilPlan(int idIntervalSeuilPlan) {
		this.idIntervalSeuilPlan = idIntervalSeuilPlan;
	}
	public Integer getIdSeuilForeignPlan() {
		return idSeuilForeignPlan;
	}
	public void setIdSeuilForeignPlan(Integer idSeuilForeignPlan) {
		this.idSeuilForeignPlan = idSeuilForeignPlan;
	}
	public Double getMinPlieSeuilPlan() {
		return minPlieSeuilPlan;
	}
	public void setMinPlieSeuilPlan(Double minPlieSeuilPlan) {
		this.minPlieSeuilPlan = minPlieSeuilPlan;
	}
	public Double getMaxPlieSeuilPlan() {
		return maxPlieSeuilPlan;
	}
	public void setMaxPlieSeuilPlan(Double maxPlieSeuilPlan) {
		this.maxPlieSeuilPlan = maxPlieSeuilPlan;
	}
	public Double getLongueurPlusSeuilPlan() {
		return longueurPlusSeuilPlan;
	}
	public void setLongueurPlusSeuilPlan(Double longueurPlusSeuilPlan) {
		this.longueurPlusSeuilPlan = longueurPlusSeuilPlan;
	}
		

}
