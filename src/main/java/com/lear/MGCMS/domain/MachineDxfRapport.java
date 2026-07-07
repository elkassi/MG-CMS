package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "MachineDxfRapportV2")
@IdClass(MachineDxfRapportId.class)
public class MachineDxfRapport {

	@Id
	private Integer processID;
	private String userName;
	@Id
	private String machineName;
	private String device;
	private String job;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime endTime;
	private String material;
	private Double matConsumption;
	private Double matConsumptionArea;
	private Double cutPathLength;
	private Double cuttingTimeInSecs;
	private Double pTime;
	private Double movementLength;
	private Integer totalCount;
	private Integer count;
	private Integer segments;
	private String state;
	
	@Override
	public String toString() {
		return "MachineDxfRapport [processID=" + processID + ", userName=" + userName + ", machineName=" + machineName
				+ ", device=" + device + ", job=" + job + ", startTime=" + startTime + ", endTime=" + endTime
				+ ", material=" + material + ", matConsumption=" + matConsumption
				+ ", matConsumptionArea=" + matConsumptionArea + ", cutPathLength=" + cutPathLength
				+ ", cuttingTimeInSecs=" + cuttingTimeInSecs + ", pTime=" + pTime + ", movementLength=" + movementLength
				+ ", totalCount=" + totalCount + ", count=" + count + ", segments=" + segments + ", state=" + state
				+ "]";
	}
	
	public Integer getProcessID() {
		return processID;
	}

	public void setProcessID(Integer processID) {
		this.processID = processID;
	}

	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getMachineName() {
		return machineName;
	}
	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}
	public String getDevice() {
		return device;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	public String getJob() {
		return job;
	}
	public void setJob(String job) {
		this.job = job;
	}
	public LocalDateTime getStartTime() {
		return startTime;
	}
	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}
	public LocalDateTime getEndTime() {
		return endTime;
	}
	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}
	public String getMaterial() {
		return material;
	}
	public void setMaterial(String material) {
		this.material = material;
	}
	public Double getMatConsumption() {
		return matConsumption;
	}
	public void setMatConsumption(Double matConsumption) {
		this.matConsumption = matConsumption;
	}
	public Double getMatConsumptionArea() {
		return matConsumptionArea;
	}
	public void setMatConsumptionArea(Double matConsumptionArea) {
		this.matConsumptionArea = matConsumptionArea;
	}
	public Double getCutPathLength() {
		return cutPathLength;
	}
	public void setCutPathLength(Double cutPathLength) {
		this.cutPathLength = cutPathLength;
	}
	public Double getCuttingTimeInSecs() {
		return cuttingTimeInSecs;
	}
	public void setCuttingTimeInSecs(Double cuttingTimeInSecs) {
		this.cuttingTimeInSecs = cuttingTimeInSecs;
	}
	public Double getpTime() {
		return pTime;
	}
	public void setpTime(Double pTime) {
		this.pTime = pTime;
	}
	public Double getMovementLength() {
		return movementLength;
	}
	public void setMovementLength(Double movementLength) {
		this.movementLength = movementLength;
	}
	public Integer getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}
	public Integer getCount() {
		return count;
	}
	public void setCount(Integer count) {
		this.count = count;
	}
	public Integer getSegments() {
		return segments;
	}
	public void setSegments(Integer segments) {
		this.segments = segments;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	
	
}
