package com.lear.MGCMS.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@IdClass(PlacementId.class)
public class Placement {

	@Id
	private String placement;
	@Id
	private String folder;
	private String partNumberMaterial;
	private Double longueur;
	private Double largeur;
	private Double efficience;
	private Integer nbrPieces;
//	private String partNumbers;
//	private Double perimetre;
//	private Double surface;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt; 
	private Long lastModified;
	
	
	@PrePersist
	public void create() {
		this.updatedAt = LocalDateTime.now();
	}
	
	@PreUpdate
	public void update() {
		this.updatedAt = LocalDateTime.now();
	}
	

	public Long getLastModified() {
		return lastModified;
	}

	public void setLastModified(Long lastModified) {
		this.lastModified = lastModified;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getFolder() {
		return folder;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}
	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}
	public Double getLongueur() {
		return longueur;
	}
	public void setLongueur(Double longueur) {
		this.longueur = longueur;
	}
	public Double getLargeur() {
		return largeur;
	}
	public void setLargeur(Double largeur) {
		this.largeur = largeur;
	}
	public Double getEfficience() {
		return efficience;
	}

	public void setEfficience(Double efficience) {
		this.efficience = efficience;
	}

	public Integer getNbrPieces() {
		return nbrPieces;
	}
	public void setNbrPieces(Integer nbrPieces) {
		this.nbrPieces = nbrPieces;
	}

	
	
	
}
