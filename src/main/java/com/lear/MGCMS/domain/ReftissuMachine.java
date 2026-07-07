package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(ReftissuMachineId.class)
public class ReftissuMachine {

	@Id
	@JsonIgnore
	@ManyToOne
	private PartNumberMaterialConfig partNumberMaterialConfig;
	 
	@Id
	private String machineType;
	
	private Integer maxPlie;
	
	private Integer maxPlieDrill;
	
	private Integer maxDrill;
	
	private Boolean defaultValue;
	
	@Lob
	private String pliesConfig;

	public ReftissuMachine() {
		super();
	}

	public Boolean getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Boolean defaultValue) {
		this.defaultValue = defaultValue;
	}

	public PartNumberMaterialConfig getPartNumberMaterialConfig() {
		return partNumberMaterialConfig;
	}

	public void setPartNumberMaterialConfig(PartNumberMaterialConfig partNumberMaterialConfig) {
		this.partNumberMaterialConfig = partNumberMaterialConfig;
	}

	public String getMachineType() {
		return machineType;
	}

	public void setMachineType(String machineType) {
		this.machineType = machineType;
	}

	public Integer getMaxPlie() {
		return maxPlie;
	}

	public void setMaxPlie(Integer maxPlie) {
		this.maxPlie = maxPlie;
	}

	public Integer getMaxPlieDrill() {
		return maxPlieDrill;
	}

	public void setMaxPlieDrill(Integer maxPlieDrill) {
		this.maxPlieDrill = maxPlieDrill;
	}

	public Integer getMaxDrill() {
		return maxDrill;
	}

	public void setMaxDrill(Integer maxDrill) {
		this.maxDrill = maxDrill;
	}

	public String getPliesConfig() {
		return pliesConfig;
	}

	public void setPliesConfig(String pliesConfig) {
		this.pliesConfig = pliesConfig;
	}
	
	
	
	
}
