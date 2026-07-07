package com.lear.MGCMS.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;

import org.hibernate.annotations.ManyToAny;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
public class Qn {
	
	@Id
	private String numeroQn;
	
	private String site;
	@ManyToOne
	private Projet projet;
	private String reftissu;
	private String description;
	private String typeDefaut;
	private String appliquerSur;
	private String descriptionDefaut;
	private String placement;
	private String digit;
	private String image;
	private String resultat;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime createdAt;
	
	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
	public String getResultat() {
		return resultat;
	}
	public void setResultat(String resultat) {
		this.resultat = resultat;
	}
	public String getDescriptionDefaut() {
		return descriptionDefaut;
	}
	public void setDescriptionDefaut(String descriptionDefaut) {
		this.descriptionDefaut = descriptionDefaut;
	}
	public String getDigit() {
		return digit;
	}
	public void setDigit(String digit) {
		this.digit = digit;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public String getNumeroQn() {
		return numeroQn;
	}
	public void setNumeroQn(String numeroQn) {
		this.numeroQn = numeroQn;
	}
	public String getSite() {
		return site;
	}
	public void setSite(String site) {
		this.site = site;
	}
	
	public Projet getProjet() {
		return projet;
	}
	public void setProjet(Projet projet) {
		this.projet = projet;
	}
	public String getReftissu() {
		return reftissu;
	}
	public void setReftissu(String reftissu) {
		this.reftissu = reftissu;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getTypeDefaut() {
		return typeDefaut;
	}
	public void setTypeDefaut(String typeDefaut) {
		this.typeDefaut = typeDefaut;
	}
	public String getAppliquerSur() {
		return appliquerSur;
	}
	public void setAppliquerSur(String appliquerSur) {
		this.appliquerSur = appliquerSur;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	
}
