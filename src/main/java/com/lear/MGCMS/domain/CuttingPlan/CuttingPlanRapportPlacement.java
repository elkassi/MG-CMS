package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class CuttingPlanRapportPlacement {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JsonIgnore
	private CuttingPlan cuttingPlan;
	private String nomPlct;
	private String nomDefPlct;
	private String longueur;
	private String largeur;
	private String perimetreTotal;
	private String numeroDefPlct;
	private String description;
	private String modeles;
	private String contraintes;
	private String na;
	private String surfaceTotal;
	private String efficience;
	private String pertePercentage;
	private String annotation;
	private String crans;
	private String nbrDeModeles;
	private String placeTailleQt;
	private String totalTailles;
	private String piecesRestantes;
	private String ajoutPieces;
	private Boolean drill1;
	private Boolean drill2;
	public CuttingPlanRapportPlacement() {
		super();
	}


	public Boolean getDrill1() {
		return drill1;
	}

	public void setDrill1(Boolean drill1) {
		this.drill1 = drill1;
	}

	public Boolean getDrill2() {
		return drill2;
	}

	public void setDrill2(Boolean drill2) {
		this.drill2 = drill2;
	}

	public String getCrans() {
		return crans;
	}

	public void setCrans(String crans) {
		this.crans = crans;
	}

	public String getNomPlct() {
		return nomPlct;
	}

	public void setNomPlct(String nomPlct) {
		this.nomPlct = nomPlct;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public CuttingPlan getCuttingPlan() {
		return cuttingPlan;
	}
	public void setCuttingPlan(CuttingPlan cuttingPlan) {
		this.cuttingPlan = cuttingPlan;
	}
	
	public String getNumeroDefPlct() {
		return numeroDefPlct;
	}
	public void setNumeroDefPlct(String numeroDefPlct) {
		this.numeroDefPlct = numeroDefPlct;
	}
	public String getNomDefPlct() {
		return nomDefPlct;
	}
	public void setNomDefPlct(String nomDefPlct) {
		this.nomDefPlct = nomDefPlct;
	}
	public String getLongueur() {
		return longueur;
	}
	public void setLongueur(String longueur) {
		this.longueur = longueur;
	}
	public String getLargeur() {
		return largeur;
	}
	public void setLargeur(String largeur) {
		this.largeur = largeur;
	}
	public String getPerimetreTotal() {
		return perimetreTotal;
	}
	public void setPerimetreTotal(String perimetreTotal) {
		this.perimetreTotal = perimetreTotal;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getModeles() {
		return modeles;
	}
	public void setModeles(String modeles) {
		this.modeles = modeles;
	}
	public String getContraintes() {
		return contraintes;
	}
	public void setContraintes(String contraintes) {
		this.contraintes = contraintes;
	}
	public String getNa() {
		return na;
	}
	public void setNa(String na) {
		this.na = na;
	}
	public String getSurfaceTotal() {
		return surfaceTotal;
	}
	public void setSurfaceTotal(String surfaceTotal) {
		this.surfaceTotal = surfaceTotal;
	}
	public String getEfficience() {
		return efficience;
	}
	public void setEfficience(String efficience) {
		this.efficience = efficience;
	}
	public String getPertePercentage() {
		return pertePercentage;
	}
	public void setPertePercentage(String pertePercentage) {
		this.pertePercentage = pertePercentage;
	}
	public String getAnnotation() {
		return annotation;
	}
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	public String getNbrDeModeles() {
		return nbrDeModeles;
	}
	public void setNbrDeModeles(String nbrDeModeles) {
		this.nbrDeModeles = nbrDeModeles;
	}
	public String getPlaceTailleQt() {
		return placeTailleQt;
	}
	public void setPlaceTailleQt(String placeTailleQt) {
		this.placeTailleQt = placeTailleQt;
	}
	public String getTotalTailles() {
		return totalTailles;
	}
	public void setTotalTailles(String totalTailles) {
		this.totalTailles = totalTailles;
	}
	public String getPiecesRestantes() {
		return piecesRestantes;
	}
	public void setPiecesRestantes(String piecesRestantes) {
		this.piecesRestantes = piecesRestantes;
	}
	public String getAjoutPieces() {
		return ajoutPieces;
	}
	public void setAjoutPieces(String ajoutPieces) {
		this.ajoutPieces = ajoutPieces;
	}
	
	

}
