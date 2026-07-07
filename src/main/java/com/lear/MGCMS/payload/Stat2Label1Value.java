package com.lear.MGCMS.payload;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Stat2Label1Value {
	
	private String label1;
	private String label2;
	private Integer value;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm:ss")
	private LocalDateTime date;
	
	public Stat2Label1Value(String label1, String label2, Integer value, LocalDateTime date) {
		super();
		this.label1 = label1;
		this.label2 = label2;
		this.value = value;
		this.date = date;
	}
	public LocalDateTime getDate() {
		return date;
	}
	public void setDate(LocalDateTime date) {
		this.date = date;
	}
	public Stat2Label1Value(String label1, String label2, Integer value) {
		super();
		this.label1 = label1;
		this.label2 = label2;
		this.value = value;
	}
	public Stat2Label1Value() {
		super();
	}
	public String getLabel1() {
		return label1;
	}
	public void setLabel1(String label1) {
		this.label1 = label1;
	}
	public String getLabel2() {
		return label2;
	}
	public void setLabel2(String label2) {
		this.label2 = label2;
	}
	public Integer getValue() {
		return value;
	}
	public void setValue(Integer value) {
		this.value = value;
	}

	
	
}
