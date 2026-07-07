package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "matlassage")
public class Matlassage implements Cloneable {

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}


	@Id
	@Column(name = "NSERIE")
	private Long nserie;
	
	@Column(name = "NOF")
	private String nof;
	@Column(name = "QUANTITE")
	private String quantite;
	@Column(name = "MODELE")
	private String modele;
	@Column(name = "DATE")
	private String date;
	@Column(name = "HEURE")
	private String heure;
	@Column(name = "Tablee")
	private String tablee;
	@Column(name = "LONGUEUR")
	private String longueur;
	@Column(name = "PLACEMENT")
	private String placement;
	@Column(name = "NCOUCHES")
	private String nCouches;
	@Column(name ="LALAIZEDEMANDE")
	private String laLaizeDemande;
	@Column(name = "REFTISSU")
	private String reftissu;
	@Column(name = "DESCRIPTION")
	private String description;
	@Column(name = "RETURNMAGASIN")
	private String returnMagasin;
	@Column(name = "Sens")
	private String sens;
	@Column(name = "Machine")
	private String machine;
	@Column(name = "Definition")
	private String definition;
	@Column(name = "statu")
	private String statu;
	@Column(name = "MATMATLASSEUR1")
	private String matMatlasseur1;
	@Column(name = "MATMATLASSEUR2")
	private String matMatlasseur2;
	@Column(name = "MATMATLASSEUR3")
	private String matMatlasseur3;
	@Column(name = "MATMATLASSEUR4")
	private String matMatlasseur4;

	@Column(name = "EQUIPE")
	private String equipe;
	//Area_Matelassage
	@Column(name = "AREA_MATELASSAGE")
	private String areaMatelassage;


	public String getAreaMatelassage() {
		return areaMatelassage;
	}

	public void setAreaMatelassage(String areaMatelassage) {
		this.areaMatelassage = areaMatelassage;
	}

	public String getEquipe() {
		return equipe;
	}

	public void setEquipe(String equipe) {
		this.equipe = equipe;
	}

	public String getMatMatlasseur1() {
		return matMatlasseur1;
	}

	public void setMatMatlasseur1(String matMatlasseur1) {
		this.matMatlasseur1 = matMatlasseur1;
	}

	public String getMatMatlasseur2() {
		return matMatlasseur2;
	}

	public void setMatMatlasseur2(String matMatlasseur2) {
		this.matMatlasseur2 = matMatlasseur2;
	}

	public String getMatMatlasseur3() {
		return matMatlasseur3;
	}

	public void setMatMatlasseur3(String matMatlasseur3) {
		this.matMatlasseur3 = matMatlasseur3;
	}

	public String getMatMatlasseur4() {
		return matMatlasseur4;
	}

	public void setMatMatlasseur4(String matMatlasseur4) {
		this.matMatlasseur4 = matMatlasseur4;
	}

	public String getStatu() {
		return statu;
	}

	public void setStatu(String statu) {
		this.statu = statu;
	}

	public String getHeure() {
		return heure;
	}

	public void setHeure(String heure) {
		this.heure = heure;
	}

	public Matlassage() {
		super();
	}

	public Long getNserie() {
		return nserie;
	}

	public void setNserie(Long nserie) {
		this.nserie = nserie;
	}

	public String getNof() {
		return nof;
	}

	public void setNof(String nof) {
		this.nof = nof;
	}

	public String getQuantite() {
		return quantite;
	}

	public void setQuantite(String quantite) {
		this.quantite = quantite;
	}

	public String getModele() {
		return modele;
	}

	public void setModele(String modele) {
		this.modele = modele;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getTablee() {
		return tablee;
	}

	public void setTablee(String tablee) {
		this.tablee = tablee;
	}

	public String getLongueur() {
		return longueur;
	}

	public void setLongueur(String longueur) {
		this.longueur = longueur;
	}

	public String getPlacement() {
		return placement;
	}

	public void setPlacement(String placement) {
		this.placement = placement;
	}

	public String getnCouches() {
		return nCouches;
	}

	public void setnCouches(String nCouches) {
		this.nCouches = nCouches;
	}

	public String getLaLaizeDemande() {
		return laLaizeDemande;
	}

	public void setLaLaizeDemande(String laLaizeDemande) {
		this.laLaizeDemande = laLaizeDemande;
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

	public String getReturnMagasin() {
		return returnMagasin;
	}

	public void setReturnMagasin(String returnMagasin) {
		this.returnMagasin = returnMagasin;
	}

	public String getSens() {
		return sens;
	}

	public void setSens(String sens) {
		this.sens = sens;
	}

	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}
	
}
