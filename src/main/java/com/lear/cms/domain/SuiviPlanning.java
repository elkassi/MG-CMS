package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "suiviplanning")
public class SuiviPlanning {
	
	@Id
	@Column(name = "id")
	private Long id;
	@Column(name = "nof") private String nof;
	@Column(name = "Projet") private String projet;
	@Column(name = "Ref_Prod_Finit") private String refProdFinit;
	@Column(name = "Desi_Prod_Finit") private String desiProdFinit;
	@Column(name = "Ref_Prod_Semi") private String refProdSemi;
	@Column(name = "NbrKits") private String nbrKits;
	@Column(name = "Statu") private String statu;
	@Column(name = "date_suivi") private String datesuivi;
	@Column(name = "Modele") private String modele;
	@Column(name = "Shift") private String shift;
	@Column(name = "HeureDebut") private String heureDebut;
	@Column(name = "HeureFin") private String heureFin;
	@Column(name = "Heure_suivi") private String heuresuivi;
	@Column(name = "HeureDebutC") private String heureDebutC;
	@Column(name = "HeureFinC") private String heureFinC;
	@Column(name = "StatuC") private String statuC;
	@Column(name = "TempsM") private String tempsM;
	@Column(name = "TempsC") private String tempsC;
	@Column(name = "Equipe") private String equipe;
	@Column(name = "datee") private String datee;
	@Column(name = "WOID") private String woid;
	@Column(name = "NSequence") private String nSequence;
	@Column(name = "QtyTotalPartNumber") private String qtyTotalPartNumber;
	@Column(name = "StatusPlan") private String statusPlan;
	@Column(name = "Definition") private String definition;
	@Column(name = "Area_SuiviPlanning") private String areaSuiviPlanning;
	@Column(name = "ID_Plan_SuiviPlanning") private String idPlanSuiviPlanning;
	
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
	public String getProjet() {
		return projet;
	}
	public void setProjet(String projet) {
		this.projet = projet;
	}
	public String getRefProdFinit() {
		return refProdFinit;
	}
	public void setRefProdFinit(String refProdFinit) {
		this.refProdFinit = refProdFinit;
	}
	public String getDesiProdFinit() {
		return desiProdFinit;
	}
	public void setDesiProdFinit(String desiProdFinit) {
		this.desiProdFinit = desiProdFinit;
	}
	public String getRefProdSemi() {
		return refProdSemi;
	}
	public void setRefProdSemi(String refProdSemi) {
		this.refProdSemi = refProdSemi;
	}
	public String getNbrKits() {
		return nbrKits;
	}
	public void setNbrKits(String nbrKits) {
		this.nbrKits = nbrKits;
	}
	public String getStatu() {
		return statu;
	}
	public void setStatu(String statu) {
		this.statu = statu;
	}
	public String getDatesuivi() {
		return datesuivi;
	}
	public void setDatesuivi(String datesuivi) {
		this.datesuivi = datesuivi;
	}
	public String getModele() {
		return modele;
	}
	public void setModele(String modele) {
		this.modele = modele;
	}
	public String getShift() {
		return shift;
	}
	public void setShift(String shift) {
		this.shift = shift;
	}
	public String getHeureDebut() {
		return heureDebut;
	}
	public void setHeureDebut(String heureDebut) {
		this.heureDebut = heureDebut;
	}
	public String getHeureFin() {
		return heureFin;
	}
	public void setHeureFin(String heureFin) {
		this.heureFin = heureFin;
	}
	public String getHeuresuivi() {
		return heuresuivi;
	}
	public void setHeuresuivi(String heuresuivi) {
		this.heuresuivi = heuresuivi;
	}
	public String getHeureDebutC() {
		return heureDebutC;
	}
	public void setHeureDebutC(String heureDebutC) {
		this.heureDebutC = heureDebutC;
	}
	public String getHeureFinC() {
		return heureFinC;
	}
	public void setHeureFinC(String heureFinC) {
		this.heureFinC = heureFinC;
	}
	public String getStatuC() {
		return statuC;
	}
	public void setStatuC(String statuC) {
		this.statuC = statuC;
	}
	public String getTempsM() {
		return tempsM;
	}
	public void setTempsM(String tempsM) {
		this.tempsM = tempsM;
	}
	public String getTempsC() {
		return tempsC;
	}
	public void setTempsC(String tempsC) {
		this.tempsC = tempsC;
	}
	public String getEquipe() {
		return equipe;
	}
	public void setEquipe(String equipe) {
		this.equipe = equipe;
	}
	public String getDatee() {
		return datee;
	}
	public void setDatee(String datee) {
		this.datee = datee;
	}
	public String getWoid() {
		return woid;
	}
	public void setWoid(String woid) {
		this.woid = woid;
	}
	public String getnSequence() {
		return nSequence;
	}
	public void setnSequence(String nSequence) {
		this.nSequence = nSequence;
	}
	public String getQtyTotalPartNumber() {
		return qtyTotalPartNumber;
	}
	public void setQtyTotalPartNumber(String qtyTotalPartNumber) {
		this.qtyTotalPartNumber = qtyTotalPartNumber;
	}
	public String getStatusPlan() {
		return statusPlan;
	}
	public void setStatusPlan(String statusPlan) {
		this.statusPlan = statusPlan;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	public String getAreaSuiviPlanning() {
		return areaSuiviPlanning;
	}
	public void setAreaSuiviPlanning(String areaSuiviPlanning) {
		this.areaSuiviPlanning = areaSuiviPlanning;
	}
	public String getIdPlanSuiviPlanning() {
		return idPlanSuiviPlanning;
	}
	public void setIdPlanSuiviPlanning(String idPlanSuiviPlanning) {
		this.idPlanSuiviPlanning = idPlanSuiviPlanning;
	}
	
	
	
}
