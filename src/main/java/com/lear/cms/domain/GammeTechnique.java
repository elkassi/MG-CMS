package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "GammeTechnique")
public class GammeTechnique {
	@Id
	@Column(name = "idGamme")
	private Long idGamme;
	@Column(name = "Titre")
	private String titre;
	@Column(name = "Code1")
	private String code1;
	@Column(name = "Code3")
	private String code3;
	@Column(name = "Code5")
	private String code5;
	@Column(name = "PartNumber")
	private String partNumber;

	@Column(name = "Description")
	private String description;
	@Column(name = "Elaborerpar")
	private String elaborerpar;
	@Column(name = "DateElaboration")
	private String dateElaboration;
	@Column(name = "Validerpar")
	private String validerpar;
	@Column(name = "ModifierPar")
	private String modifierPar;
	@Column(name = "DateModification")
	private String dateModification;
	@Column(name = "ValiderModPar")
	private String validerModPar;
	@Column(name = "Packaging")
	private String packaging;
	@Column(name = "ECN")
	private String ecn;
	@Column(name = "DateUpdate")
	private String dateUpdate;
	@Column(name = "UserName")
	private String userName;

	@Column(name = "Supplier_Kit")
	private String supplierKit;
	@Column(name = "Site")
	private String site;
	@Column(name = "Indice_Label")
	private String indiceLabel;
	@Column(name = "Color_Label")
	private String colorLabel;
	@Column(name = "Customer_PN_Label")
	private String customerPNLabel;
	@Column(name = "JLR_PN_Label")
	private String jlrPNLabel;
	@Column(name = "Q_LEvel_Label")
	private String qLEvelLabel;
	@Column(name = "XATN_Label")
	private String xatnLabel;
	@Column(name = "Description_Label")
	private String descriptionLabel;
	@Column(name = "Souligner_Label")
	private String soulignerLabel;
	@Column(name = "Status_A_M_Gamme")
	private String statusAMGamme;
	@Column(name = "HostName_Gamme")
	private String hostNameGamme;
	@Column(name = "Session_W_Gamme")
	private String sessionWGamme;
	@Column(name = "User_Confirmation_Gamme")
	private String userConfirmationGamme;


	public Long getIdGamme() {
		return idGamme;
	}
	public void setIdGamme(Long idGamme) {
		this.idGamme = idGamme;
	}
	public String getTitre() {
		return titre;
	}
	public void setTitre(String titre) {
		this.titre = titre;
	}
	public String getCode1() {
		return code1;
	}
	public void setCode1(String code1) {
		this.code1 = code1;
	}
	public String getCode3() {
		return code3;
	}
	public void setCode3(String code3) {
		this.code3 = code3;
	}
	public String getCode5() {
		return code5;
	}
	public void setCode5(String code5) {
		this.code5 = code5;
	}
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public String getEcn() {
		return ecn;
	}
	public void setEcn(String ecn) {
		this.ecn = ecn;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getElaborerpar() {
		return elaborerpar;
	}

	public void setElaborerpar(String elaborerpar) {
		this.elaborerpar = elaborerpar;
	}

	public String getDateElaboration() {
		return dateElaboration;
	}

	public void setDateElaboration(String dateElaboration) {
		this.dateElaboration = dateElaboration;
	}

	public String getValiderpar() {
		return validerpar;
	}

	public void setValiderpar(String validerpar) {
		this.validerpar = validerpar;
	}

	public String getModifierPar() {
		return modifierPar;
	}

	public void setModifierPar(String modifierPar) {
		this.modifierPar = modifierPar;
	}

	public String getDateModification() {
		return dateModification;
	}

	public void setDateModification(String dateModification) {
		this.dateModification = dateModification;
	}

	public String getValiderModPar() {
		return validerModPar;
	}

	public void setValiderModPar(String validerModPar) {
		this.validerModPar = validerModPar;
	}

	public String getPackaging() {
		return packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	public String getDateUpdate() {
		return dateUpdate;
	}

	public void setDateUpdate(String dateUpdate) {
		this.dateUpdate = dateUpdate;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getSupplierKit() {
		return supplierKit;
	}

	public void setSupplierKit(String supplierKit) {
		this.supplierKit = supplierKit;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getIndiceLabel() {
		return indiceLabel;
	}

	public void setIndiceLabel(String indiceLabel) {
		this.indiceLabel = indiceLabel;
	}

	public String getColorLabel() {
		return colorLabel;
	}

	public void setColorLabel(String colorLabel) {
		this.colorLabel = colorLabel;
	}

	public String getCustomerPNLabel() {
		return customerPNLabel;
	}

	public void setCustomerPNLabel(String customerPNLabel) {
		this.customerPNLabel = customerPNLabel;
	}

	public String getJlrPNLabel() {
		return jlrPNLabel;
	}

	public void setJlrPNLabel(String jlrPNLabel) {
		this.jlrPNLabel = jlrPNLabel;
	}

	public String getqLEvelLabel() {
		return qLEvelLabel;
	}

	public void setqLEvelLabel(String qLEvelLabel) {
		this.qLEvelLabel = qLEvelLabel;
	}

	public String getXatnLabel() {
		return xatnLabel;
	}

	public void setXatnLabel(String xatnLabel) {
		this.xatnLabel = xatnLabel;
	}

	public String getDescriptionLabel() {
		return descriptionLabel;
	}

	public void setDescriptionLabel(String descriptionLabel) {
		this.descriptionLabel = descriptionLabel;
	}

	public String getSoulignerLabel() {
		return soulignerLabel;
	}

	public void setSoulignerLabel(String soulignerLabel) {
		this.soulignerLabel = soulignerLabel;
	}

	public String getStatusAMGamme() {
		return statusAMGamme;
	}

	public void setStatusAMGamme(String statusAMGamme) {
		this.statusAMGamme = statusAMGamme;
	}

	public String getHostNameGamme() {
		return hostNameGamme;
	}

	public void setHostNameGamme(String hostNameGamme) {
		this.hostNameGamme = hostNameGamme;
	}

	public String getSessionWGamme() {
		return sessionWGamme;
	}

	public void setSessionWGamme(String sessionWGamme) {
		this.sessionWGamme = sessionWGamme;
	}

	public String getUserConfirmationGamme() {
		return userConfirmationGamme;
	}

	public void setUserConfirmationGamme(String userConfirmationGamme) {
		this.userConfirmationGamme = userConfirmationGamme;
	}
}
