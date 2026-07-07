package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class PartNumberMaterialUpdate {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String pnOld;
	
	private String pnNew;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPnOld() {
		return pnOld;
	}

	public void setPnOld(String pnOld) {
		this.pnOld = pnOld;
	}

	public String getPnNew() {
		return pnNew;
	}

	public void setPnNew(String pnNew) {
		this.pnNew = pnNew;
	}
	
	

}
