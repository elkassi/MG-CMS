package com.lear.MGCMS.domain.CuttingRequest;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(CuttingRequestPartNumberId.class)
@Table(name = "CuttingRequestPartNumber")
public class CuttingRequestPartNumberV2 {

	@Id
	private String partNumber;
	
	@Id
	@ManyToOne
	@JsonIgnore
	private CuttingRequestV2 cuttingRequest;
	private String description;
	private String item;
	private Double quantityPer;
	private Integer quantity;
	private String wo;
	private String woid;
	private Integer gammePrinted = 0;
	private Integer packageQty = 10;
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public CuttingRequestV2 getCuttingRequest() {
		return cuttingRequest;
	}
	public void setCuttingRequest(CuttingRequestV2 cuttingRequest) {
		this.cuttingRequest = cuttingRequest;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public Double getQuantityPer() {
		return quantityPer;
	}
	public void setQuantityPer(Double quantityPer) {
		this.quantityPer = quantityPer;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	public String getWo() {
		return wo;
	}
	public void setWo(String wo) {
		this.wo = wo;
	}
	public String getWoid() {
		return woid;
	}
	public void setWoid(String woid) {
		this.woid = woid;
	}
	public Integer getGammePrinted() {
		return gammePrinted;
	}
	public void setGammePrinted(Integer gammePrinted) {
		this.gammePrinted = gammePrinted;
	}
	public Integer getPackageQty() {
		return packageQty;
	}
	public void setPackageQty(Integer packageQty) {
		this.packageQty = packageQty;
	}
	
	
	
}
