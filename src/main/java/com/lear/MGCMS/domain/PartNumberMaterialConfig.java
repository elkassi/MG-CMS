package com.lear.MGCMS.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
public class PartNumberMaterialConfig {

	@Id
	private String partNumberMaterial;
	
	private String description;
	
	private Integer vitesse;
	
	private String rotation; // 90 180 FIX
	
	private Double plaque;
	
	private Double tauxScrap;
	
	private String matelassageEndroit;
	
	@Lob
	private String commentaire;

	private Double margeLaizeMin;
	private Double margeLaizeMax;
	private Boolean validated0BF;
	private Boolean validatedIP6;
	private String buffer1IP6;
	private String buffer2IP6;
	private Boolean fipDev;
	@Column(name = "weight_unit")
	private Double weightUnit; // kg per m² of this material

	@ManyToOne
	private User createdBy;
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    @Column(updatable = false)
	private LocalDateTime createdAt;
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime lastUsedDate;

//	private Double margeLaizeMin;
//	private Double margeLaizeMax;
//	private Boolean validated0BF;
	
	@OneToMany(mappedBy="partNumberMaterialConfig", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<ReftissuMachine> reftissuMachines = new ArrayList<ReftissuMachine>();
	
	@OneToMany(mappedBy="partNumberMaterialConfig", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<ReftissuCategory> reftissuCategories = new ArrayList<ReftissuCategory>();
	@OneToMany(mappedBy="partNumberMaterialConfig", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<ReftissuMargin> reftissuMargins = new ArrayList<ReftissuMargin>();
	
	@Transient
	private String changes;

	public Double getMargeLaizeMin() {
		return margeLaizeMin;
	}

	public void setMargeLaizeMin(Double margeLaizeMin) {
		this.margeLaizeMin = margeLaizeMin;
	}

	public Double getMargeLaizeMax() {
		return margeLaizeMax;
	}

	public void setMargeLaizeMax(Double margeLaizeMax) {
		this.margeLaizeMax = margeLaizeMax;
	}

	public Boolean getValidated0BF() {
		return validated0BF;
	}

	public void setValidated0BF(Boolean validated0BF) {
		this.validated0BF = validated0BF;
	}

	public Boolean getValidatedIP6() {
		return validatedIP6;
	}

	public void setValidatedIP6(Boolean validatedIP6) {
		this.validatedIP6 = validatedIP6;
	}

	public String getBuffer1IP6() {
		return buffer1IP6;
	}

	public void setBuffer1IP6(String buffer1IP6) {
		this.buffer1IP6 = buffer1IP6;
	}

	public String getBuffer2IP6() {
		return buffer2IP6;
	}

	public void setBuffer2IP6(String buffer2IP6) {
		this.buffer2IP6 = buffer2IP6;
	}

	public Boolean getFipDev() {
		return fipDev;
	}

	public void setFipDev(Boolean fipDev) {
		this.fipDev = fipDev;
	}

	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


	public PartNumberMaterialConfig() {
		super();
	}

	public String getChanges() {
		return changes;
	}



	public void setChanges(String changes) {
		this.changes = changes;
	}



	public String getMatelassageEndroit() {
		return matelassageEndroit;
	}



	public void setMatelassageEndroit(String matelassageEndroit) {
		this.matelassageEndroit = matelassageEndroit;
	}



	public List<ReftissuMargin> getReftissuMargins() {
		return reftissuMargins;
	}



	public void setReftissuMargins(List<ReftissuMargin> reftissuMargins) {
		this.reftissuMargins = reftissuMargins;
	}



	public List<ReftissuCategory> getReftissuCategories() {
		return reftissuCategories;
	}



	public void setReftissuCategories(List<ReftissuCategory> reftissuCategories) {
		this.reftissuCategories = reftissuCategories;
	}



	public List<ReftissuMachine> getReftissuMachines() {
		return reftissuMachines;
	}



	public void setReftissuMachines(List<ReftissuMachine> reftissuMachines) {
		this.reftissuMachines = reftissuMachines;
	}



	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}

	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getVitesse() {
		return vitesse;
	}

	public void setVitesse(Integer vitesse) {
		this.vitesse = vitesse;
	}

	public String getRotation() {
		return rotation;
	}

	public void setRotation(String rotation) {
		this.rotation = rotation;
	}

	public Double getPlaque() {
		return plaque;
	}

	public void setPlaque(Double plaque) {
		this.plaque = plaque;
	}

	public Double getTauxScrap() {
		return tauxScrap;
	}

	public void setTauxScrap(Double tauxScrap) {
		this.tauxScrap = tauxScrap;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

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

	public LocalDateTime getLastUsedDate() {
		return lastUsedDate;
	}

	public void setLastUsedDate(LocalDateTime lastUsedDate) {
		this.lastUsedDate = lastUsedDate;
	}

	public Double getWeightUnit() {
		return weightUnit;
	}

	public void setWeightUnit(Double weightUnit) {
		this.weightUnit = weightUnit;
	}
}
