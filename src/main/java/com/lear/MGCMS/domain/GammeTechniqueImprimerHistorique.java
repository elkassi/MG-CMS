package com.lear.MGCMS.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;

@Entity
public class GammeTechniqueImprimerHistorique {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String modification;
	@ManyToOne
	private User modifierPar;
	
	private LocalDateTime dateModification;

	@PrePersist
	public void created() {
		this.dateModification = LocalDateTime.now();
	}

	
	
	public GammeTechniqueImprimerHistorique(String modification, User modifierPar) {
		super();
		this.modification = modification;
		this.modifierPar = modifierPar;
	}



	public GammeTechniqueImprimerHistorique() {
		super();
	}



	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getModification() {
		return modification;
	}

	public void setModification(String modification) {
		this.modification = modification;
	}

	public User getModifierPar() {
		return modifierPar;
	}

	public void setModifierPar(User modifierPar) {
		this.modifierPar = modifierPar;
	}

	public LocalDateTime getDateModification() {
		return dateModification;
	}

	public void setDateModification(LocalDateTime dateModification) {
		this.dateModification = dateModification;
	}
	
	
	

}
