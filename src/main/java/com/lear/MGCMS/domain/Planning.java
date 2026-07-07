package com.lear.MGCMS.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

@Entity
@IdClass(PlanningId.class)
public class Planning {
	@Id
	private LocalDate planningDate; 
	@Id
	private String shift;
	
	private Integer rowId;
	
	@Id
	private String partNumber; // to continue
	
	private String description;
	private String item;
	private String groupName;
	private String designGroup;
	private String coverGroup;
	private String status;
	private Integer quantity;
	private String color;
	private String commentaire;
	
	
	
	
	public Planning() {
		super();
	}

	

	public String getCommentaire() {
		return commentaire;
	}



	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}



	public Integer getRowId() {
		return rowId;
	}

	public void setRowId(Integer rowId) {
		this.rowId = rowId;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public LocalDate getPlanningDate() {
		return planningDate;
	}

	public void setPlanningDate(LocalDate planningDate) {
		this.planningDate = planningDate;
	}

	public String getShift() {
		return shift;
	}

	public void setShift(String shift) {
		this.shift = shift;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

}

