package com.lear.MGCMS.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@IdClass(PlacementDetailId.class)
public class PlacementDetail {

	@Id
	private Integer ind;
	@Id
	private String placement;
	@Id
	private String folder;
	
	private String pattern;
//	private String description;
//	private String categoriePiece;
//	private String taille;
	private String idPaquet;
	private String nomMedele;
	private String gaucheDroite;
//	@Lob
//	private String content;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt;
	
	@PrePersist
	public void create() {
		this.updatedAt = LocalDateTime.now();
	}
	
	@PreUpdate
	public void update() {
		this.updatedAt = LocalDateTime.now();
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
	public Integer getInd() {
		return ind;
	}
	public void setInd(Integer ind) {
		this.ind = ind;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public String getPattern() {
		return pattern;
	}
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	public String getIdPaquet() {
		return idPaquet;
	}
	public void setIdPaquet(String idPaquet) {
		this.idPaquet = idPaquet;
	}
	public String getNomMedele() {
		return nomMedele;
	}
	public void setNomMedele(String nomMedele) {
		this.nomMedele = nomMedele;
	}
	public String getGaucheDroite() {
		return gaucheDroite;
	}
	public void setGaucheDroite(String gaucheDroite) {
		this.gaucheDroite = gaucheDroite;
	}
	
	
	
}
