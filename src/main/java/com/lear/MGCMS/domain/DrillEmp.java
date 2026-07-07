package com.lear.MGCMS.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DrillEmp {

	@Id
	private String pattern;
	
	private Integer drill1;
	private Integer drill2;
	private String projet;

	private LocalDateTime updateAt;
	
	


	public String getProjet() {
		return projet;
	}

	public void setProjet(String projet) {
		this.projet = projet;
	}

	public LocalDateTime getUpdateAt() {
		return updateAt;
	}

	public void setUpdateAt(LocalDateTime updateAt) {
		this.updateAt = updateAt;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public Integer getDrill1() {
		return drill1;
	}

	public void setDrill1(Integer drill1) {
		this.drill1 = drill1;
	}

	public Integer getDrill2() {
		return drill2;
	}

	public void setDrill2(Integer drill2) {
		this.drill2 = drill2;
	}
	
	
	
}
