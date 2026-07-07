package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(ReftissuCategoryId.class)
public class ReftissuCategory {
	
	@Id
	@JsonIgnore
	@ManyToOne
	private PartNumberMaterialConfig partNumberMaterialConfig;
	 
	@Id
	private String category;
	
	@Lob
	private String description;
	
	private Double borneMin;
	private Double borneMax;
	
	private Boolean defaultValue;
	
	

	public ReftissuCategory() {
		super();
	}

	public PartNumberMaterialConfig getPartNumberMaterialConfig() {
		return partNumberMaterialConfig;
	}

	public void setPartNumberMaterialConfig(PartNumberMaterialConfig partNumberMaterialConfig) {
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
