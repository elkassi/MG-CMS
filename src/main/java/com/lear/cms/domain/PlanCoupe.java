package com.lear.cms.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "PlanCoupe")
public class PlanCoupe {
	
	@Id
	@Column(name = "ID_PlanCoupe") private Long idPlanCoupe;
	@Column(name = "Index_PlanCoupe") private String indexPlanCoupe;
	@Column(name = "Group_PlanCoupe") private String groupPlanCoupe;
	@Column(name = "Version_PlanCoupe") private String versionPlanCoupe;
	@Column(name = "Version2_PlanCoupe") private String version2PlanCoupe;
	@Column(name = "Definition_planCoupe") private String definitionPlanCoupe;
	@Column(name = "Date_Created_PlanCoupe") private LocalDate dateCreatedPlanCoupe;
	@Column(name = "Hour_Created_PlanCoupe") private LocalTime hourCreatedPlanCoupe;
	@Column(name = "Date_Modified_PlanCoupe") private LocalDate dateModifiedPlanCoupe;
	@Column(name = "Hour_Modified_PlanCoupe") private LocalTime hourModifiedPlanCoupe;
	@Column(name = "Date_Enabled_PlanCoupe") private LocalDate dateEnabledPlanCoupe;
	@Column(name = "Hour_Enabled_PlanCoupe") private LocalTime hourEnabledPlanCoupe;
	@Column(name = "Date_Disabled_PlanCoupe") private LocalDate dateDisabledPlanCoupe;
	@Column(name = "Hour_Disabled_PlanCoupe") private LocalTime hourDisabledPlanCoupe;
	@Column(name = "Quantity_PlanCoupe") private Integer quantityPlanCoupe;
	@Column(name = "Status_PlanCoupe") private Boolean statusPlanCoupe;
	@Column(name = "Type_PlanCoupe") private String typePlanCoupe;
	@Column(name = "Libelle_planCoupe") private String libelleplanCoupe;
	@Column(name = "Copy_From_PlanCoupe") private Integer copyFromPlanCoupe;
	@Column(name = "Comment_planCoupe") private String commentplanCoupe;
	@Column(name = "Start_Date_From_PlanCoupe") private LocalDateTime startDateFromPlanCoupe;
	@Column(name = "End_Date_To_PlanCoupe") private LocalDateTime endDateToPlanCoupe;

	/*
	,[CreatedBy_userName_PlanCoupe]
      ,[CreatedBy_HostaName_PlanCoupe]
      ,[CreatedBy_SessionW_PlanCoupe]
      ,[ModifiedBy_userName_PlanCoupe]
      ,[ModifiedBy_HostaName_PlanCoupe]
      ,[ModifiedBy_SessionW_PlanCoupe]
      ,[EnabledBy_userName_PlanCoupe]
      ,[EnabledBy_HostaName_PlanCoupe]
      ,[EnabledBy_SessionW_PlanCoupe]
      ,[DisabledBy_userName_PlanCoupe]
      ,[DisabledBy_HostaName_PlanCoupe]
      ,[DisabledBy_SessionW_PlanCoupe]
	 */

	@Column(name = "CreatedBy_userName_PlanCoupe") private String createdByUserNamePlanCoupe;
	@Column(name = "CreatedBy_HostaName_PlanCoupe") private String createdByHostaNamePlanCoupe;
	@Column(name = "CreatedBy_SessionW_PlanCoupe") private String createdBySessionWPlanCoupe;

	@Column(name = "ModifiedBy_userName_PlanCoupe") private String modifiedByUserNamePlanCoupe;
	@Column(name = "ModifiedBy_HostaName_PlanCoupe") private String modifiedByHostaNamePlanCoupe;
	@Column(name = "ModifiedBy_SessionW_PlanCoupe") private String modifiedBySessionWPlanCoupe;

	@Column(name = "EnabledBy_userName_PlanCoupe") private String enabledByUserNamePlanCoupe;
	@Column(name = "EnabledBy_HostaName_PlanCoupe") private String enabledByHostaNamePlanCoupe;
	@Column(name = "EnabledBy_SessionW_PlanCoupe") private String enabledBySessionWPlanCoupe;

	@Column(name = "DisabledBy_userName_PlanCoupe") private String disabledByUserNamePlanCoupe;
	@Column(name = "DisabledBy_HostaName_PlanCoupe") private String disabledByHostaNamePlanCoupe;
	@Column(name = "DisabledBy_SessionW_PlanCoupe") private String disabledBySessionWPlanCoupe;

	public String getDisabledByHostaNamePlanCoupe() {
		return disabledByHostaNamePlanCoupe;
	}

	public void setDisabledByHostaNamePlanCoupe(String disabledByHostaNamePlanCoupe) {
		this.disabledByHostaNamePlanCoupe = disabledByHostaNamePlanCoupe;
	}

	public String getDisabledByUserNamePlanCoupe() {
		return disabledByUserNamePlanCoupe;
	}

