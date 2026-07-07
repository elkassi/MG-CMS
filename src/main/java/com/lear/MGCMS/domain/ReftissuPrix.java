package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ReftissuPrix {
	@Id
	private String reftissu;
	private Double prix;
	
	public ReftissuPrix() {
		super();
	}
	public ReftissuPrix(String reftissu, Double prix) {
		super();
		this.reftissu = reftissu;
		this.prix = prix;
	}
	public String getReftissu() {
		return reftissu;
	}
	public void setReftissu(String reftissu) {
		this.reftissu = reftissu;
	}
	public Double getPrix() {
		return prix;
	}
	public void setPrix(Double prix) {
		this.prix = prix;
	}

}
