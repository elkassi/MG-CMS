package com.lear.MGCMS.exceptions;

public class MatriculeAlreadyExistsResponse {
	private String matricule;

	public String getMatricule() {
		return matricule;
	}

	public void setMatricule(String matricule) {
		this.matricule = matricule;
	}

	public MatriculeAlreadyExistsResponse(String matricule) {
		super();
		this.matricule = matricule;
	}
	
}
