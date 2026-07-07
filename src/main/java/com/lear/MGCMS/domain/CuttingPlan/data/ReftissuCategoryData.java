package com.lear.MGCMS.domain.CuttingPlan.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.Table;

import com.lear.MGCMS.domain.ReftissuCategoryId;

@Entity
@Table(name = "ReftissuCategory")
@IdClass(ReftissuCategoryId.class)
public class ReftissuCategoryData {

	@Id
	@Column(name = "partNumberMaterialConfig_partNumberMaterial")
	private String partNumberMaterialConfig;

	@Id
	private String category;

	@Lob
	private String description;

	private Double borneMin;
	private Double borneMax;

	private Boolean defaultValue;

	public ReftissuCategoryData() {
		super();
	}

	public String getPartNumberMaterialConfig() {
		return partNumberMaterialConfig;
	}

	public void setPartNumberMaterialConfig(String partNumberMaterialConfig) {
		this.partNumberMaterialConfig = partNumberMaterialConfig;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getBorneMin() {
		return borneMin;
	}

	public void setBorneMin(Double borneMin) {
		this.borneMin = borneMin;
	}

	public Double getBorneMax() {
		return borneMax;
	}

	public void setBorneMax(Double borneMax) {
		this.borneMax = borneMax;
	}

	public Boolean getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Boolean defaultValue) {
		this.defaultValue = defaultValue;
	}
}
