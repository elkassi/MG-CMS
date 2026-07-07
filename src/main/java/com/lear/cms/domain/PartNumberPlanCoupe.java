package com.lear.cms.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PartNumber_PlanCoupe")
public class PartNumberPlanCoupe {
	
	@Id
	@Column(name = "ID_PartNumber_PlanCoupe")
	private Long idPartNumberPlanCoupe;
	
	@Column(name = "ID_PartNumber_PlanForeign_PlanCoupe")
	private Long idPartNumberPlanForeignPlanCoupe;

	@Column(name = "PartNumber_PlanCoupe")
	private String partNumberPlanCoupe;

	@Column(name = "Description_PartNumber_PlanCoupe")
	private String descriptionPartNumberPlanCoupe;

	@Column(name = "KitTextil_PlanCoupe")
	private String kitTextilPlanCoupe;
	
	@Column(name = "Quantity_PartNumber_PlanCoupe")
	private Integer quantityPartNumberPlanCoupe;

	public Long getIdPartNumberPlanCoupe() {
		return idPartNumberPlanCoupe;
	}

	public void setIdPartNumberPlanCoupe(Long idPartNumberPlanCoupe) {
		this.idPartNumberPlanCoupe = idPartNumberPlanCoupe;
	}

	public Long getIdPartNumberPlanForeignPlanCoupe() {
		return idPartNumberPlanForeignPlanCoupe;
	}

	public void setIdPartNumberPlanForeignPlanCoupe(Long idPartNumberPlanForeignPlanCoupe) {
		this.idPartNumberPlanForeignPlanCoupe = idPartNumberPlanForeignPlanCoupe;
	}

	public String getPartNumberPlanCoupe() {
		return partNumberPlanCoupe;
	}

	public void setPartNumberPlanCoupe(String partNumberPlanCoupe) {
		this.partNumberPlanCoupe = partNumberPlanCoupe;
	}

	public String getDescriptionPartNumberPlanCoupe() {
		return descriptionPartNumberPlanCoupe;
	}

	public void setDescriptionPartNumberPlanCoupe(String descriptionPartNumberPlanCoupe) {
		this.descriptionPartNumberPlanCoupe = descriptionPartNumberPlanCoupe;
	}

	public String getKitTextilPlanCoupe() {
		return kitTextilPlanCoupe;
	}

	public void setKitTextilPlanCoupe(String kitTextilPlanCoupe) {
		this.kitTextilPlanCoupe = kitTextilPlanCoupe;
	}

	public Integer getQuantityPartNumberPlanCoupe() {
		return quantityPartNumberPlanCoupe;
	}

	public void setQuantityPartNumberPlanCoupe(Integer quantityPartNumberPlanCoupe) {
		this.quantityPartNumberPlanCoupe = quantityPartNumberPlanCoupe;
	}
	
	

}
