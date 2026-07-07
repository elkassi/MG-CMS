package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(ReftissuMarginId.class)
public class ReftissuMargin {
	
	@Id
	@JsonIgnore
	@ManyToOne
	private PartNumberMaterialConfig partNumberMaterialConfig;
	 
	@Id
	private Integer intervalId;
	
	private Double longueurMin;
	
	private Double longueurMax;
	
	// Optional machine field - when null, applies to all machines
	// When set, this margin config is specific to that machine and takes priority
	private String machine;
			
	@Lob
	private String pliesConfig;

	public ReftissuMargin() {
		super();
	}

	public PartNumberMaterialConfig getPartNumberMaterialConfig() {
		return partNumberMaterialConfig;
	}

	public void setPartNumberMaterialConfig(PartNumberMaterialConfig partNumberMaterialConfig) {
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

	public String getPliesConfig() {
		return pliesConfig;
	}

	public void setPliesConfig(String pliesConfig) {
		this.pliesConfig = pliesConfig;
	}
	
	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

}
