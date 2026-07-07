package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "CategoryLaize_PlanCoupe")
public class CategoryLaizePlanCoupe {
	
	@Id
	@Column(name = "IDCategory_Plan")
	private int idCategoryPlan;
	
	@Column(name = "ID_ItemForeign_Plan")
	private Integer idItemForeignPlan;
	
	@Column(name = "CategoryName_Plan")
	private String categoryNamePlan;
	
	@Column(name = "DescriptionCategory_Plan")
	private String descriptionCategoryPlan;
	
	@Column(name = "LaizeCategory_Plan")
	private Double laizeCategoryPlan;
	
	@Column(name = "BorneMinCategory_Plan")
	private Double borneMinCategoryPlan;
	
	@Column(name = "BorneMaxCategory_Plan")
	private Double borneMaxCategoryPlan;
	
	@Column(name = "Default_Category_Plan")
	private Boolean defaultCategoryPlan;

	
	public CategoryLaizePlanCoupe() {
		super();
	}

	public int getIdCategoryPlan() {
		return idCategoryPlan;
	}

	public void setIdCategoryPlan(int idCategoryPlan) {
		this.idCategoryPlan = idCategoryPlan;
	}

	public Integer getIdItemForeignPlan() {
		return idItemForeignPlan;
	}

	public void setIdItemForeignPlan(Integer idItemForeignPlan) {
		this.idItemForeignPlan = idItemForeignPlan;
	}

	public String getCategoryNamePlan() {
		return categoryNamePlan;
	}

	public void setCategoryNamePlan(String categoryNamePlan) {
		this.categoryNamePlan = categoryNamePlan;
	}

	public String getDescriptionCategoryPlan() {
		return descriptionCategoryPlan;
	}

	public void setDescriptionCategoryPlan(String descriptionCategoryPlan) {
		this.descriptionCategoryPlan = descriptionCategoryPlan;
	}

	public Double getLaizeCategoryPlan() {
		return laizeCategoryPlan;
	}

	public void setLaizeCategoryPlan(Double laizeCategoryPlan) {
		this.laizeCategoryPlan = laizeCategoryPlan;
	}

	public Double getBorneMinCategoryPlan() {
		return borneMinCategoryPlan;
	}

	public void setBorneMinCategoryPlan(Double borneMinCategoryPlan) {
		this.borneMinCategoryPlan = borneMinCategoryPlan;
	}

	public Double getBorneMaxCategoryPlan() {
		return borneMaxCategoryPlan;
	}

	public void setBorneMaxCategoryPlan(Double borneMaxCategoryPlan) {
		this.borneMaxCategoryPlan = borneMaxCategoryPlan;
	}

	public Boolean getDefaultCategoryPlan() {
		return defaultCategoryPlan;
	}

	public void setDefaultCategoryPlan(Boolean defaultCategoryPlan) {
		this.defaultCategoryPlan = defaultCategoryPlan;
	}
	
	
	
}
