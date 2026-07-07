package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CuttingSpeed {

	@Id
	private String config;
	
	private Double vitesse;

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public Double getVitesse() {
		return vitesse;
	}

	public void setVitesse(Double vitesse) {
		this.vitesse = vitesse;
	}
	
	
}
