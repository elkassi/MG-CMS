package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PartNumberInfo")
public class PartNumberInfo2 {

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

	@Override
	public String toString() {
		return "PartNumberInfo2{" +
				"partNumber='" + partNumber + '\'' +
				", description='" + description + '\'' +
				", status='" + status + '\'' +
				", prodLine='" + prodLine + '\'' +
				", ItemType='" + ItemType + '\'' +
				", designGroup='" + designGroup + '\'' +
				", itemGroup='" + itemGroup + '\'' +
				", covertype='" + covertype + '\'' +
				", apd='" + apd + '\'' +
				'}';
	}

	public String getPartNumber() {
		return partNumber;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
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

}
