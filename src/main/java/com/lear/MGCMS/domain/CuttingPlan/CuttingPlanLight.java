package com.lear.MGCMS.domain.CuttingPlan;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.User;

@Entity
@Table(name="CuttingPlan")
public class CuttingPlanLight {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String description;
	private String projet;
	private String version;
	private String version2;
	private String definition;
	
	
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime createdAt;
	@ManyToOne
	private User createdBy;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime updatedAt;
	@ManyToOne
	private User updatedBy;
	private Boolean enabled = true;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime enabledAt;
	@ManyToOne
	private User enabledBy;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime disabledAt;
	@ManyToOne
	private User disabledBy;
	private String type;
	private Long copyId;
	private Long cmsId;

	@Lob
	private String commentaire;
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime startDate;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime endDate;
	
	@OneToMany(mappedBy="cuttingPlan", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<CuttingPlanPartNumber> cuttingPlanPartNumbers = new ArrayList<CuttingPlanPartNumber>();
	
	@OneToMany(mappedBy="cuttingPlan", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<CuttingPlanMaterial> cuttingPlanMaterials = new ArrayList<CuttingPlanMaterial>();

	// cad verification
	@Lob private String verification1;
	@Lob private String verification2;
	@Lob private String verification3;
	@Lob private String verification4;
	@Lob private String verification5;
	@Lob private String verification6;
//	@Lob private String verification7;
//	@Lob private String verification8;
private Boolean consommation = false;
	private String alertMessages;
	private Boolean foam;

	public Boolean getFoam() {
		return foam;
	}

	public void setFoam(Boolean foam) {
		this.foam = foam;
	}

	public String getAlertMessages() {
		return alertMessages;
	}

	public void setAlertMessages(String alertMessages) {
		this.alertMessages = alertMessages;
	}

	public Boolean getConsommation() {
		return consommation;
	}

	public void setConsommation(Boolean consommation) {
		this.consommation = consommation;
	}

	public String getVerification1() {
		return verification1;
	}

	public void setVerification1(String verification1) {
		this.verification1 = verification1;
	}

	public String getVerification2() {
		return verification2;
	}

	public void setVerification2(String verification2) {
		this.verification2 = verification2;
	}

	public String getVerification3() {
		return verification3;
	}

	public void setVerification3(String verification3) {
		this.verification3 = verification3;
	}

	public String getVerification4() {
		return verification4;
	}

	public void setVerification4(String verification4) {
		this.verification4 = verification4;
	}

	public String getVerification5() {
		return verification5;
	}

	public void setVerification5(String verification5) {
		this.verification5 = verification5;
	}

	public String getVerification6() {
		return verification6;
	}

	public void setVerification6(String verification6) {
		this.verification6 = verification6;
	}


	public Long getCmsId() {
		return cmsId;
	}

	public void setCmsId(Long cmsId) {
		this.cmsId = cmsId;
	}

	public List<CuttingPlanPartNumber> getCuttingPlanPartNumbers() {
		return cuttingPlanPartNumbers;
	}

	public void setCuttingPlanPartNumbers(List<CuttingPlanPartNumber> cuttingPlanPartNumbers) {
		this.cuttingPlanPartNumbers = cuttingPlanPartNumbers;
	}

	public List<CuttingPlanMaterial> getCuttingPlanMaterials() {
		return cuttingPlanMaterials;
	}

	public void setCuttingPlanMaterials(List<CuttingPlanMaterial> cuttingPlanMaterials) {
		this.cuttingPlanMaterials = cuttingPlanMaterials;
	}

	public Long getCopyId() {
		return copyId;
	}

	public void setCopyId(Long copyId) {
		this.copyId = copyId;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getVersion2() {
		return version2;
	}

	public void setVersion2(String version2) {
		this.version2 = version2;
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

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public User getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public LocalDateTime getEnabledAt() {
		return enabledAt;
	}

	public void setEnabledAt(LocalDateTime enabledAt) {
		this.enabledAt = enabledAt;
	}

	public User getEnabledBy() {
		return enabledBy;
	}

	public void setEnabledBy(User enabledBy) {
		this.enabledBy = enabledBy;
	}

	public LocalDateTime getDisabledAt() {
		return disabledAt;
	}

	public void setDisabledAt(LocalDateTime disabledAt) {
		this.disabledAt = disabledAt;
	}

	public User getDisabledBy() {
		return disabledBy;
	}

	public void setDisabledBy(User disabledBy) {
		this.disabledBy = disabledBy;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDateTime startDate) {
		this.startDate = startDate;
	}

	public LocalDateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDateTime endDate) {
		this.endDate = endDate;
	}

}
