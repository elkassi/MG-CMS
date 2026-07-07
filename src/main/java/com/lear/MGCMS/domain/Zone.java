package com.lear.MGCMS.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

@Entity
public class Zone {

	/**
	 * Dispatcher category, set in migration V2_01. STRICT zones hold a fixed
	 * set of machine types (e.g. FirstArticle = Lectra + Lectra IP6). The
	 * single SHARED zone hosts LASER-DXF + LASER-LSR and acts as spillover
	 * when a STRICT zone needs laser work.
	 */
	public enum Category {
		STRICT,
		SHARED
	}

	@Id
	@NotBlank(message = "nom est obligatoire")
	private String nom;
	private String code;
	private String description;
	private String rollLocations;
	private Integer orderInd;

	/** See {@link Category}. Defaulted to STRICT in V2_01. */
	@Enumerated(EnumType.STRING)
	@Column(name = "category", length = 16, nullable = false)
	private Category category = Category.STRICT;

	/**
	 * Soft-active flag. Inactive zones are hidden from the dispatcher / chef-de-zone
	 * pages but kept for history. Defaults to {@code true} at DDL level.
	 */
	@Column(name = "is_active", nullable = false)
	private boolean isActive = true;


	public String getRollLocations() {
		return rollLocations;
	}

	public void setRollLocations(String rollLocations) {
		this.rollLocations = rollLocations;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getOrderInd() {
		return orderInd;
	}

	public void setOrderInd(Integer orderInd) {
		this.orderInd = orderInd;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		this.isActive = active;
	}

}
