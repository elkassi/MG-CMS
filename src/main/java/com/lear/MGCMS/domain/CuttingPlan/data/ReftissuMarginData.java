package com.lear.MGCMS.domain.CuttingPlan.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.Table;

import com.lear.MGCMS.domain.ReftissuMarginId;

@Entity
@Table(name = "ReftissuMargin")
@IdClass(ReftissuMarginId.class)
public class ReftissuMarginData {

	@Id
	@Column(name = "partNumberMaterialConfig_partNumberMaterial")
	private String partNumberMaterialConfig;

	@Id
	private Integer intervalId;

	private Double longueurMin;

	private Double longueurMax;

	private String machine;

	@Lob
	private String pliesConfig;

	public ReftissuMarginData() {
		super();
	}

	public String getPartNumberMaterialConfig() {
		return partNumberMaterialConfig;
	}

	public void setPartNumberMaterialConfig(String partNumberMaterialConfig) {
		this.partNumberMaterialConfig = partNumberMaterialConfig;
	}

	public Integer getIntervalId() {
		return intervalId;
	}

	public void setIntervalId(Integer intervalId) {
		this.intervalId = intervalId;
	}

	public Double getLongueurMin() {
		return longueurMin;
	}

	public void setLongueurMin(Double longueurMin) {
		this.longueurMin = longueurMin;
	}

	public Double getLongueurMax() {
		return longueurMax;
	}

	public void setLongueurMax(Double longueurMax) {
		this.longueurMax = longueurMax;
	}

	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public String getPliesConfig() {
		return pliesConfig;
	}

	public void setPliesConfig(String pliesConfig) {
		this.pliesConfig = pliesConfig;
	}
}
