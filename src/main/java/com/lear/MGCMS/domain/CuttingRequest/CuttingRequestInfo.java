package com.lear.MGCMS.domain.CuttingRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMax;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.Zone;

@Entity
@Table(name = "CuttingRequest")
public class CuttingRequestInfo {
	
	 @Id
	private String sequence;
	private Long cuttingPlanId;
    private String projet;
    private String version;
    
	private String modele;
	private String definition;	
//	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
//	private LocalDateTime createdAt;
	private LocalDate planningDate; 
	private String shift;
	@ManyToOne
	private Zone zone;
	private Long cmsId;
	private LocalDate dueDate;
	private String dueShift;

	public Long getCmsId() {
		return cmsId;
	}

	public void setCmsId(Long cmsId) {
		this.cmsId = cmsId;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getDueShift() {
		return dueShift;
	}

	public void setDueShift(String dueShift) {
		this.dueShift = dueShift;
	}

	public Zone getZone() {
		return zone;
	}
	public void setZone(Zone zone) {
		this.zone = zone;
	}
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public Long getCuttingPlanId() {
		return cuttingPlanId;
	}
	public void setCuttingPlanId(Long cuttingPlanId) {
		this.cuttingPlanId = cuttingPlanId;
	}
	public String getProjet() {
		return projet;
	}
	public void setProjet(String projet) {
		this.projet = projet;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getModele() {
		return modele;
	}
	public void setModele(String modele) {
		this.modele = modele;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
//	public LocalDateTime getCreatedAt() {
//		return createdAt;
//	}
//	public void setCreatedAt(LocalDateTime createdAt) {
//		this.createdAt = createdAt;
//	}
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
	
	

}
