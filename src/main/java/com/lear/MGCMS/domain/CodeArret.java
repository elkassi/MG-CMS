package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CodeArret {
	@Id
	private String code;
	private String departement;
	private String typeArret;
	private String motifArret;
	private String email;

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDepartement() {
		return departement;
	}
	public void setDepartement(String departement) {
		this.departement = departement;
	}
	public String getTypeArret() {
		return typeArret;
	}
	public void setTypeArret(String typeArret) {
		this.typeArret = typeArret;
	}
	public String getMotifArret() {
		return motifArret;
	}
	public void setMotifArret(String motifArret) {
		this.motifArret = motifArret;
	}
	
}
