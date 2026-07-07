package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;


@Entity
@IdClass(PartNumberBoomId.class)
public class PartNumberBoom {
	
	@Id
	private String partNumber;
	private String description;
	
	@Id
	private String partNumberMaterial;
	private String partNumberMaterialDescription;
		
	private String project;
	private String version;
	private String item;
	private Double quantityPer;
	
	
	
	@Override
	public String toString() {
		return "PartNumberBoom [partNumber=" + partNumber + ", description=" + description + ", partNumberMaterial="
				+ partNumberMaterial + ", partNumberMaterialDescription=" + partNumberMaterialDescription + ", project="
				+ project + ", version=" + version + ", item=" + item + ", quantityPer=" + quantityPer + "]";
	}

	public PartNumberBoom() {
		super();
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

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

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
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

	public Double getQuantityPer() {
		return quantityPer;
	}

	public void setQuantityPer(Double quantityPer) {
		this.quantityPer = quantityPer;
	}
	
	
	
	
}
