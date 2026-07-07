package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;

@Entity
public class Defaut {

	@Id
	@NotBlank(message = "ce champ ne peut pas être null")
	private String code;
	
	@NotBlank(message = "ce champ ne peut pas être null")
	private String description;
	
	private String responsable;
	
	private String typeDefaut;
	
	private Boolean  active = true;

	public Defaut() {
		super();
	}
	
	

	public Boolean getActive() {
		return active;
	}



	public void setActive(Boolean active) {
		this.active = active;
	}



	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getResponsable() {
		return responsable;
	}

	public void setResponsable(String responsable) {
		this.responsable = responsable;
	}

	public String getTypeDefaut() {
		return typeDefaut;
	}

	public void setTypeDefaut(String typeDefaut) {
		this.typeDefaut = typeDefaut;
	}

	

	
	
	
}
