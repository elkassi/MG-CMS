package com.lear.MGCMS.domain.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;

@Entity
public class CuttingRequest {
	
    @Id
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

	@OneToMany(mappedBy="cuttingRequest", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<CuttingRequestPartNumber> cuttingRequestPartNumbers = new ArrayList<CuttingRequestPartNumber>();

	@OneToMany(mappedBy="cuttingRequest", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<CuttingRequestSerie> cuttingRequestSeries = new ArrayList<CuttingRequestSerie>();
	
	@OneToMany(mappedBy="cuttingRequest", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	List<CuttingRequestBox> cuttingRequestBoxs = new ArrayList<CuttingRequestBox>();

	private Long cmsId;
	private LocalDate dueDate;
	private String dueShift;
	@ManyToOne
	private Zone zone;

	// ------------------------------------------------------------------
	// Dispatcher output (Phase 2 schema, activated in Phase 4+).
	// Kept as simple scalar fields — no FK to Zone — so they can be null
	// pre-dispatch and dropped cheaply if the feature is rolled back.
	// ------------------------------------------------------------------

	/**
	 * Zone name produced by SequenceDispatcherService.publish(...).
	 * Null until the dispatcher has run for this sequence.
	 */
	@javax.persistence.Column(name = "dispatched_zone", length = 64)
	private String dispatchedZone;

	/**
	 * Chef-de-zone lifecycle for {@link #dispatchedZone}. Values:
	 * PENDING / ACCEPTED / REJECTED. See V2_02 migration.
	 */
	@javax.persistence.Column(name = "zone_acceptance_status", length = 16)
	private String zoneAcceptanceStatus;

	/**
	 * When {@code true}, a chef has pinned this serie to a specific machine
	 * queue position and the ordonnancement engine must not reshuffle it.
	 */
	@javax.persistence.Column(name = "pinned_by_chef", nullable = false)
	private boolean pinnedByChef = false;

	/** Timestamp of the last dispatch (publish / force / rebalance). */
	@javax.persistence.Column(name = "dispatchedAt")
	private java.time.LocalDateTime dispatchedAt;

	/** Matricule of the user who performed the last dispatch. */
	@javax.persistence.Column(name = "dispatchedBy", length = 50)
	private String dispatchedBy;

	/** Timestamp when the chef accepted the sequence. */
	@javax.persistence.Column(name = "zoneAcceptedAt")
	private java.time.LocalDateTime zoneAcceptedAt;

	/** Matricule of the chef who accepted/rejected the sequence. */
	@javax.persistence.Column(name = "zoneAcceptedBy", length = 50)
	private String zoneAcceptedBy;

	/** Free-text reason when the chef rejects a sequence. */
	@javax.persistence.Column(name = "zoneRejectionReason", length = 512)
	private String zoneRejectionReason;

	/**
	 * Lifecycle of the sequence as a whole — independent from the per-serie
	 * statusCoupe / statusMatelassage. See
	 * {@link com.lear.MGCMS.domain.CuttingRequest.SequenceStatus} for the
	 * vocabulary: {@code IMPORTED}, {@code RELEASED}, {@code STARTED},
	 * {@code COMPLETED}, {@code MATERIAL_MISSING}, {@code INCOMPLETE}. The
	 * engine / workbench / dispatcher load the in-production set
	 * ({@code RELEASED, STARTED, MATERIAL_MISSING}); {@code IMPORTED} is the
	 * pre-release picklist-candidate state and {@code COMPLETED}/{@code INCOMPLETE}
	 * are terminal.
	 */
	private String sequenceStatus;

	/**
	 * Zone the sequence is fixed to when logistics releases it (status
	 * {@code RELEASED}). Persisted as the camelCase column {@code releaseZone}
	 * (V16_01). Takes precedence over {@link #dispatchedZone} in the live-charge
	 * effective-zone resolution; null until release.
	 */
	private String releaseZone;

	@Transient
	private Map<String, Object> splitInfo;
	@Transient
	private List<Map<String, Object>> splitInfos = new ArrayList<Map<String, Object>>();
//	private int priority = 1;
//	// Waiting, Released, In progress, Complete, Incomplete
//	private String status = "Waiting";
//	private String statusMatelassage = "Waiting";

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


	public User getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}


	public Zone getZone() {
		return zone;
	}


	public void setZone(Zone zone) {
		this.zone = zone;
	}

	public Map<String, Object> getSplitInfo() {
		return splitInfo;
	}

	public void setSplitInfo(Map<String, Object> splitInfo) {
		this.splitInfo = splitInfo;
	}

	public List<Map<String, Object>> getSplitInfos() {
		return splitInfos;
	}

	public void setSplitInfos(List<Map<String, Object>> splitInfos) {
		this.splitInfos = splitInfos;
	}


	public CuttingRequest() {
		super();
	}
	

	public List<CuttingRequestBox> getCuttingRequestBoxs() {
		return cuttingRequestBoxs;
	}


	public void setCuttingRequestBoxs(List<CuttingRequestBox> cuttingRequestBoxs) {
		this.cuttingRequestBoxs = cuttingRequestBoxs;
	}


	public Long getCuttingPlanId() {
		return cuttingPlanId;
	}

	public void setCuttingPlanId(Long cuttingPlanId) {
		this.cuttingPlanId = cuttingPlanId;
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

	public List<CuttingRequestSerie> getCuttingRequestSeries() {
		return cuttingRequestSeries;
	}

	public void setCuttingRequestSeries(List<CuttingRequestSerie> cuttingRequestSeries) {
		this.cuttingRequestSeries = cuttingRequestSeries;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
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

	public List<CuttingRequestPartNumber> getCuttingRequestPartNumbers() {
		return cuttingRequestPartNumbers;
	}

	public void setCuttingRequestPartNumbers(List<CuttingRequestPartNumber> cuttingRequestPartNumbers) {
		this.cuttingRequestPartNumbers = cuttingRequestPartNumbers;
	}

	public String getDispatchedZone() {
		return dispatchedZone;
	}

	public void setDispatchedZone(String dispatchedZone) {
		this.dispatchedZone = dispatchedZone;
	}

	public String getZoneAcceptanceStatus() {
		return zoneAcceptanceStatus;
	}

	public void setZoneAcceptanceStatus(String zoneAcceptanceStatus) {
		this.zoneAcceptanceStatus = zoneAcceptanceStatus;
	}

	public boolean isPinnedByChef() {
		return pinnedByChef;
	}

	public void setPinnedByChef(boolean pinnedByChef) {
		this.pinnedByChef = pinnedByChef;
	}

	public java.time.LocalDateTime getDispatchedAt() { return dispatchedAt; }
	public void setDispatchedAt(java.time.LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }

	public String getDispatchedBy() { return dispatchedBy; }
	public void setDispatchedBy(String dispatchedBy) { this.dispatchedBy = dispatchedBy; }

	public java.time.LocalDateTime getZoneAcceptedAt() { return zoneAcceptedAt; }
	public void setZoneAcceptedAt(java.time.LocalDateTime zoneAcceptedAt) { this.zoneAcceptedAt = zoneAcceptedAt; }

	public String getZoneAcceptedBy() { return zoneAcceptedBy; }
	public void setZoneAcceptedBy(String zoneAcceptedBy) { this.zoneAcceptedBy = zoneAcceptedBy; }

	public String getZoneRejectionReason() { return zoneRejectionReason; }
	public void setZoneRejectionReason(String zoneRejectionReason) { this.zoneRejectionReason = zoneRejectionReason; }

	public String getSequenceStatus() { return sequenceStatus; }
	public void setSequenceStatus(String sequenceStatus) { this.sequenceStatus = sequenceStatus; }

	public String getReleaseZone() { return releaseZone; }
	public void setReleaseZone(String releaseZone) { this.releaseZone = releaseZone; }

}
