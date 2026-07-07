package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(GammeTechniquePartNumberMaterialId.class)
public class GammeTechniquePartNumberMaterial {
	
	@Id
	@ManyToOne
	@JsonIgnore
	private GammeTechnique gammeTechnique;
	
	@Id
	private String partNumberMaterial;
	
	private Double zoom;
	

	
	public GammeTechnique getGammeTechnique() {
		return gammeTechnique;
	}

	public void setGammeTechnique(GammeTechnique gammeTechnique) {
		this.gammeTechnique = gammeTechnique;
	}

	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}

	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}

	public Double getZoom() {
		return zoom;
	}

	public void setZoom(Double zoom) {
		this.zoom = zoom;
	}
	
	

}
