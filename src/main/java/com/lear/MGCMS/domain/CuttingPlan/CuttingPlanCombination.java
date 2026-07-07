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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.User;

@Entity
public class CuttingPlanCombination {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String description;
	@NotBlank(message = "ce champ ne peut pas être null")
	private String projet;
	private String version;
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
	
	private Long copyId;
	@Lob
	private String commentaire;
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime startDate;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime endDate;

	@OneToMany(mappedBy="cuttingPlanCombination", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<CuttingPlanCombinationPartNumber> cuttingPlanCombinationPartNumbers = new ArrayList<CuttingPlanCombinationPartNumber>();

//	private String multiplications;
	
	
	
//	public String getMultiplications() {
//		return multiplications;
//	}
//
//	public void setMultiplications(String multiplications) {
//		this.multiplications = multiplications;
//	}

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

	public Long getCopyId() {
		return copyId;
	}

	public void setCopyId(Long copyId) {
		this.copyId = copyId;
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

	public List<CuttingPlanCombinationPartNumber> getCuttingPlanCombinationPartNumbers() {
		return cuttingPlanCombinationPartNumbers;
	}

	public void setCuttingPlanCombinationPartNumbers(
			List<CuttingPlanCombinationPartNumber> cuttingPlanCombinationPartNumbers) {
		this.cuttingPlanCombinationPartNumbers = cuttingPlanCombinationPartNumbers;
	}

	

}
