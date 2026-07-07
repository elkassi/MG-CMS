package com.lear.MGCMS.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
public class WorkOrder {
	
	@Id
	private String wo;
	private String woid;
	private String item;
	private String partNumber; // to continue
	private String description;
	private String groupName;
	private String designGroup;
	private String coverGroup;
	private String partNumberStatus;
	private Double qtyOpen;
	private Double qtyRejeter;
	private Double qtyCompleted;
	private LocalDate dueDate;
	private String shift;
	private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private LocalDateTime updatedAt;
	
//	@PrePersist
//	public void add () {
//		this.createdAt = LocalDateTime.now();
//	}
//	
//	@PreUpdate
//	public void update() {
//		this.updatedAt = LocalDateTime.now();
//	}
	
	
	
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getWo() {
		return wo;
	}
	public void setWo(String wo) {
		this.wo = wo;
	}
	public String getWoid() {
		return woid;
	}
	public void setWoid(String woid) {
		this.woid = woid;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getDesignGroup() {
		return designGroup;
	}
	public void setDesignGroup(String designGroup) {
		this.designGroup = designGroup;
	}
	public String getCoverGroup() {
		return coverGroup;
	}
	public void setCoverGroup(String coverGroup) {
		this.coverGroup = coverGroup;
	}
	public String getPartNumberStatus() {
		return partNumberStatus;
	}
	public void setPartNumberStatus(String partNumberStatus) {
		this.partNumberStatus = partNumberStatus;
	}
	public Double getQtyOpen() {
		return qtyOpen;
	}
	public void setQtyOpen(Double qtyOpen) {
		this.qtyOpen = qtyOpen;
	}
	public Double getQtyRejeter() {
		return qtyRejeter;
	}
	public void setQtyRejeter(Double qtyRejeter) {
		this.qtyRejeter = qtyRejeter;
	}
	public Double getQtyCompleted() {
		return qtyCompleted;
	}
	public void setQtyCompleted(Double qtyCompleted) {
		this.qtyCompleted = qtyCompleted;
	}
	public LocalDate getDueDate() {
		return dueDate;
	}
	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}
	public String getShift() {
		return shift;
	}
	public void setShift(String shift) {
		this.shift = shift;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	// Deactivation field - allows ROLE_CAD to temporarily deactivate a workOrder
	private Boolean deactivated = false;
	
	public Boolean getDeactivated() {
		return deactivated;
	}
	public void setDeactivated(Boolean deactivated) {
		this.deactivated = deactivated;
	}
	
}
