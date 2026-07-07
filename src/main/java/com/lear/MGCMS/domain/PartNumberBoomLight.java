package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "PartNumberBoom")
@IdClass(PartNumberBoomId.class)
public class PartNumberBoomLight {
	
	@Id
	private String partNumber;
	private String description;
	
	@Id
	private String partNumberMaterial;
	private String partNumberMaterialDescription;
	private String item;
	private Double quantityPer;
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
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
	public PartNumberBoomLight() {
		super();
	}
	@Override
	public String toString() {
		return "PartNumberBoomLight [partNumber=" + partNumber + ", description=" + description
				+ ", partNumberMaterial=" + partNumberMaterial + ", partNumberMaterialDescription="
				+ partNumberMaterialDescription + ", item=" + item + ", quantityPer=" + quantityPer + "]";
	}
	
	

}
