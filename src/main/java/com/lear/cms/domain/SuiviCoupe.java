package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "suivicoupe")
public class SuiviCoupe {
	
	@Id
	private Long id;
	
	@Column(name = "nof") private String nof;
	@Column(name = "nserie") private String nserie;
	@Column(name = "modele") private String modele;
	@Column(name = "refTissu") private String refTissu;
	@Column(name = "designation") private String designation;
	@Column(name = "tablee") private String tablee;
	@Column(name = "dateDebut") private String dateDebut;
	@Column(name = "Fin_Incomplet") private String finIncomplet;
	@Column(name = "Debut_Incomplet") private String debutIncomplet;
	@Column(name = "dateFin") private String dateFin;
	@Column(name = "statu") private String statu;
	@Column(name = "machine") private String machine;
	@Column(name = "TempsCoupe") private String tempsCoupe;
	@Column(name = "Shift") private String shift;
	@Column(name = "TempsArret") private String tempsArret;
	@Column(name = "Placement") private String placement;
	@Column(name = "Longueur") private String longueur;
	@Column(name = "NbrCouches") private String nbrCouches;
	@Column(name = "Date") private String date;
	@Column(name = "TempsCoupe_brute") private String tempsCoupeBrute;
	@Column(name = "TempsReactivite") private String tempsReactivite;
	@Column(name = "Type") private String type;
	@Column(name = "tablee2") private String tablee2;
	@Column(name = "shift2") private String shift2;
	@Column(name = "date2") private String date2;
	@Column(name = "Area_SuiviCoupe") private String areaSuiviCoupe;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getNof() {
		return nof;
	}
	public void setNof(String nof) {
		this.nof = nof;
	}
	public String getNserie() {
		return nserie;
	}
	public void setNserie(String nserie) {
		this.nserie = nserie;
	}
	public String getModele() {
		return modele;
	}
	public void setModele(String modele) {
		this.modele = modele;
	}
	public String getRefTissu() {
		return refTissu;
	}
	public void setRefTissu(String refTissu) {
		this.refTissu = refTissu;
	}
	public String getDesignation() {
		return designation;
	}
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	public String getTablee() {
		return tablee;
	}
	public void setTablee(String tablee) {
		this.tablee = tablee;
	}
	public String getDateDebut() {
		return dateDebut;
	}
	public void setDateDebut(String dateDebut) {
		this.dateDebut = dateDebut;
	}
	public String getFinIncomplet() {
		return finIncomplet;
	}
	public void setFinIncomplet(String finIncomplet) {
		this.finIncomplet = finIncomplet;
	}
	public String getDebutIncomplet() {
		return debutIncomplet;
	}
	public void setDebutIncomplet(String debutIncomplet) {
		this.debutIncomplet = debutIncomplet;
	}
	public String getDateFin() {
		return dateFin;
	}
	public void setDateFin(String dateFin) {
		this.dateFin = dateFin;
	}
	public String getStatu() {
		return statu;
	}
	public void setStatu(String statu) {
		this.statu = statu;
	}
	public String getMachine() {
		return machine;
	}
	public void setMachine(String machine) {
		this.machine = machine;
	}
	public String getTempsCoupe() {
		return tempsCoupe;
	}
	public void setTempsCoupe(String tempsCoupe) {
		this.tempsCoupe = tempsCoupe;
	}
	public String getShift() {
		return shift;
	}
	public void setShift(String shift) {
		this.shift = shift;
	}
	public String getTempsArret() {
		return tempsArret;
	}
	public void setTempsArret(String tempsArret) {
		this.tempsArret = tempsArret;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public String getLongueur() {
		return longueur;
	}
	public void setLongueur(String longueur) {
		this.longueur = longueur;
	}
	public String getNbrCouches() {
		return nbrCouches;
	}
	public void setNbrCouches(String nbrCouches) {
		this.nbrCouches = nbrCouches;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getTempsCoupeBrute() {
		return tempsCoupeBrute;
	}
	public void setTempsCoupeBrute(String tempsCoupeBrute) {
		this.tempsCoupeBrute = tempsCoupeBrute;
	}
	public String getTempsReactivite() {
		return tempsReactivite;
	}
	public void setTempsReactivite(String tempsReactivite) {
		this.tempsReactivite = tempsReactivite;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTablee2() {
		return tablee2;
	}
	public void setTablee2(String tablee2) {
		this.tablee2 = tablee2;
	}
	public String getShift2() {
		return shift2;
	}
	public void setShift2(String shift2) {
		this.shift2 = shift2;
	}
	public String getDate2() {
		return date2;
	}
	public void setDate2(String date2) {
		this.date2 = date2;
	}
	public String getAreaSuiviCoupe() {
		return areaSuiviCoupe;
	}
	public void setAreaSuiviCoupe(String areaSuiviCoupe) {
		this.areaSuiviCoupe = areaSuiviCoupe;
	}
	
	


}
