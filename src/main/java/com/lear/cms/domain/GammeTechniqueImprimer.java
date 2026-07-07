package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "GammeTechniqueImprimer")
public class GammeTechniqueImprimer {

	@Id
	@Column(name = "idGammeImp")
	private Long idGammeImp;
	@Column(name = "TitreImp")
	private String titreImp;
	
	@Column(name = "Code1Imp")
	private String code1Imp;
	@Column(name = "Code3Imp")
	private String code3Imp;
	@Column(name = "Code5Imp")
	private String code5Imp;
	@Column(name = "PartNumberImp")
	private String partNumberImp;
	@Column(name = "DescriptionImp")
	private String descriptionImp;
	
	@Column(name = "ElaborerparImp")
	private String elaborerparImp;
	@Column(name = "DateElaborationImp")
	private String dateElaborationImp;
	@Column(name = "ValiderparImp")
	private String validerparImp;
	@Column(name = "ModifierParImp")
	private String modifierParImp;
	@Column(name = "DateModificationImp")
	private String dateModificationImp;
	@Column(name = "ValiderModParImp")
	private String validerModParImp;
	@Column(name = "ShiftImp")
	private String shiftImp;
	@Column(name = "DateImprissionImp")
	private String dateImprissionImp;
	@Column(name = "DateRechercheImp")
	private String dateRechercheImp;
	@Column(name = "UserNameImp")
	private String userNameImp;
	@Column(name = "IdGamme1")
	private Integer idGamme1;
	@Column(name = "StatusID")
	private String statusID;
	@Column(name = "Supplier_Kit_Imp")
	private String supplierKitImp;
	@Column(name = "Site_Imp")
	private String siteImp;
	@Column(name = "Indice_Label_Imp")
	private String indiceLabelImp;
	@Column(name = "Color_Label_Imp")
	private String colorLabelImp;
	@Column(name = "Customer_PN_Label_Imp")
	private String customerPN_LabelImp;
	@Column(name = "JLR_PN_Label_Imp")
	private String jlr_PNLabelImp;
	@Column(name = "Q_LEvel_Label_Imp")
	private String qLEvelLabelImp;
	@Column(name = "XATN_Label_Imp")
	private String xatnLabelImp;
	@Column(name = "Description_Label_Imp")
	private String descriptionLabelImp;
	@Column(name = "Souligner_Label_Imp")
	private String soulignerLabelImp;
	@Column(name = "Matricule_Lavel_Imp")
	private String matriculeLavelImp;
	
	//
	@Column(name = "NSerieGammeImp")
	private Integer nSerieGammeImp;
	@Column(name = "NSequenceImp")
	private String nSequenceImp;
	@Column(name = "NOFImp")
	private String nofImp;
	@Column(name = "WOIDImp")
	private String woidImp;
	@Column(name = "ECNImp")
	private String ecnImp;
	@Column(name = "PackagingImp")
	private String packagingImp;
	@Column(name = "QuantiteImp")
	private String quantiteImp;
	@Column(name = "NbrImprissionImp")
	private String nbrImprissionImp;
	
		      
		    		  
		      
	
