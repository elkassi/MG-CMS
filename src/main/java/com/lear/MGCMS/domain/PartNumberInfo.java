package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PartNumberInfo {

	@Id
	private String partNumber;
	private String description;
	private String status;
	private String prodLine;
	private String ItemType;
	private String designGroup;
	private String itemGroup;
	private String covertype;
	private String apd;
	private Integer packageQty;
	private Double weight;
	private Double perimetre;
	private Double totalPerimetre;
	private Double tempsDeCoupe;

	public Double getPerimetre() {
		return perimetre;
	}

	public void setPerimetre(Double perimetre) {
		this.perimetre = perimetre;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getProdLine() {
		return prodLine;
	}

	public void setProdLine(String prodLine) {
		this.prodLine = prodLine;
	}

	public String getItemType() {
		return ItemType;
	}

	public void setItemType(String itemType) {
		ItemType = itemType;
	}

	public String getDesignGroup() {
		return designGroup;
	}

	public void setDesignGroup(String designGroup) {
		this.designGroup = designGroup;
	}

	public String getItemGroup() {
		return itemGroup;
	}

	public void setItemGroup(String itemGroup) {
		this.itemGroup = itemGroup;
	}

	public String getCovertype() {
		return covertype;
	}

	public void setCovertype(String covertype) {
		this.covertype = covertype;
	}

	public String getApd() {
		return apd;
	}

	public void setApd(String apd) {
		this.apd = apd;
	}

	public PartNumberInfo() {
		super();
	}
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public Integer getPackageQty() {
		return packageQty;
	}
	public void setPackageQty(Integer packageQty) {
		this.packageQty = packageQty;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getTotalPerimetre() {
		return totalPerimetre;
	}

	public void setTotalPerimetre(Double totalPerimetre) {
		this.totalPerimetre = totalPerimetre;
	}

	public Double getTempsDeCoupe() {
		return tempsDeCoupe;
	}

	public void setTempsDeCoupe(Double tempsDeCoupe) {
		this.tempsDeCoupe = tempsDeCoupe;
	}
}