	public void setDisabledByUserNamePlanCoupe(String disabledByUserNamePlanCoupe) {
		this.disabledByUserNamePlanCoupe = disabledByUserNamePlanCoupe;
	}

	public String getCreatedByHostaNamePlanCoupe() {
		return createdByHostaNamePlanCoupe;
	}

	public void setCreatedByHostaNamePlanCoupe(String createdByHostaNamePlanCoupe) {
		this.createdByHostaNamePlanCoupe = createdByHostaNamePlanCoupe;
	}

	public String getCreatedBySessionWPlanCoupe() {
		return createdBySessionWPlanCoupe;
	}

	public void setCreatedBySessionWPlanCoupe(String createdBySessionWPlanCoupe) {
		this.createdBySessionWPlanCoupe = createdBySessionWPlanCoupe;
	}

	public String getModifiedByUserNamePlanCoupe() {
		return modifiedByUserNamePlanCoupe;
	}

	public void setModifiedByUserNamePlanCoupe(String modifiedByUserNamePlanCoupe) {
		this.modifiedByUserNamePlanCoupe = modifiedByUserNamePlanCoupe;
	}

	public String getModifiedByHostaNamePlanCoupe() {
		return modifiedByHostaNamePlanCoupe;
	}

	public void setModifiedByHostaNamePlanCoupe(String modifiedByHostaNamePlanCoupe) {
		this.modifiedByHostaNamePlanCoupe = modifiedByHostaNamePlanCoupe;
	}

	public String getModifiedBySessionWPlanCoupe() {
		return modifiedBySessionWPlanCoupe;
	}

	public void setModifiedBySessionWPlanCoupe(String modifiedBySessionWPlanCoupe) {
		this.modifiedBySessionWPlanCoupe = modifiedBySessionWPlanCoupe;
	}

	public String getEnabledByUserNamePlanCoupe() {
		return enabledByUserNamePlanCoupe;
	}

	public void setEnabledByUserNamePlanCoupe(String enabledByUserNamePlanCoupe) {
		this.enabledByUserNamePlanCoupe = enabledByUserNamePlanCoupe;
	}

	public String getEnabledByHostaNamePlanCoupe() {
		return enabledByHostaNamePlanCoupe;
	}

	public void setEnabledByHostaNamePlanCoupe(String enabledByHostaNamePlanCoupe) {
		this.enabledByHostaNamePlanCoupe = enabledByHostaNamePlanCoupe;
	}

	public String getEnabledBySessionWPlanCoupe() {
		return enabledBySessionWPlanCoupe;
	}

	public void setEnabledBySessionWPlanCoupe(String enabledBySessionWPlanCoupe) {
		this.enabledBySessionWPlanCoupe = enabledBySessionWPlanCoupe;
	}



	public String getDisabledBySessionWPlanCoupe() {
		return disabledBySessionWPlanCoupe;
	}

	public void setDisabledBySessionWPlanCoupe(String disabledBySessionWPlanCoupe) {
		this.disabledBySessionWPlanCoupe = disabledBySessionWPlanCoupe;
	}

	public String getCreatedByUserNamePlanCoupe() {
		return createdByUserNamePlanCoupe;
	}

	public void setCreatedByUserNamePlanCoupe(String createdByUserNamePlanCoupe) {
		this.createdByUserNamePlanCoupe = createdByUserNamePlanCoupe;
	}

	@Transient
	private List<PartNumberPlanCoupe> partNumberPlanCoupes = new ArrayList<PartNumberPlanCoupe>();

	@Transient
	private List<SpreadingCuttingPlanCoupe> spreadingCuttingPlanCoupes = new ArrayList<SpreadingCuttingPlanCoupe>();



	public List<PartNumberPlanCoupe> getPartNumberPlanCoupes() {
		return partNumberPlanCoupes;
	}

	public void setPartNumberPlanCoupes(List<PartNumberPlanCoupe> partNumberPlanCoupes) {
		this.partNumberPlanCoupes = partNumberPlanCoupes;
	}

	public List<SpreadingCuttingPlanCoupe> getSpreadingCuttingPlanCoupes() {
		return spreadingCuttingPlanCoupes;
	}

	public void setSpreadingCuttingPlanCoupes(List<SpreadingCuttingPlanCoupe> spreadingCuttingPlanCoupes) {
		this.spreadingCuttingPlanCoupes = spreadingCuttingPlanCoupes;
	}

	public Long getIdPlanCoupe() {
		return idPlanCoupe;
	}

	public void setIdPlanCoupe(Long idPlanCoupe) {
		this.idPlanCoupe = idPlanCoupe;
	}

	public String getIndexPlanCoupe() {
		return indexPlanCoupe;
	}

	public void setIndexPlanCoupe(String indexPlanCoupe) {
		this.indexPlanCoupe = indexPlanCoupe;
	}

	public String getGroupPlanCoupe() {
		return groupPlanCoupe;
	}

	public void setGroupPlanCoupe(String groupPlanCoupe) {
		this.groupPlanCoupe = groupPlanCoupe;
	}

