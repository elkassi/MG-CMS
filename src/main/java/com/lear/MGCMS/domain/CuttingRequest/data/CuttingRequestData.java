package com.lear.MGCMS.domain.CuttingRequest.data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;

@Entity
@Table(name = "CuttingRequest")
public class CuttingRequestData {

	@Id
    @GeneratedValue(generator = "custom-sequence")
    @GenericGenerator(name = "custom-sequence", strategy = "com.lear.MGCMS.utils.CustomSequenceGenerator")
	private String sequence;
	private Long cuttingPlanId;
    private String projet;
    private String version;
    
	private String modele;
	private String definition;		
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;
	
	@ManyToOne
	private User createdBy;
	private LocalDate planningDate; 
	private String shift;
	
	@ManyToOne
	private Zone zone;
	private Long cmsId;
	private LocalDate dueDate;
	private String dueShift;
	// Lifecycle vocabulary — see com.lear.MGCMS.domain.CuttingRequest.SequenceStatus:
	// IMPORTED, RELEASED, STARTED, COMPLETED, MATERIAL_MISSING, INCOMPLETE
	private String sequenceStatus = "IMPORTED";
	// Zone fixed at logistics release (status RELEASED); camelCase column releaseZone (V16_01)
	private String releaseZone;
	// Who wrote releaseZone — see ReleaseZoneSource (LOGISTICS/CHEF/AUTO, V17_02)
	private String releaseZoneSource;

	// Transient field for all-zones query (not persisted)
	@Transient
	private String zoneName;

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

	public Long getCmsId() {
		return cmsId;
	}

	public void setCmsId(Long cmsId) {
		this.cmsId = cmsId;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
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

	public Zone getZone() {
		return zone;
	}

	public void setZone(Zone zone) {
		this.zone = zone;
	}
	
	public String getZoneName() {
		return zoneName;
	}

	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	public String getSequenceStatus() {
		return sequenceStatus;
	}

	public void setSequenceStatus(String sequenceStatus) {
		this.sequenceStatus = sequenceStatus;
	}

	public String getReleaseZone() {
		return releaseZone;
	}

	public void setReleaseZone(String releaseZone) {
		this.releaseZone = releaseZone;
	}

	public String getReleaseZoneSource() {
		return releaseZoneSource;
	}

	public void setReleaseZoneSource(String releaseZoneSource) {
		this.releaseZoneSource = releaseZoneSource;
	}

}
