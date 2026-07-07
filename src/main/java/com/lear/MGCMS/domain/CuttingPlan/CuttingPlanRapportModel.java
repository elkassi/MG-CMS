package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class CuttingPlanRapportModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JsonIgnore
	private CuttingPlan cuttingPlan;
	
	private String nomPlct;
	private String nomModele;
	private String tissu;
	private String quantite;
	private String optionModele;
	private String codeTaille;
	private String alteration;
	private String dynamique;
	private String nbrModele;
	private String piecesParPaquets;
	private String nbrDeTailles;
	private String ajoutPiece;
	private String taille;
	private String direction;
	private String longueurPlct;
	private String longueurPlctPerc;
	private String dateHeure;
	private String dernModifUtili;
	private String heureDeCreation;
	private String creationUtili;
	private String heureModelePrec;
	private String modifUtilPrecedente;
	public CuttingPlanRapportModel() {
		super();
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
	public String getNomPlct() {
		return nomPlct;
	}
	public void setNomPlct(String nomPlct) {
		this.nomPlct = nomPlct;
	}
	public String getLongueurPlctPerc() {
		return longueurPlctPerc;
	}
	public void setLongueurPlctPerc(String longueurPlctPerc) {
		this.longueurPlctPerc = longueurPlctPerc;
	}
	public String getNomModele() {
		return nomModele;
	}
	public void setNomModele(String nomModele) {
		this.nomModele = nomModele;
	}
	public String getTissu() {
		return tissu;
	}
	public void setTissu(String tissu) {
		this.tissu = tissu;
	}
	public String getQuantite() {
		return quantite;
	}
	public void setQuantite(String quantite) {
		this.quantite = quantite;
	}
	public String getOptionModele() {
		return optionModele;
	}
	public void setOptionModele(String optionModele) {
		this.optionModele = optionModele;
	}
	public String getCodeTaille() {
		return codeTaille;
	}
	public void setCodeTaille(String codeTaille) {
		this.codeTaille = codeTaille;
	}
	public String getAlteration() {
		return alteration;
	}
	public void setAlteration(String alteration) {
		this.alteration = alteration;
	}
	public String getDynamique() {
		return dynamique;
	}
	public void setDynamique(String dynamique) {
		this.dynamique = dynamique;
	}
	public String getNbrModele() {
		return nbrModele;
	}
	public void setNbrModele(String nbrModele) {
		this.nbrModele = nbrModele;
	}
	public String getPiecesParPaquets() {
		return piecesParPaquets;
	}
	public void setPiecesParPaquets(String piecesParPaquets) {
		this.piecesParPaquets = piecesParPaquets;
	}
	public String getNbrDeTailles() {
		return nbrDeTailles;
	}
	public void setNbrDeTailles(String nbrDeTailles) {
		this.nbrDeTailles = nbrDeTailles;
	}
	public String getAjoutPiece() {
		return ajoutPiece;
	}
	public void setAjoutPiece(String ajoutPiece) {
		this.ajoutPiece = ajoutPiece;
	}
	public String getTaille() {
		return taille;
	}
	public void setTaille(String taille) {
		this.taille = taille;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public String getLongueurPlct() {
		return longueurPlct;
	}
	public void setLongueurPlct(String longueurPlct) {
		this.longueurPlct = longueurPlct;
	}
	public String getDateHeure() {
		return dateHeure;
	}
	public void setDateHeure(String dateHeure) {
		this.dateHeure = dateHeure;
	}
	public String getDernModifUtili() {
		return dernModifUtili;
	}
	public void setDernModifUtili(String dernModifUtili) {
		this.dernModifUtili = dernModifUtili;
	}
	public String getHeureDeCreation() {
		return heureDeCreation;
	}
	public void setHeureDeCreation(String heureDeCreation) {
		this.heureDeCreation = heureDeCreation;
	}
	public String getCreationUtili() {
		return creationUtili;
	}
	public void setCreationUtili(String creationUtili) {
		this.creationUtili = creationUtili;
	}
	public String getHeureModelePrec() {
		return heureModelePrec;
	}
	public void setHeureModelePrec(String heureModelePrec) {
		this.heureModelePrec = heureModelePrec;
	}
	public String getModifUtilPrecedente() {
		return modifUtilPrecedente;
	}
	public void setModifUtilPrecedente(String modifUtilPrecedente) {
		this.modifUtilPrecedente = modifUtilPrecedente;
	}
	
	
	
	
	
	
	
	
	
	
	
}
