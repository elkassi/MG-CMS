package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Machine {

	@Id
	private String code;
	
	private Double maxLaize;
		
	private Double maxLength;
	
	@ManyToOne
	private MachineType machineType;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Double getMaxLaize() {
		return maxLaize;
	}

	public void setMaxLaize(Double maxLaize) {
		this.maxLaize = maxLaize;
	}

	public Double getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(Double maxLength) {
		this.maxLength = maxLength;
	}

	public MachineType getMachineType() {
		return machineType;
	}

	public void setMachineType(MachineType machineType) {
		this.machineType = machineType;
	}
	
	
	
}
