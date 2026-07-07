package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "produitfinit")
public class ProduitFinit {
	
	@Id
	@Column(name = "id")
	private Long id;
	
	@Column(name = "noff") private String noff;
	@Column(name = "Ref_Prod_Finit") private String refProdFinit;
	@Column(name = "Desi_Prod_Finit") private String desiProdFinit;
	@Column(name = "Ref_Prod_Semi") private String refProdSemi;
	@Column(name = "NbrKit") private String nbrKit;
	@Column(name = "WOID") private String woid;
	@Column(name = "NSequence") private String nSequence;
	@Column(name = "QtyTotalPartNumber") private String qtyTotalPartNumber;
	@Column(name = "StatusPlan") private String statusPlan;
	@Column(name = "Area_ProduitFinit") private String areaProduitFinit;
	@Column(name = "ID_Plan_ProduiFinit") private String idPlanProduiFinit;
	
	public String getWoid() {
		return woid;
	}
	public void setWoid(String woid) {
		this.woid = woid;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getNoff() {
		return noff;
	}
	public void setNoff(String noff) {
		this.noff = noff;
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
	public String getNbrKit() {
		return nbrKit;
	}
	public void setNbrKit(String nbrKit) {
		this.nbrKit = nbrKit;
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
	public String getAreaProduitFinit() {
		return areaProduitFinit;
	}
	public void setAreaProduitFinit(String areaProduitFinit) {
		this.areaProduitFinit = areaProduitFinit;
	}
	public String getIdPlanProduiFinit() {
		return idPlanProduiFinit;
	}
	public void setIdPlanProduiFinit(String idPlanProduiFinit) {
		this.idPlanProduiFinit = idPlanProduiFinit;
	}
	
	


}
