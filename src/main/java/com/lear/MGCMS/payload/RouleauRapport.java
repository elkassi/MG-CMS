package com.lear.MGCMS.payload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class RouleauRapport {
	private String reftissu;
	private String lotFrs;
	private String idRouleau;
	private Double laize;
	private Double retour;
	private String serie;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;
	private String tableMatelassage;

	private String locationMP;
	private Double quantityMP;

	private String statut;

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public Double getQuantityMP() {
		return quantityMP;
	}

	public void setQuantityMP(Double quantityMP) {
		this.quantityMP = quantityMP;
	}

	public String getLocationMP() {
		return locationMP;
	}

	public void setLocationMP(String locationMP) {
		this.locationMP = locationMP;
	}

	public String getReftissu() {
		return reftissu;
	}

	public void setReftissu(String reftissu) {
		this.reftissu = reftissu;
	}

	public String getLotFrs() {
		return lotFrs;
	}
	public void setLotFrs(String lotFrs) {
		this.lotFrs = lotFrs;
	}
	public String getIdRouleau() {
		return idRouleau;
	}
	public void setIdRouleau(String idRouleau) {
		this.idRouleau = idRouleau;
	}
	public Double getLaize() {
		return laize;
	}
	public void setLaize(Double laize) {
		this.laize = laize;
	}
	public Double getRetour() {
		return retour;
	}
	public void setRetour(Double retour) {
		this.retour = retour;
	}
	public String getSerie() {
		return serie;
	}
	public void setSerie(String serie) {
		this.serie = serie;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public String getTableMatelassage() {
		return tableMatelassage;
	}
	public void setTableMatelassage(String tableMatelassage) {
		this.tableMatelassage = tableMatelassage;
	}
	
	
	
}
