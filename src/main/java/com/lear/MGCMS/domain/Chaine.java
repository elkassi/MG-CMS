package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

//@Entity
public class Chaine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "nom est obligatoire")
	private String nom;
	
	@ManyToOne
	@JsonIgnore
	private LieuDetection lieuDetection;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	

	public LieuDetection getLieuDetection() {
		return lieuDetection;
	}

	public void setLieuDetection(LieuDetection lieuDetection) {
		this.lieuDetection = lieuDetection;
	}

	public Chaine() {
		super();
	}
	
	
	
}
