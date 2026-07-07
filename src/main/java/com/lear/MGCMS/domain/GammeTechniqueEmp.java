package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(GammeTechniqueEmpId.class)
public class GammeTechniqueEmp {
	
	@Id
	@ManyToOne
	@JsonIgnore
	private GammeTechnique gammeTechnique;
	
	@Id
	private String panelNumber;
	
	private Double labelX;
	private Double labelY;
	private Double labelSize;
	private Integer rotation = 0;
	private Boolean inverse = false;
	
	public GammeTechniqueEmp() {
		super();
	}
	
	public Double getLabelSize() {
		return labelSize;
	}

	public void setLabelSize(Double labelSize) {
		this.labelSize = labelSize;
	}

	public GammeTechnique getGammeTechnique() {
		return gammeTechnique;
	}
	public void setGammeTechnique(GammeTechnique gammeTechnique) {
		this.gammeTechnique = gammeTechnique;
	}
	
	public String getPanelNumber() {
		return panelNumber;
	}

	public void setPanelNumber(String panelNumber) {
		this.panelNumber = panelNumber;
	}

	public Double getLabelX() {
		return labelX;
	}
	public void setLabelX(Double labelX) {
		this.labelX = labelX;
	}
	public Double getLabelY() {
		return labelY;
	}
	public void setLabelY(Double labelY) {
		this.labelY = labelY;
	}
	public Integer getRotation() {
		return rotation;
	}
	public void setRotation(Integer rotation) {
		this.rotation = rotation;
	}
	public Boolean getInverse() {
		return inverse;
	}
	public void setInverse(Boolean inverse) {
		this.inverse = inverse;
	}
	
	
	
	
	

}
