package com.lear.MGCMS.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

//@Entity
public class LieuDetection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "nom est obligatoire")
	private String nom;
	
	@OneToMany(mappedBy="lieuDetection", cascade = CascadeType.ALL)
	private List<Chaine> chaines = new ArrayList<Chaine>();
	
	@ManyToOne
	@JsonIgnore
	private Site site;

	public LieuDetection() {
		super();
	}

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

	public List<Chaine> getChaines() {
		return chaines;
	}

	public void setChaines(List<Chaine> chaines) {
		this.chaines = chaines;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}
	
	
	
	
}
