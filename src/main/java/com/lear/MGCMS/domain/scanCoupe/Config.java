package com.lear.MGCMS.domain.scanCoupe;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Scan_Config")
public class Config {
	
	@Id
	@Column(name = "param")
	private String param;
	@Column(name = "value")
	private String value;
	public Config() {
		super();
	}
	public String getParam() {
		return param;
	}
	public void setParam(String param) {
		this.param = param;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}