	@Override
	public String toString() {
		return "idGammeImp=" + idGammeImp + ", titreImp=" + titreImp + ", code3Imp=" + code3Imp
				+ ", partNumberImp=" + partNumberImp + ", descriptionImp=" + descriptionImp + ", nSerieGammeImp="
				+ nSerieGammeImp + ", nSequenceImp=" + nSequenceImp + "";
	}
	public GammeTechniqueImprimer() {
		super();
	}
	public GammeTechniqueImprimer(Long idGammeImp, String titreImp, String code1Imp, String code3Imp, String code5Imp,
			String partNumberImp, String descriptionImp, String elaborerparImp, String dateElaborationImp,
			String validerparImp, String modifierParImp, String dateModificationImp, String validerModParImp,
			String shiftImp, String dateImprissionImp, String dateRechercheImp, String userNameImp, Integer idGamme1,
			String statusID, String supplierKitImp, String siteImp, String indiceLabelImp, String colorLabelImp,
			String customerPN_LabelImp, String jlr_PNLabelImp, String qLEvelLabelImp, String xatnLabelImp,
			String descriptionLabelImp, String soulignerLabelImp, String matriculeLavelImp, Integer nSerieGammeImp,
			String nSequenceImp, String nofImp, String woidImp, String ecnImp, String packagingImp, String quantiteImp,
			String nbrImprissionImp) {
		super();
		this.idGammeImp = idGammeImp;
		this.titreImp = titreImp;
		this.code1Imp = code1Imp;
		this.code3Imp = code3Imp;
		this.code5Imp = code5Imp;
		this.partNumberImp = partNumberImp;
		this.descriptionImp = descriptionImp;
		this.elaborerparImp = elaborerparImp;
		this.dateElaborationImp = dateElaborationImp;
		this.validerparImp = validerparImp;
		this.modifierParImp = modifierParImp;
		this.dateModificationImp = dateModificationImp;
		this.validerModParImp = validerModParImp;
		this.shiftImp = shiftImp;
		this.dateImprissionImp = dateImprissionImp;
		this.dateRechercheImp = dateRechercheImp;
		this.userNameImp = userNameImp;
		this.idGamme1 = idGamme1;
		this.statusID = statusID;
		this.supplierKitImp = supplierKitImp;
		this.siteImp = siteImp;
		this.indiceLabelImp = indiceLabelImp;
		this.colorLabelImp = colorLabelImp;
		this.customerPN_LabelImp = customerPN_LabelImp;
		this.jlr_PNLabelImp = jlr_PNLabelImp;
		this.qLEvelLabelImp = qLEvelLabelImp;
		this.xatnLabelImp = xatnLabelImp;
		this.descriptionLabelImp = descriptionLabelImp;
		this.soulignerLabelImp = soulignerLabelImp;
		this.matriculeLavelImp = matriculeLavelImp;
		this.nSerieGammeImp = nSerieGammeImp;
		this.nSequenceImp = nSequenceImp;
		this.nofImp = nofImp;
		this.woidImp = woidImp;
		this.ecnImp = ecnImp;
		this.packagingImp = packagingImp;
		this.quantiteImp = quantiteImp;
		this.nbrImprissionImp = nbrImprissionImp;
	}
	
