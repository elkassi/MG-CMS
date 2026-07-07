package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Interval_ItemMachine_PlanCoupe")
public class IntervalItemMachinePlanCoupe {
	
	@Id
	@Column(name = "ID_Interval_ItemMachine_Plan")
	private int idIntervalItemMachinePlan;
	
	@Column(name = "ID_ItemMachineForeign_Plan")
	private Integer idItemMachineForeignPlan;
	
	@Column(name = "MinPlie_Plan")
	private Integer minPliePlan;
	
	@Column(name = "MaxPlie_Plan")
	private Integer maxPliePlan;
	
	@Column(name = "Configuration_Plan")
	private String configurationPlan;
	
	@Column(name = "MatelassageEndroit_Plan")
	private String matelassageEndroitPlan;

	public int getIdIntervalItemMachinePlan() {
		return idIntervalItemMachinePlan;
	}

	public void setIdIntervalItemMachinePlan(int idIntervalItemMachinePlan) {
		this.idIntervalItemMachinePlan = idIntervalItemMachinePlan;
	}

	public Integer getIdItemMachineForeignPlan() {
		return idItemMachineForeignPlan;
	}

	public void setIdItemMachineForeignPlan(Integer idItemMachineForeignPlan) {
		this.idItemMachineForeignPlan = idItemMachineForeignPlan;
	}

	public Integer getMinPliePlan() {
		return minPliePlan;
	}

	public void setMinPliePlan(Integer minPliePlan) {
		this.minPliePlan = minPliePlan;
	}

	public Integer getMaxPliePlan() {
		return maxPliePlan;
	}

	public void setMaxPliePlan(Integer maxPliePlan) {
		this.maxPliePlan = maxPliePlan;
	}

	public String getConfigurationPlan() {
		return configurationPlan;
	}

	public void setConfigurationPlan(String configurationPlan) {
		this.configurationPlan = configurationPlan;
	}

	public String getMatelassageEndroitPlan() {
		return matelassageEndroitPlan;
	}

	public void setMatelassageEndroitPlan(String matelassageEndroitPlan) {
		this.matelassageEndroitPlan = matelassageEndroitPlan;
	}
	
	
	
}
