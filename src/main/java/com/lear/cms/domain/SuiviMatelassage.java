package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "suivimatelassage")
public class SuiviMatelassage {
	@Id
	private Long id;
	
	@Column(name = "nserie") private String nserie;
	@Column(name = "nof") private String nof;
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
	@Column(name = "Shift") private String shift;
	@Column(name = "Date") private String date;
	@Column(name = "TempsM") private String tempsM;
	@Column(name = "Definition") private String definition;
	@Column(name = "Area_SuiviMatelassage") private String areaSuiviMatelassage;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getNserie() {
		return nserie;
	}
	public void setNserie(String nserie) {
		this.nserie = nserie;
	}
	public String getNof() {
		return nof;
	}
	public void setNof(String nof) {
		this.nof = nof;
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
	public String getShift() {
		return shift;
	}
	public void setShift(String shift) {
		this.shift = shift;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getTempsM() {
		return tempsM;
	}
	public void setTempsM(String tempsM) {
		this.tempsM = tempsM;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	public String getAreaSuiviMatelassage() {
		return areaSuiviMatelassage;
	}
	public void setAreaSuiviMatelassage(String areaSuiviMatelassage) {
		this.areaSuiviMatelassage = areaSuiviMatelassage;
	}
	
	

}