	public String getElaborerparImp() {
		return elaborerparImp;
	}
	public void setElaborerparImp(String elaborerparImp) {
		this.elaborerparImp = elaborerparImp;
	}
	public String getDateElaborationImp() {
		return dateElaborationImp;
	}
	public void setDateElaborationImp(String dateElaborationImp) {
		this.dateElaborationImp = dateElaborationImp;
	}
	public String getValiderparImp() {
		return validerparImp;
	}
	public void setValiderparImp(String validerparImp) {
		this.validerparImp = validerparImp;
	}
	public String getModifierParImp() {
		return modifierParImp;
	}
	public void setModifierParImp(String modifierParImp) {
		this.modifierParImp = modifierParImp;
	}
	public String getDateModificationImp() {
		return dateModificationImp;
	}
	public void setDateModificationImp(String dateModificationImp) {
		this.dateModificationImp = dateModificationImp;
	}
	public String getValiderModParImp() {
		return validerModParImp;
	}
	public void setValiderModParImp(String validerModParImp) {
		this.validerModParImp = validerModParImp;
	}
	public String getShiftImp() {
		return shiftImp;
	}
	public void setShiftImp(String shiftImp) {
		this.shiftImp = shiftImp;
	}
	public String getDateImprissionImp() {
		return dateImprissionImp;
	}
	public void setDateImprissionImp(String dateImprissionImp) {
		this.dateImprissionImp = dateImprissionImp;
	}
	public String getDateRechercheImp() {
		return dateRechercheImp;
	}
	public void setDateRechercheImp(String dateRechercheImp) {
		this.dateRechercheImp = dateRechercheImp;
	}
	public String getUserNameImp() {
		return userNameImp;
	}
	public void setUserNameImp(String userNameImp) {
		this.userNameImp = userNameImp;
	}
	public Integer getIdGamme1() {
		return idGamme1;
	}
	public void setIdGamme1(Integer idGamme1) {
		this.idGamme1 = idGamme1;
	}
	public String getStatusID() {
		return statusID;
	}
	public void setStatusID(String statusID) {
		this.statusID = statusID;
	}
	public String getSupplierKitImp() {
		return supplierKitImp;
	}
	public void setSupplierKitImp(String supplierKitImp) {
		this.supplierKitImp = supplierKitImp;
	}
	public String getSiteImp() {
		return siteImp;
	}
	public void setSiteImp(String siteImp) {
		this.siteImp = siteImp;
	}
	public String getIndiceLabelImp() {
		return indiceLabelImp;
	}
	public void setIndiceLabelImp(String indiceLabelImp) {
		this.indiceLabelImp = indiceLabelImp;
	}
	public String getColorLabelImp() {
		return colorLabelImp;
	}
	public void setColorLabelImp(String colorLabelImp) {
		this.colorLabelImp = colorLabelImp;
	}
	public String getCustomerPN_LabelImp() {
		return customerPN_LabelImp;
	}
	public void setCustomerPN_LabelImp(String customerPN_LabelImp) {
		this.customerPN_LabelImp = customerPN_LabelImp;
	}
	public String getJlr_PNLabelImp() {
		return jlr_PNLabelImp;
	}
	public void setJlr_PNLabelImp(String jlr_PNLabelImp) {
		this.jlr_PNLabelImp = jlr_PNLabelImp;
	}
	public String getqLEvelLabelImp() {
		return qLEvelLabelImp;
	}
	public void setqLEvelLabelImp(String qLEvelLabelImp) {
		this.qLEvelLabelImp = qLEvelLabelImp;
	}
	public String getXatnLabelImp() {
		return xatnLabelImp;
	}
	public void setXatnLabelImp(String xatnLabelImp) {
		this.xatnLabelImp = xatnLabelImp;
	}
	public String getDescriptionLabelImp() {
		return descriptionLabelImp;
	}
	public void setDescriptionLabelImp(String descriptionLabelImp) {
		this.descriptionLabelImp = descriptionLabelImp;
	}
	public String getSoulignerLabelImp() {
		return soulignerLabelImp;
	}
	public void setSoulignerLabelImp(String soulignerLabelImp) {
		this.soulignerLabelImp = soulignerLabelImp;
	}
	public String getMatriculeLavelImp() {
		return matriculeLavelImp;
	}
	public void setMatriculeLavelImp(String matriculeLavelImp) {
		this.matriculeLavelImp = matriculeLavelImp;
	}
	public String getNbrImprissionImp() {
		return nbrImprissionImp;
	}
	public void setNbrImprissionImp(String nbrImprissionImp) {
		this.nbrImprissionImp = nbrImprissionImp;
	}
	public String getQuantiteImp() {
		return quantiteImp;
	}
	public void setQuantiteImp(String quantiteImp) {
		this.quantiteImp = quantiteImp;
	}
	public String getDescriptionImp() {
		return descriptionImp;
	}
	public void setDescriptionImp(String descriptionImp) {
		this.descriptionImp = descriptionImp;
	}
	public String getWoidImp() {
		return woidImp;
	}
	public void setWoidImp(String woidImp) {
		this.woidImp = woidImp;
	}
	public String getPackagingImp() {
		return packagingImp;
	}
	public void setPackagingImp(String packagingImp) {
		this.packagingImp = packagingImp;
	}
	public String getnSequenceImp() {
		return nSequenceImp;
	}
	public void setnSequenceImp(String nSequenceImp) {
		this.nSequenceImp = nSequenceImp;
	}
	public String getEcnImp() {
		return ecnImp;
	}
	public void setEcnImp(String ecnImp) {
		this.ecnImp = ecnImp;
	}
	public Long getIdGammeImp() {
		return idGammeImp;
	}
	public void setIdGammeImp(Long idGammeImp) {
		this.idGammeImp = idGammeImp;
	}
	public String getTitreImp() {
		return titreImp;
	}
	public void setTitreImp(String titreImp) {
		this.titreImp = titreImp;
	}
	public String getCode1Imp() {
		return code1Imp;
	}
	public void setCode1Imp(String code1Imp) {
		this.code1Imp = code1Imp;
	}
	public String getCode3Imp() {
		return code3Imp;
	}
	public void setCode3Imp(String code3Imp) {
		this.code3Imp = code3Imp;
	}
	public String getCode5Imp() {
		return code5Imp;
	}
	public void setCode5Imp(String code5Imp) {
		this.code5Imp = code5Imp;
	}
	public String getPartNumberImp() {
		return partNumberImp;
	}
	public void setPartNumberImp(String partNumberImp) {
		this.partNumberImp = partNumberImp;
	}
	public Integer getnSerieGammeImp() {
		return nSerieGammeImp;
	}
	public void setnSerieGammeImp(Integer nSerieGammeImp) {
		this.nSerieGammeImp = nSerieGammeImp;
	}
	public String getNofImp() {
		return nofImp;
	}
	public void setNofImp(String nofImp) {
		this.nofImp = nofImp;
	}	
	
}
