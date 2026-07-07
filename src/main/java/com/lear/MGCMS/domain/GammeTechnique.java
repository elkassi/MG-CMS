package com.lear.MGCMS.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
public class GammeTechnique {

	@Id
	private String partNumber;
	private String description;
	private String item;
	private String itemcode5;
	private String leatherKit;
	private String supplierKit;
	private Double heightRow;
	
	private String image;
	private Integer imageHeight;
	
	@ManyToOne
	private User createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;
	
	@ManyToOne
	private User updatedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt;
	
	@OneToMany(mappedBy="gammeTechnique", cascade = CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<GammeTechniquePartNumberMaterial> gammeTechniquePartNumberMaterials = new ArrayList<GammeTechniquePartNumberMaterial>();
	
	@OneToMany(mappedBy="gammeTechnique", cascade = CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<GammeTechniqueEmp> gammeTechniqueEmps = new ArrayList<GammeTechniqueEmp>();

	
	
	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public User getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
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

	public String getItemcode5() {
		return itemcode5;
	}

	public void setItemcode5(String itemcode5) {
		this.itemcode5 = itemcode5;
	}

	public String getLeatherKit() {
		return leatherKit;
	}

	public void setLeatherKit(String leatherKit) {
		this.leatherKit = leatherKit;
	}

	public String getSupplierKit() {
		return supplierKit;
	}

	public void setSupplierKit(String supplierKit) {
		this.supplierKit = supplierKit;
	}

	public Double getHeightRow() {
		return heightRow;
	}

	public void setHeightRow(Double heightRow) {
		this.heightRow = heightRow;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public Integer getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(Integer imageHeight) {
		this.imageHeight = imageHeight;
	}

	public List<GammeTechniquePartNumberMaterial> getGammeTechniquePartNumberMaterials() {
		return gammeTechniquePartNumberMaterials;
	}

	public void setGammeTechniquePartNumberMaterials(
			List<GammeTechniquePartNumberMaterial> gammeTechniquePartNumberMaterials) {
		this.gammeTechniquePartNumberMaterials = gammeTechniquePartNumberMaterials;
	}

	public List<GammeTechniqueEmp> getGammeTechniqueEmps() {
		return gammeTechniqueEmps;
	}

	public void setGammeTechniqueEmps(List<GammeTechniqueEmp> gammeTechniqueEmps) {
		this.gammeTechniqueEmps = gammeTechniqueEmps;
	}

	
	
	
}
