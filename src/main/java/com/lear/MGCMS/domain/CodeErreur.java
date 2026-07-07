package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

@Entity
public class CodeErreur {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String code;
	private String designation;
	private String rootCause;
	private String actionPossible;
	private boolean internevantOperateur;
	private boolean internevantTechLear;
	private boolean internevantTechLectra;
	public CodeErreur() {
		super();
	}
	
	public String getRootCause() {
		return rootCause;
	}

	public void setRootCause(String rootCause) {
		this.rootCause = rootCause;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDesignation() {
		return designation;
	}
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	public String getActionPossible() {
		return actionPossible;
	}
	public void setActionPossible(String actionPossible) {
		this.actionPossible = actionPossible;
	}
	public boolean isInternevantOperateur() {
		return internevantOperateur;
	}
	public void setInternevantOperateur(boolean internevantOperateur) {
		this.internevantOperateur = internevantOperateur;
	}
	public boolean isInternevantTechLear() {
		return internevantTechLear;
	}
	public void setInternevantTechLear(boolean internevantTechLear) {
		this.internevantTechLear = internevantTechLear;
	}
	public boolean isInternevantTechLectra() {
		return internevantTechLectra;
	}
	public void setInternevantTechLectra(boolean internevantTechLectra) {
		this.internevantTechLectra = internevantTechLectra;
	}

	
}
