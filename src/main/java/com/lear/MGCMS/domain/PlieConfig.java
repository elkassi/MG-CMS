package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ManyToAny;

@Entity
public class PlieConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	private Projet projet;

	private String partNumberMaterial;
	
	private Integer plieOld;
	
	private Integer plieNew;

	private String rotation;

	public String getRotation() {
		return rotation;
	}

	public void setRotation(String rotation) {
		this.rotation = rotation;
	}

	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}

	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Projet getProjet() {
		return projet;
	}

	public void setProjet(Projet projet) {
		this.projet = projet;
	}

	public Integer getPlieOld() {
		return plieOld;
	}

	public void setPlieOld(Integer plieOld) {
		this.plieOld = plieOld;
	}

	public Integer getPlieNew() {
		return plieNew;
	}

	public void setPlieNew(Integer plieNew) {
		this.plieNew = plieNew;
	}
	
	
	
}
