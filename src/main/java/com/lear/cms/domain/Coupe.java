package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "coupe")
@XmlRootElement
public class Coupe {
	
	@Id
	private Long id;
	
	@Column(name = "nof")
	private String nof;
	
	@Column(name = "nserie")
	private Long nserie;
	
	@Column(name = "Datedebut")
	private String datedebut;
	
	@Column(name = "DateFin")
	private String dateFin;
	
	@Column(name = "drill1")
	private String drill1;
	
	@Column(name = "drill2")
	private String drill2;
	
	@Column(name = "origineX")
	private String origineX;
	
	@Column(name = "origineY")
	private String origineY;
	
	@Column(name = "placement")
	private String placement;

	@Column(name = "configuration")
	private String configuration;
	
	@Column(name = "matricule")
	private String matricule;
	
	@Column(name = "machine")
	private String machine;
	
	@Column(name = "TempsCoupe")
	private String tempsCoupe;
	
	@Column(name = "Statut")
	private String statut;
	@Column(name = "drill")
	private String drill;
	
	@Column(name = "matricule2")
	private String matricule2;
	@Column(name = "machine2")
	private String machine2;
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
	public Long getNserie() {
		return nserie;
	}
	public void setNserie(Long nserie) {
		this.nserie = nserie;
	}
	public String getDatedebut() {
		return datedebut;
	}
	public void setDatedebut(String datedebut) {
		this.datedebut = datedebut;
	}
	public String getDateFin() {
		return dateFin;
	}
	public void setDateFin(String dateFin) {
		this.dateFin = dateFin;
	}
	public String getDrill1() {
		return drill1;
	}
	public void setDrill1(String drill1) {
		this.drill1 = drill1;
	}
	public String getDrill2() {
		return drill2;
	}
	public void setDrill2(String drill2) {
		this.drill2 = drill2;
	}
	public String getOrigineX() {
		return origineX;
	}
	public void setOrigineX(String origineX) {
		this.origineX = origineX;
	}
	public String getStatut() {
		return statut;
	}
	public void setStatut(String statut) {
		this.statut = statut;
	}
	public String getOrigineY() {
		return origineY;
	}
	public void setOrigineY(String origineY) {
		this.origineY = origineY;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public String getConfiguration() {
		return configuration;
	}
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}
	public String getMatricule() {
		return matricule;
	}
	public void setMatricule(String matricule) {
		this.matricule = matricule;
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
	
	public String getDrill() {
		return drill;
	}
	public void setDrill(String drill) {
		this.drill = drill;
	}
	public String getMatricule2() {
		return matricule2;
	}
	public void setMatricule2(String matricule2) {
		this.matricule2 = matricule2;
	}
	public String getMachine2() {
		return machine2;
	}
	public void setMachine2(String machine2) {
		this.machine2 = machine2;
	}
	public Coupe() {
		super();
	}
	
	
	
	
	
	
	
	
	


}
