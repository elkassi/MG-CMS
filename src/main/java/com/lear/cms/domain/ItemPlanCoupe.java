package com.lear.cms.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Item_PlanCoupe")
public class ItemPlanCoupe {
	@Id
	@Column(name = "ID_Item_Plan")
	private int idItemPlan;
	
	@Column(name = "ItemNumber_Plan")
	private String itemNumberPlan;
	
	@Column(name = "Description_Plan")
	private String descriptionPlan;
	@Column(name = "VitesseCoupe_Plan")
	private String vitesseCoupePlan;
	@Column(name = "Rotation_Plan")
	private String rotationPlan;
	@Column(name = "Plaque_Plan")
	private String plaquePlan;
	@Column(name = "Type_Plan")
	private String typePlan;
	@Column(name = "User_Plan")
	private String userPlan;
	@Column(name = "HostName_User_Plan")
	private String hostNameUserPlan;
	@Column(name = "Session_User_Plan")
	private String sessionUserPlan;
	@Column(name = "Operation_Date_Plan")
	private LocalDate operationDatePlan;
	@Column(name = "Operation_Hour_Plan")
	private LocalTime operationHourPlan;
	@Column(name = "Comment_Plan")
	private String commentPlan;
	@Column(name = "IDDefaultMachine_Plan")
	private Integer idDefaultMachinePlan;
	@Column(name = "TauxScrap_Plan")
	private String tauxScrapPlan;
	
	public ItemPlanCoupe() {
		super();
	}
	public int getIdItemPlan() {
		return idItemPlan;
	}
	public void setIdItemPlan(int idItemPlan) {
		this.idItemPlan = idItemPlan;
	}
	public String getItemNumberPlan() {
		return itemNumberPlan;
	}
	public void setItemNumberPlan(String itemNumberPlan) {
		this.itemNumberPlan = itemNumberPlan;
	}
	public String getDescriptionPlan() {
		return descriptionPlan;
	}
	public void setDescriptionPlan(String descriptionPlan) {
		this.descriptionPlan = descriptionPlan;
	}
	public String getVitesseCoupePlan() {
		return vitesseCoupePlan;
	}
	public void setVitesseCoupePlan(String vitesseCoupePlan) {
		this.vitesseCoupePlan = vitesseCoupePlan;
	}
	public String getRotationPlan() {
		return rotationPlan;
	}
	public void setRotationPlan(String rotationPlan) {
		this.rotationPlan = rotationPlan;
	}
	public String getPlaquePlan() {
		return plaquePlan;
	}
	public void setPlaquePlan(String plaquePlan) {
		this.plaquePlan = plaquePlan;
	}
	public String getTypePlan() {
		return typePlan;
	}
	public void setTypePlan(String typePlan) {
		this.typePlan = typePlan;
	}
	public String getUserPlan() {
		return userPlan;
	}
	public void setUserPlan(String userPlan) {
		this.userPlan = userPlan;
	}
	public String getHostNameUserPlan() {
		return hostNameUserPlan;
	}
	public void setHostNameUserPlan(String hostNameUserPlan) {
		this.hostNameUserPlan = hostNameUserPlan;
	}
	public String getSessionUserPlan() {
		return sessionUserPlan;
	}
	public void setSessionUserPlan(String sessionUserPlan) {
		this.sessionUserPlan = sessionUserPlan;
	}
	public LocalDate getOperationDatePlan() {
		return operationDatePlan;
	}
	public void setOperationDatePlan(LocalDate operationDatePlan) {
		this.operationDatePlan = operationDatePlan;
	}
	public LocalTime getOperationHourPlan() {
		return operationHourPlan;
	}
	public void setOperationHourPlan(LocalTime operationHourPlan) {
		this.operationHourPlan = operationHourPlan;
	}
	public String getCommentPlan() {
		return commentPlan;
	}
	public void setCommentPlan(String commentPlan) {
		this.commentPlan = commentPlan;
	}
	public Integer getIdDefaultMachinePlan() {
		return idDefaultMachinePlan;
	}
	public void setIdDefaultMachinePlan(Integer idDefaultMachinePlan) {
		this.idDefaultMachinePlan = idDefaultMachinePlan;
	}
	public String getTauxScrapPlan() {
		return tauxScrapPlan;
	}
	public void setTauxScrapPlan(String tauxScrapPlan) {
		this.tauxScrapPlan = tauxScrapPlan;
	}
	
	
}
