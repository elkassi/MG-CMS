package com.lear.MGCMS.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javassist.expr.NewArray;

@Entity
@Table(name="site")
public class Site {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "nom est obligatoire")
	private String nom;
	
	@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "sites_projets",
            joinColumns = @JoinColumn(name = "site_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "projet_nom", referencedColumnName = "nom")
            )
	private List<Projet> projets= new ArrayList<Projet>();
	
//	@OneToMany(mappedBy="site", cascade = CascadeType.ALL)
//	private List<LieuDetection> lieuDetections = new ArrayList<LieuDetection>();
	
	public Site() {
		super();
	}

	
	
	@Override
	public String toString() {
		return "[nom=" + nom + "]";
	}



	public List<Projet> getProjets() {
		return projets;
	}


	public void setProjets(List<Projet> projets) {
		this.projets = projets;
	}


//	public List<LieuDetection> getLieuDetections() {
//		return lieuDetections;
//	}
//
//
//	public void setLieuDetections(List<LieuDetection> lieuDetections) {
//		this.lieuDetections = lieuDetections;
//	}


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
	
	

}
