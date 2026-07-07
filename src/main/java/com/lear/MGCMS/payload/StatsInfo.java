package com.lear.MGCMS.payload;

public class StatsInfo {
	
	private String info;
	private Long value;
	
	public StatsInfo(String info, Long value) {
		super();
		this.info = info;
		this.value = value;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	public Long getValue() {
		return value;
	}
	public void setValue(Long value) {
		this.value = value;
	}

	
	
}
