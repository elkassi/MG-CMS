package com.lear.MGCMS.domain.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.CodeDefaut;
import com.lear.MGCMS.domain.CodeScrap;

@Entity
@Table(name = "CuttingRequestSerie")
public class CuttingRequestSerieLight {
	
	@Id
	private String serie;
	
	@ManyToOne
	@JoinColumn(name = "cuttingRequest_sequence")
	private CuttingRequestInfo  cuttingRequest;
	
	private String partNumberMaterial;
	private String description;
	private Double longueur;
	private String partNumbers;	
	private String machine;
	private Integer nbrCouche;
	private String placement;
	private Double laize;
	private String config;
	private String drill;
	
	private Double perimetre;
	private Double tempsDeCoupe;

	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime createdAt; 
	private LocalDate planningDate; 
	private String shift;
	
	private LocalDateTime dateDebutMatelassage;
	private LocalDateTime dateFinMatelassage;
	private String statusMatelassage = "Waiting";
	
	private String tableCoupe;
	private String coupeur1;
	private String coupeur2;
	private String statusCoupe = "Waiting";
	private LocalDateTime dateDebutCoupe;
	private LocalDateTime dateFinCoupe;

	private String quantite;



	public String getQuantite() {
		return quantite;
	}

	public void setQuantite(String quantite) {
		this.quantite = quantite;
	}

	public String getSerie() {
		return serie;
	}
	public void setSerie(String serie) {
		this.serie = serie;
	}
	public CuttingRequestInfo getCuttingRequest() {
		return cuttingRequest;
	}
	public void setCuttingRequest(CuttingRequestInfo cuttingRequest) {
		this.cuttingRequest = cuttingRequest;
	}
	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}
	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Double getLongueur() {
		return longueur;
	}
	public void setLongueur(Double longueur) {
		this.longueur = longueur;
	}
	public String getPartNumbers() {
		return partNumbers;
	}
	public void setPartNumbers(String partNumbers) {
		this.partNumbers = partNumbers;
	}
	public String getMachine() {
		return machine;
	}
	public void setMachine(String machine) {
		this.machine = machine;
	}
	public Integer getNbrCouche() {
		return nbrCouche;
	}
	public void setNbrCouche(Integer nbrCouche) {
		this.nbrCouche = nbrCouche;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public Double getLaize() {
		return laize;
	}
	public void setLaize(Double laize) {
		this.laize = laize;
	}
	public String getConfig() {
		return config;
	}
	public void setConfig(String config) {
		this.config = config;
	}
	public String getDrill() {
		return drill;
	}
	public void setDrill(String drill) {
		this.drill = drill;
	}
	public Double getPerimetre() {
		return perimetre;
	}
	public void setPerimetre(Double perimetre) {
		this.perimetre = perimetre;
	}
	public Double getTempsDeCoupe() {
		return tempsDeCoupe;
	}
	public void setTempsDeCoupe(Double tempsDeCoupe) {
		this.tempsDeCoupe = tempsDeCoupe;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDate getPlanningDate() {
		return planningDate;
	}
	public void setPlanningDate(LocalDate planningDate) {
		this.planningDate = planningDate;
	}
	public String getShift() {
		return shift;
	}
	public void setShift(String shift) {
		this.shift = shift;
	}
	public LocalDateTime getDateDebutMatelassage() {
		return dateDebutMatelassage;
	}
	public void setDateDebutMatelassage(LocalDateTime dateDebutMatelassage) {
		this.dateDebutMatelassage = dateDebutMatelassage;
	}
	public LocalDateTime getDateFinMatelassage() {
		return dateFinMatelassage;
	}
	public void setDateFinMatelassage(LocalDateTime dateFinMatelassage) {
		this.dateFinMatelassage = dateFinMatelassage;
	}
	public String getStatusMatelassage() {
		return statusMatelassage;
	}
	public void setStatusMatelassage(String statusMatelassage) {
		this.statusMatelassage = statusMatelassage;
	}
	public String getTableCoupe() {
		return tableCoupe;
	}
	public void setTableCoupe(String tableCoupe) {
		this.tableCoupe = tableCoupe;
	}
	public String getCoupeur1() {
		return coupeur1;
	}
	public void setCoupeur1(String coupeur1) {
		this.coupeur1 = coupeur1;
	}
	public String getCoupeur2() {
		return coupeur2;
	}
	public void setCoupeur2(String coupeur2) {
		this.coupeur2 = coupeur2;
	}
	public String getStatusCoupe() {
		return statusCoupe;
	}
	public void setStatusCoupe(String statusCoupe) {
		this.statusCoupe = statusCoupe;
	}
	public LocalDateTime getDateDebutCoupe() {
		return dateDebutCoupe;
	}
	public void setDateDebutCoupe(LocalDateTime dateDebutCoupe) {
		this.dateDebutCoupe = dateDebutCoupe;
	}
	public LocalDateTime getDateFinCoupe() {
		return dateFinCoupe;
	}
	public void setDateFinCoupe(LocalDateTime dateFinCoupe) {
		this.dateFinCoupe = dateFinCoupe;
	}
	
	
	
}
