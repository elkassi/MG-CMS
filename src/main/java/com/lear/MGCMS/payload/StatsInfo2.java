package com.lear.MGCMS.payload;

import javax.persistence.Entity;

import org.springframework.beans.factory.annotation.Autowired;

public class StatsInfo2 {

	private String info;
	private Long value1;
	private Long value2;
	
	public StatsInfo2(String info, Long value1, Long value2) {
		super();
		this.info = info;
		this.value1 = value1;
		this.value2 = value2;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	public Long getValue1() {
		return value1;
	}
	public void setValue1(Long value1) {
		this.value1 = value1;
	}
	public Long getValue2() {
		return value2;
	}
	public void setValue2(Long value2) {
		this.value2 = value2;
	}
	
	
}
