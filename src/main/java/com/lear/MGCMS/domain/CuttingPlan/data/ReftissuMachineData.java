package com.lear.MGCMS.domain.CuttingPlan.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.Table;

import com.lear.MGCMS.domain.ReftissuMachineId;

@Entity
@Table(name = "ReftissuMachine")
@IdClass(ReftissuMachineId.class)
public class ReftissuMachineData {

	@Id
	@Column(name = "partNumberMaterialConfig_partNumberMaterial")
	private String partNumberMaterialConfig;

	@Id
	private String machineType;

	private Integer maxPlie;

	private Integer maxPlieDrill;

	private Integer maxDrill;

	private Boolean defaultValue;

	@Lob
	private String pliesConfig;

	public ReftissuMachineData() {
		super();
	}

	public String getPartNumberMaterialConfig() {
		return partNumberMaterialConfig;
	}

	public void setPartNumberMaterialConfig(String partNumberMaterialConfig) {
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

	public Boolean getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Boolean defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getPliesConfig() {
		return pliesConfig;
	}

	public void setPliesConfig(String pliesConfig) {
		this.pliesConfig = pliesConfig;
	}
}
