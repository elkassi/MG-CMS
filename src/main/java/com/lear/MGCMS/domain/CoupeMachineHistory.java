package com.lear.MGCMS.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(CoupeMachineHistoryId.class)
public class CoupeMachineHistory {

	@Id
	private String machine;
	@Id
	private String fileReport;
	@Id
	private Integer ind;
	private LocalDateTime lineDate;
	private String placement;
	private String errorCode;
	private String type;
	private String extra;

	
	public String getExtra() {
		return extra;
	}
	public void setExtra(String extra) {
		this.extra = extra;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getMachine() {
		return machine;
	}
	public void setMachine(String machine) {
		this.machine = machine;
	}
	public String getFileReport() {
		return fileReport;
	}
	public void setFileReport(String fileReport) {
		this.fileReport = fileReport;
	}
	public Integer getInd() {
		return ind;
	}
	public void setInd(Integer ind) {
		this.ind = ind;
	}
	public LocalDateTime getLineDate() {
		return lineDate;
	}
	public void setLineDate(LocalDateTime lineDate) {
		this.lineDate = lineDate;
	}
	
}
