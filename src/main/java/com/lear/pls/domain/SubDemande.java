package com.lear.pls.domain;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class SubDemande {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "demande_id")
	private String demande;
	private String sequence;
	private String pn;
	private String partNumberCoverDesciption;
	
//	@Column(updatable = false)
	private String empNumb;
	

	@NotNull(message = "ce champ ne peut pas être null")
	private int quantite;
	private String poste;
	private String matricule;
	
	private String reponse;
	
	private String pnEmp;
	private String partNumberMaterial;
	private String partNumberMaterialDescription;	
	
	
	private String placement;
	private String config;
	private String laLaizeDemande;
	private String description;
	private String drill1;
	private String drill2;
	private String sens;
	
	private String longueurPlacement;
	private String longueurMatelas;
	
	private String stock;
	private String longueur;
	private String largeur;
	private String total;
	private String resteRouleau;
	private String demandeVariance;
	@Column(name = "zoneRouleau")
	private String zoneRouleau;
	private String placementEmp;
	   
	private String transport;
	
	private boolean printed = false;
	
	private Integer nbrCouche;
	
	private String nlotfrs;
	
	
	
	@Override
	public String toString() {
		return "SubDemande [id=" + id + ", demande=" + demande + ", sequence=" + sequence + ", pn=" + pn
				+ ", partNumberCoverDesciption=" + partNumberCoverDesciption + ", empNumb=" + empNumb + ", quantite="
				+ quantite + ", poste=" + poste + ", matricule=" + matricule + ", reponse=" + reponse + ", pnEmp="
				+ pnEmp + ", partNumberMaterial=" + partNumberMaterial + ", partNumberMaterialDescription="
				+ partNumberMaterialDescription + ", placement=" + placement + ", config=" + config
				+ ", laLaizeDemande=" + laLaizeDemande + ", description=" + description + ", drill1=" + drill1
				+ ", drill2=" + drill2 + ", sens=" + sens + ", longueurPlacement=" + longueurPlacement
				+ ", longueurMatelas=" + longueurMatelas + ", stock=" + stock + ", longueur=" + longueur + ", largeur="
				+ largeur + ", total=" + total + ", resteRouleau=" + resteRouleau + ", demandeVariance="
				+ demandeVariance + ", zoneRouleau=" + zoneRouleau + ", placementEmp=" + placementEmp + ", transport="
				+ transport + ", printed=" + printed + ", nbrCouche=" + nbrCouche + ", nlotfrs=" + nlotfrs + "]";
	}

	public SubDemande() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDemande() {
		return demande;
	}

	public void setDemande(String demande) {
		this.demande = demande;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public String getPn() {
		return pn;
	}

	public void setPn(String pn) {
		this.pn = pn;
	}

	public String getPartNumberCoverDesciption() {
		return partNumberCoverDesciption;
	}

	public void setPartNumberCoverDesciption(String partNumberCoverDesciption) {
		this.partNumberCoverDesciption = partNumberCoverDesciption;
	}

	public String getEmpNumb() {
		return empNumb;
	}

	public void setEmpNumb(String empNumb) {
		this.empNumb = empNumb;
	}

	public int getQuantite() {
		return quantite;
	}

	public void setQuantite(int quantite) {
		this.quantite = quantite;
	}

	public String getPoste() {
		return poste;
	}

	public void setPoste(String poste) {
		this.poste = poste;
	}

	public String getMatricule() {
		return matricule;
	}

	public void setMatricule(String matricule) {
		this.matricule = matricule;
	}

	public String getReponse() {
		return reponse;
	}

	public void setReponse(String reponse) {
		this.reponse = reponse;
	}

	public String getPnEmp() {
		return pnEmp;
	}

	public void setPnEmp(String pnEmp) {
		this.pnEmp = pnEmp;
	}

	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}

	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}

	public String getPartNumberMaterialDescription() {
		return partNumberMaterialDescription;
	}

	public void setPartNumberMaterialDescription(String partNumberMaterialDescription) {
		this.partNumberMaterialDescription = partNumberMaterialDescription;
	}

	public String getPlacement() {
		return placement;
	}

	public void setPlacement(String placement) {
		this.placement = placement;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getLaLaizeDemande() {
		return laLaizeDemande;
	}

	public void setLaLaizeDemande(String laLaizeDemande) {
		this.laLaizeDemande = laLaizeDemande;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getSens() {
		return sens;
	}

	public void setSens(String sens) {
		this.sens = sens;
	}

	public String getLongueurPlacement() {
		return longueurPlacement;
	}

	public void setLongueurPlacement(String longueurPlacement) {
		this.longueurPlacement = longueurPlacement;
	}

	public String getLongueurMatelas() {
		return longueurMatelas;
	}

	public void setLongueurMatelas(String longueurMatelas) {
		this.longueurMatelas = longueurMatelas;
	}

	public String getStock() {
		return stock;
	}

	public void setStock(String stock) {
		this.stock = stock;
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

	public String getTotal() {
		return total;
	}

	public void setTotal(String total) {
		this.total = total;
	}

	public String getResteRouleau() {
		return resteRouleau;
	}

	public void setResteRouleau(String resteRouleau) {
		this.resteRouleau = resteRouleau;
	}

	public String getDemandeVariance() {
		return demandeVariance;
	}

	public void setDemandeVariance(String demandeVariance) {
		this.demandeVariance = demandeVariance;
	}

	public String getZoneRouleau() {
		return zoneRouleau;
	}

	public void setZoneRouleau(String zoneRouleau) {
		this.zoneRouleau = zoneRouleau;
	}

	public String getPlacementEmp() {
		return placementEmp;
	}

	public void setPlacementEmp(String placementEmp) {
		this.placementEmp = placementEmp;
	}

	public String getTransport() {
		return transport;
	}

	public void setTransport(String transport) {
		this.transport = transport;
	}

	public boolean isPrinted() {
		return printed;
	}

	public void setPrinted(boolean printed) {
		this.printed = printed;
	}

	public Integer getNbrCouche() {
		return nbrCouche;
	}

	public void setNbrCouche(Integer nbrCouche) {
		this.nbrCouche = nbrCouche;
	}

	public String getNlotfrs() {
		return nlotfrs;
	}

	public void setNlotfrs(String nlotfrs) {
		this.nlotfrs = nlotfrs;
	}
	
	
	
	
}
