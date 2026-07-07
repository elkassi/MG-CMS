package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "Order_Schedule")
public class OrderSchedule {
	
	@Id
	@Column(name= "ID_Demande")
	private Long idDemande;
	@Column(name= "Site_Demande") 
    private String siteDemande;
	@Column(name= "Chaine_Demande") 
    private String chaineDemande;
	@Column(name= "Project_Demande") 
    private String projectDemande;
	@Column(name= "PartNumber_Demande") 
    private String partNumberDemande;
	@Column(name= "Description_PN_Demande") 
    private String descriptionPNDemande;
	@Column(name= "Leather_Kit_Demande") 
    private String leatherKitDemande;
	@Column(name= "Textil_Kit_Demande") 
    private String textilKitDemande;
	@Column(name= "Quantite_Demande") 
    private Integer quantiteDemande;
	@Column(name= "Date_Demande") 
    private LocalDate dateDemande;
	@Column(name= "Shift_Demande") 
    private String shiftDemande;
	@Column(name= "Matricule_Demandeur_Demande") 
    private String matriculeDemandeurDemande;
	@Column(name= "Nom_Demandeur_Demande") 
    private String nomDemandeurDemande;
	@Column(name= "Status_Demande") 
    private String statusDemande;
	@Column(name= "Remarque_Demande") 
    private String remarqueDemande;
	@Column(name= "Status_PS_Demande") 
    private String statusPSDemande;
	@Column(name= "Status_Reception_Sewing_Demande") 
    private String statusReceptionSewingDemande;
	@Column(name= "Creation_Date_Demande") 
    private LocalDate creationDateDemande;
	@Column(name= "Creation_Hour_Demande") 
    private LocalTime creationHourDemande;
	@Column(name= "Modification_Date_Demande") 
    private LocalDate modificationDateDemande;
	@Column(name= "Modification_Hour_Demande") 
    private LocalTime modificationHourDemande;
	@Column(name= "UserName_Demande") 
    private String userNameDemande;
	@Column(name= "HostName_Demande") 
    private String hostNameDemande;
	@Column(name= "SessionW_Demande") 
    private String sessionWDemande;
	@Column(name= "WorkCenter_Demande") 
    private String workCenterDemande;
	@Column(name= "Asprova_ID") 
    private String asprovaID;
	@Column(name= "Production_Start_Time") 
    private LocalDateTime productionStartTime;
	@Column(name= "Production_End_Time") 
    private LocalDateTime productionEndTime;
	@Column(name= "Marker_ID") 
    private String markerID;
	@Column(name= "Status_Asp") 
    private String statusAsp;
	@Column(name= "Marker_Group_ID_D") 
    private String markerGroupIDD;
	@Column(name= "Import_Date_D") 
    private LocalDateTime importDateD;
	public Long getIdDemande() {
		return idDemande;
	}
	public void setIdDemande(Long idDemande) {
		this.idDemande = idDemande;
	}
	public String getSiteDemande() {
		return siteDemande;
	}
	public void setSiteDemande(String siteDemande) {
		this.siteDemande = siteDemande;
	}
	public String getChaineDemande() {
		return chaineDemande;
	}
	public void setChaineDemande(String chaineDemande) {
		this.chaineDemande = chaineDemande;
	}
	public String getProjectDemande() {
		return projectDemande;
	}
	public void setProjectDemande(String projectDemande) {
		this.projectDemande = projectDemande;
	}
	public String getPartNumberDemande() {
		return partNumberDemande;
	}
	public void setPartNumberDemande(String partNumberDemande) {
		this.partNumberDemande = partNumberDemande;
	}
	public String getDescriptionPNDemande() {
		return descriptionPNDemande;
	}
	public void setDescriptionPNDemande(String descriptionPNDemande) {
		this.descriptionPNDemande = descriptionPNDemande;
	}
	public String getLeatherKitDemande() {
		return leatherKitDemande;
	}
	public void setLeatherKitDemande(String leatherKitDemande) {
		this.leatherKitDemande = leatherKitDemande;
	}
	public String getTextilKitDemande() {
		return textilKitDemande;
	}
	public void setTextilKitDemande(String textilKitDemande) {
		this.textilKitDemande = textilKitDemande;
	}
	public Integer getQuantiteDemande() {
		return quantiteDemande;
	}
	public void setQuantiteDemande(Integer quantiteDemande) {
		this.quantiteDemande = quantiteDemande;
	}
	public LocalDate getDateDemande() {
		return dateDemande;
	}
	public void setDateDemande(LocalDate dateDemande) {
		this.dateDemande = dateDemande;
	}
	public String getShiftDemande() {
		return shiftDemande;
	}
	public void setShiftDemande(String shiftDemande) {
		this.shiftDemande = shiftDemande;
	}
	public String getMatriculeDemandeurDemande() {
		return matriculeDemandeurDemande;
	}
	public void setMatriculeDemandeurDemande(String matriculeDemandeurDemande) {
		this.matriculeDemandeurDemande = matriculeDemandeurDemande;
	}
	public String getNomDemandeurDemande() {
		return nomDemandeurDemande;
	}
	public void setNomDemandeurDemande(String nomDemandeurDemande) {
		this.nomDemandeurDemande = nomDemandeurDemande;
	}
	public String getStatusDemande() {
		return statusDemande;
	}
	public void setStatusDemande(String statusDemande) {
		this.statusDemande = statusDemande;
	}
	public String getRemarqueDemande() {
		return remarqueDemande;
	}
	public void setRemarqueDemande(String remarqueDemande) {
		this.remarqueDemande = remarqueDemande;
	}
	public String getStatusPSDemande() {
		return statusPSDemande;
	}
	public void setStatusPSDemande(String statusPSDemande) {
		this.statusPSDemande = statusPSDemande;
	}
	public String getStatusReceptionSewingDemande() {
		return statusReceptionSewingDemande;
	}
	public void setStatusReceptionSewingDemande(String statusReceptionSewingDemande) {
		this.statusReceptionSewingDemande = statusReceptionSewingDemande;
	}
	public LocalDate getCreationDateDemande() {
		return creationDateDemande;
	}
	public void setCreationDateDemande(LocalDate creationDateDemande) {
		this.creationDateDemande = creationDateDemande;
	}
	public LocalTime getCreationHourDemande() {
		return creationHourDemande;
	}
	public void setCreationHourDemande(LocalTime creationHourDemande) {
		this.creationHourDemande = creationHourDemande;
	}
	public LocalDate getModificationDateDemande() {
		return modificationDateDemande;
	}
	public void setModificationDateDemande(LocalDate modificationDateDemande) {
		this.modificationDateDemande = modificationDateDemande;
	}
	public LocalTime getModificationHourDemande() {
		return modificationHourDemande;
	}
	public void setModificationHourDemande(LocalTime modificationHourDemande) {
		this.modificationHourDemande = modificationHourDemande;
	}
	public String getUserNameDemande() {
		return userNameDemande;
	}
	public void setUserNameDemande(String userNameDemande) {
		this.userNameDemande = userNameDemande;
	}
	public String getHostNameDemande() {
		return hostNameDemande;
	}
	public void setHostNameDemande(String hostNameDemande) {
		this.hostNameDemande = hostNameDemande;
	}
	public String getSessionWDemande() {
		return sessionWDemande;
	}
	public void setSessionWDemande(String sessionWDemande) {
		this.sessionWDemande = sessionWDemande;
	}
	public String getWorkCenterDemande() {
		return workCenterDemande;
	}
	public void setWorkCenterDemande(String workCenterDemande) {
		this.workCenterDemande = workCenterDemande;
	}
	public String getAsprovaID() {
		return asprovaID;
	}
	public void setAsprovaID(String asprovaID) {
		this.asprovaID = asprovaID;
	}
	public LocalDateTime getProductionStartTime() {
		return productionStartTime;
	}
	public void setProductionStartTime(LocalDateTime productionStartTime) {
		this.productionStartTime = productionStartTime;
	}
	public LocalDateTime getProductionEndTime() {
		return productionEndTime;
	}
	public void setProductionEndTime(LocalDateTime productionEndTime) {
		this.productionEndTime = productionEndTime;
	}
	public String getMarkerID() {
		return markerID;
	}
	public void setMarkerID(String markerID) {
		this.markerID = markerID;
	}
	public String getStatusAsp() {
		return statusAsp;
	}
	public void setStatusAsp(String statusAsp) {
		this.statusAsp = statusAsp;
	}
	public String getMarkerGroupIDD() {
		return markerGroupIDD;
	}
	public void setMarkerGroupIDD(String markerGroupIDD) {
		this.markerGroupIDD = markerGroupIDD;
	}
	public LocalDateTime getImportDateD() {
		return importDateD;
	}
	public void setImportDateD(LocalDateTime importDateD) {
		this.importDateD = importDateD;
	}
	
	
	
	

}
