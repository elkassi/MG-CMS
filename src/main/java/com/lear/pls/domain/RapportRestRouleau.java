package com.lear.pls.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RapportRestRouleau {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	private Demande pls;

	private String reftissu;

	private String description;

	private Double quantitePLS;

	private Double prixUnit;

	private Double prixTotal;

	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@Transient
	private String defaut;

	@Transient
	private String projet;

	@Transient
	private String typeDefaut;

	@Transient
	private String site;

	public RapportRestRouleau() {
		super();
	}

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Demande getPls() {
		return pls;
	}

	public void setPls(Demande pls) {
		this.pls = pls;
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

	public Double getQuantitePLS() {
		return quantitePLS;
	}

	public void setQuantitePLS(Double quantitePLS) {
		this.quantitePLS = quantitePLS;
	}

	public Double getPrixUnit() {
		return prixUnit;
	}

	public void setPrixUnit(Double prixUnit) {
		this.prixUnit = prixUnit;
	}

	public Double getPrixTotal() {
		return prixTotal;
	}

	public void setPrixTotal(Double prixTotal) {
		this.prixTotal = prixTotal;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getDefaut() {
		return defaut;
	}

	public void setDefaut(String defaut) {
		this.defaut = defaut;
	}

	public String getProjet() {
		return projet;
	}

	public void setProjet(String projet) {
		this.projet = projet;
	}

	public String getTypeDefaut() {
		return typeDefaut;
	}

	public void setTypeDefaut(String typeDefaut) {
		this.typeDefaut = typeDefaut;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}
}
