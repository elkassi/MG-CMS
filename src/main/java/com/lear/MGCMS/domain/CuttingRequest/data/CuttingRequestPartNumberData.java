package com.lear.MGCMS.domain.CuttingRequest.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestPartNumberId;

@Entity
@Table(name = "CuttingRequestPartNumber")
@IdClass(CuttingRequestPartNumberId.class)
public class CuttingRequestPartNumberData {
	
	@Id
	private String partNumber;
	
	@Id
	@Column(name = "cuttingRequest_sequence")
	private String cuttingRequest;
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
	public String getCuttingRequest() {
		return cuttingRequest;
	}
	public void setCuttingRequest(String cuttingRequest) {
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
