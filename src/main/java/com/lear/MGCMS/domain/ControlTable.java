package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ControlTable {

	@Id
	private String poste;
	
	private String pcName;

	public String getPoste() {
		return poste;
	}

	public void setPoste(String poste) {
		this.poste = poste;
	}

	public String getPcName() {
		return pcName;
	}

	public void setPcName(String pcName) {
		this.pcName = pcName;
	}
	
	
}