	public String getVersionPlanCoupe() {
		return versionPlanCoupe;
	}

	public void setVersionPlanCoupe(String versionPlanCoupe) {
		this.versionPlanCoupe = versionPlanCoupe;
	}

	public String getVersion2PlanCoupe() {
		return version2PlanCoupe;
	}

	public void setVersion2PlanCoupe(String version2PlanCoupe) {
		this.version2PlanCoupe = version2PlanCoupe;
	}

	public String getDefinitionPlanCoupe() {
		return definitionPlanCoupe;
	}

	public void setDefinitionPlanCoupe(String definitionPlanCoupe) {
		this.definitionPlanCoupe = definitionPlanCoupe;
	}

	public LocalDate getDateCreatedPlanCoupe() {
		return dateCreatedPlanCoupe;
	}

	public void setDateCreatedPlanCoupe(LocalDate dateCreatedPlanCoupe) {
		this.dateCreatedPlanCoupe = dateCreatedPlanCoupe;
	}

	public LocalTime getHourCreatedPlanCoupe() {
		return hourCreatedPlanCoupe;
	}

	public void setHourCreatedPlanCoupe(LocalTime hourCreatedPlanCoupe) {
		this.hourCreatedPlanCoupe = hourCreatedPlanCoupe;
	}

	public LocalDate getDateModifiedPlanCoupe() {
		return dateModifiedPlanCoupe;
	}

	public void setDateModifiedPlanCoupe(LocalDate dateModifiedPlanCoupe) {
		this.dateModifiedPlanCoupe = dateModifiedPlanCoupe;
	}

	public LocalTime getHourModifiedPlanCoupe() {
		return hourModifiedPlanCoupe;
	}

	public void setHourModifiedPlanCoupe(LocalTime hourModifiedPlanCoupe) {
		this.hourModifiedPlanCoupe = hourModifiedPlanCoupe;
	}

	public LocalDate getDateEnabledPlanCoupe() {
		return dateEnabledPlanCoupe;
	}

	public void setDateEnabledPlanCoupe(LocalDate dateEnabledPlanCoupe) {
		this.dateEnabledPlanCoupe = dateEnabledPlanCoupe;
	}

	public LocalTime getHourEnabledPlanCoupe() {
		return hourEnabledPlanCoupe;
	}

	public void setHourEnabledPlanCoupe(LocalTime hourEnabledPlanCoupe) {
		this.hourEnabledPlanCoupe = hourEnabledPlanCoupe;
	}

	public LocalDate getDateDisabledPlanCoupe() {
		return dateDisabledPlanCoupe;
	}

	public void setDateDisabledPlanCoupe(LocalDate dateDisabledPlanCoupe) {
		this.dateDisabledPlanCoupe = dateDisabledPlanCoupe;
	}

	public LocalTime getHourDisabledPlanCoupe() {
		return hourDisabledPlanCoupe;
	}

	public void setHourDisabledPlanCoupe(LocalTime hourDisabledPlanCoupe) {
		this.hourDisabledPlanCoupe = hourDisabledPlanCoupe;
	}

	public Integer getQuantityPlanCoupe() {
		return quantityPlanCoupe;
	}

	public void setQuantityPlanCoupe(Integer quantityPlanCoupe) {
		this.quantityPlanCoupe = quantityPlanCoupe;
	}

	public Boolean getStatusPlanCoupe() {
		return statusPlanCoupe;
	}

	public void setStatusPlanCoupe(Boolean statusPlanCoupe) {
		this.statusPlanCoupe = statusPlanCoupe;
	}

	public String getTypePlanCoupe() {
		return typePlanCoupe;
	}

	public void setTypePlanCoupe(String typePlanCoupe) {
		this.typePlanCoupe = typePlanCoupe;
	}

	public String getLibelleplanCoupe() {
		return libelleplanCoupe;
	}

	public void setLibelleplanCoupe(String libelleplanCoupe) {
		this.libelleplanCoupe = libelleplanCoupe;
	}

	public Integer getCopyFromPlanCoupe() {
		return copyFromPlanCoupe;
	}

	public void setCopyFromPlanCoupe(Integer copyFromPlanCoupe) {
		this.copyFromPlanCoupe = copyFromPlanCoupe;
	}

	public String getCommentplanCoupe() {
		return commentplanCoupe;
	}

	public void setCommentplanCoupe(String commentplanCoupe) {
		this.commentplanCoupe = commentplanCoupe;
	}

	public LocalDateTime getStartDateFromPlanCoupe() {
		return startDateFromPlanCoupe;
	}

	public void setStartDateFromPlanCoupe(LocalDateTime startDateFromPlanCoupe) {
		this.startDateFromPlanCoupe = startDateFromPlanCoupe;
	}

	public LocalDateTime getEndDateToPlanCoupe() {
		return endDateToPlanCoupe;
	}

	public void setEndDateToPlanCoupe(LocalDateTime endDateToPlanCoupe) {
		this.endDateToPlanCoupe = endDateToPlanCoupe;
	}

	
	
}
