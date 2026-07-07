package com.lear.MGCMS.domain;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class ProductionTable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "nom est obligatoire")
	private String nom;
	@ManyToOne
	private MachineType machineType;
	private String pcMatelassage;
	private String pcCoupe;
	private String ipImprimante;
	@ManyToOne
	private Zone zone;
	private String bibliobus;
	private Double vibrationTime;
	private Double vacuumTime;
	private String serialNumber;
	private String type;
	private LocalDate installationDate;
	private Boolean autorisationAirbag = false;
	private Boolean forPls = false;

	private Integer calibrageDrill1;
	private Double calibrageDrill1Value;
	private Integer calibrageDrill2;
	private Double calibrageDrill2Value;

	private String versionCMS;
	private String versionCMSCoupe;

	private Double tableLength = 14.0; // Physical table length in metres

	// Machine expected efficiency (%). PlanDeCharge scales cutting time by
	// 1/(efficience/100); replaces the legacy hardcoded Gerber x2.
	private Double efficience = 90.0;

	public Integer getCalibrageDrill1() {
		return calibrageDrill1;
	}

	public void setCalibrageDrill1(Integer calibrageDrill1) {
		this.calibrageDrill1 = calibrageDrill1;
	}

	public Double getCalibrageDrill1Value() {
		return calibrageDrill1Value;
	}

	public void setCalibrageDrill1Value(Double calibrageDrill1Value) {
		this.calibrageDrill1Value = calibrageDrill1Value;
	}

	public Integer getCalibrageDrill2() {
		return calibrageDrill2;
	}

	public void setCalibrageDrill2(Integer calibrageDrill2) {
		this.calibrageDrill2 = calibrageDrill2;
	}

	public Double getCalibrageDrill2Value() {
		return calibrageDrill2Value;
	}

	public void setCalibrageDrill2Value(Double calibrageDrill2Value) {
		this.calibrageDrill2Value = calibrageDrill2Value;
	}

	public String getVersionCMS() {
		return versionCMS;
	}

	public void setVersionCMS(String versionCMS) {
		this.versionCMS = versionCMS;
	}

	public String getVersionCMSCoupe() {
		return versionCMSCoupe;
	}

	public void setVersionCMSCoupe(String versionCMSCoupe) {
		this.versionCMSCoupe = versionCMSCoupe;
	}

	public Double getTableLength() {
		return tableLength;
	}

	public void setTableLength(Double tableLength) {
		this.tableLength = tableLength;
	}

	public Double getEfficience() {
		return efficience;
	}

	public void setEfficience(Double efficience) {
		this.efficience = efficience;
	}

	public Boolean getForPls() {
		return forPls;
	}

	public void setForPls(Boolean forPls) {
		this.forPls = forPls;
	}

	public Boolean getAutorisationAirbag() {
		return autorisationAirbag;
	}

	public void setAutorisationAirbag(Boolean autorisationAirbag) {
		this.autorisationAirbag = autorisationAirbag;
	}

	public Double getVibrationTime() {
		return vibrationTime;
	}

	public void setVibrationTime(Double vibrationTime) {
		this.vibrationTime = vibrationTime;
	}

	public Double getVacuumTime() {
		return vacuumTime;
	}

	public void setVacuumTime(Double vacuumTime) {
		this.vacuumTime = vacuumTime;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public LocalDate getInstallationDate() {
		return installationDate;
	}

	public void setInstallationDate(LocalDate installationDate) {
		this.installationDate = installationDate;
	}

	public String getBibliobus() {
		return bibliobus;
	}
	public void setBibliobus(String bibliobus) {
		this.bibliobus = bibliobus;
	}
	public Zone getZone() {
		return zone;
	}
	public void setZone(Zone zone) {
		this.zone = zone;
	}
	public String getIpImprimante() {
		return ipImprimante;
	}
	public void setIpImprimante(String ipImprimante) {
		this.ipImprimante = ipImprimante;
	}
	public MachineType getMachineType() {
		return machineType;
	}
	public void setMachineType(MachineType machineType) {
		this.machineType = machineType;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getNom() {
		return nom;
	}
	public void setNom(String nom) {
		this.nom = nom;
	}
	public String getPcMatelassage() {
		return pcMatelassage;
	}
	public void setPcMatelassage(String pcMatelassage) {
		this.pcMatelassage = pcMatelassage;
	}
	public String getPcCoupe() {
		return pcCoupe;
	}
	public void setPcCoupe(String pcCoupe) {
		this.pcCoupe = pcCoupe;
	}
	public ProductionTable() {
		super();
	}
	
	
	
	
}
