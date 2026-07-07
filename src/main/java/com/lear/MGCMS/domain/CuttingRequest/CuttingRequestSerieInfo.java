package com.lear.MGCMS.domain.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.CodeDefaut;
import com.lear.MGCMS.domain.CodeScrap;

@Entity
@Table(name = "CuttingRequestSerie")
public class CuttingRequestSerieInfo {
	
	@Id
	private String serie;
	
	@ManyToOne
	@JoinColumn(name = "cuttingRequest_sequence")
	private CuttingRequestInfo  cuttingRequest;
	
	private String partNumberMaterial;
	private String description;
	private String matelassageEndroit;
	private Double longueur;


	private String partNumbers;
	private Integer groupPlacement;
	private Boolean activated;
	
	private String machine;
	private Integer maxPlie;
	private Integer maxPlieDrill;
	private Integer maxDrill;
	private Integer nbrCouche;
	private String placement;
	private Double laize;
	private String config;
	private String drill;
	
	private Double perimetre;
	private Double tempsDeCoupe;

	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime createdAt; 
	private LocalDate planningDate; 
	private String shift;
	
	private Integer ind;
	@Transient
	private Integer total;
	private String zoneMatelassage;
	private String tableMatelassage;
	private String matelasseur1;
	private String matelasseur2;
	private String matelasseur3;
	private String matelasseur4;

	private LocalDateTime dateDebutMatelassage;
	private LocalDateTime dateFinMatelassage;
	private String statusMatelassage = "Waiting";
	
	private String zoneCoupe;
	private String tableCoupe;
	private String coupeur1;
	private String coupeur2;
	private String statusCoupe = "Waiting";
	private LocalDateTime dateDebutCoupe;
	private LocalDateTime dateFinCoupe;
	private Boolean autoCoupe = true;
	
	private Integer nbrPiece;
	
	private String tableQualite;
	private String controlleur;
	private String matriculePicking;
	private Double qteNonConforme;
	@ManyToOne
	private CodeDefaut codeDefaut;
	private Double qteScrap;
	@ManyToOne
	private CodeScrap codeScrap;
	
	private Double nbrPieceTotal;
	private String lieuDetection;
	@ManyToOne
	private CodeDefaut codeDefautAdditionnel;
	private String premierPaquet;
	private String milieuPaquet;
	private String dernierPaquet;
	private String verificationDrill;
	private String verificationDrill2;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime premierPaquetDate;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime milieuPaquetDate;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime dernierPaquetDate;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime verificationDrillDate;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime verificationDrill2Date;


	@OneToMany(mappedBy="cuttingRequestSerie", cascade = CascadeType.ALL)
	@NotEmpty(message = "ce champ ne peut pas être vide")
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<CuttingRequestSerieRouleauInfo> cuttingRequestSerieRouleaus = new ArrayList<CuttingRequestSerieRouleauInfo>();

	@DecimalMax(value = "9999999.999", inclusive = true)
	private Double retourMagasin;

	private String quantite;


	@Transient
	private Integer numberOfOptions;

	public Integer getNumberOfOptions() {
		return numberOfOptions;
	}

	public void setNumberOfOptions(Integer numberOfOptions) {
		this.numberOfOptions = numberOfOptions;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public LocalDateTime getPremierPaquetDate() {
		return premierPaquetDate;
	}

	public void setPremierPaquetDate(LocalDateTime premierPaquetDate) {
		this.premierPaquetDate = premierPaquetDate;
	}

	public LocalDateTime getMilieuPaquetDate() {
		return milieuPaquetDate;
	}

	public void setMilieuPaquetDate(LocalDateTime milieuPaquetDate) {
		this.milieuPaquetDate = milieuPaquetDate;
	}

	public LocalDateTime getDernierPaquetDate() {
		return dernierPaquetDate;
	}

	public void setDernierPaquetDate(LocalDateTime dernierPaquetDate) {
		this.dernierPaquetDate = dernierPaquetDate;
	}

	public LocalDateTime getVerificationDrillDate() {
		return verificationDrillDate;
	}

	public void setVerificationDrillDate(LocalDateTime verificationDrillDate) {
		this.verificationDrillDate = verificationDrillDate;
	}

	public LocalDateTime getVerificationDrill2Date() {
		return verificationDrill2Date;
	}

	public void setVerificationDrill2Date(LocalDateTime verificationDrill2Date) {
		this.verificationDrill2Date = verificationDrill2Date;
	}

	public String getVerificationDrill2() {
		return verificationDrill2;
	}

	public void setVerificationDrill2(String verificationDrill2) {
		this.verificationDrill2 = verificationDrill2;
	}

	public String getMilieuPaquet() {
		return milieuPaquet;
	}

	public void setMilieuPaquet(String milieuPaquet) {
		this.milieuPaquet = milieuPaquet;
	}

	public String getQuantite() {
		return quantite;
	}

	public void setQuantite(String quantite) {
		this.quantite = quantite;
	}

	public String getMatelasseur3() {
		return matelasseur3;
	}
	public void setMatelasseur3(String matelasseur3) {
		this.matelasseur3 = matelasseur3;
	}
	public String getMatelasseur4() {
		return matelasseur4;
	}
	public void setMatelasseur4(String matelasseur4) {
		this.matelasseur4 = matelasseur4;
	}
	public Double getPerimetre() {
		return perimetre;
	}
	public void setPerimetre(Double perimetre) {
		this.perimetre = perimetre;
	}
	public Double getTempsDeCoupe() {
		return tempsDeCoupe;
	}
	public void setTempsDeCoupe(Double tempsDeCoupe) {
		this.tempsDeCoupe = tempsDeCoupe;
	}
	public Double getNbrPieceTotal() {
		return nbrPieceTotal;
	}
	public void setNbrPieceTotal(Double nbrPieceTotal) {
		this.nbrPieceTotal = nbrPieceTotal;
	}
	public String getLieuDetection() {
		return lieuDetection;
	}
	public void setLieuDetection(String lieuDetection) {
		this.lieuDetection = lieuDetection;
	}
	public CodeDefaut getCodeDefautAdditionnel() {
		return codeDefautAdditionnel;
	}
	public void setCodeDefautAdditionnel(CodeDefaut codeDefautAdditionnel) {
		this.codeDefautAdditionnel = codeDefautAdditionnel;
	}
	public String getPremierPaquet() {
		return premierPaquet;
	}
	public void setPremierPaquet(String premierPaquet) {
		this.premierPaquet = premierPaquet;
	}
	public String getDernierPaquet() {
		return dernierPaquet;
	}
	public void setDernierPaquet(String dernierPaquet) {
		this.dernierPaquet = dernierPaquet;
	}
	public String getVerificationDrill() {
		return verificationDrill;
	}
	public void setVerificationDrill(String verificationDrill) {
		this.verificationDrill = verificationDrill;
	}
	public String getTableQualite() {
		return tableQualite;
	}
	public void setTableQualite(String tableQualite) {
		this.tableQualite = tableQualite;
	}
	public String getControlleur() {
		return controlleur;
	}
	public void setControlleur(String controlleur) {
		this.controlleur = controlleur;
	}
	public String getMatriculePicking() {
		return matriculePicking;
	}
	public void setMatriculePicking(String matriculePicking) {
		this.matriculePicking = matriculePicking;
	}
	public Double getQteNonConforme() {
		return qteNonConforme;
	}
	public void setQteNonConforme(Double qteNonConforme) {
		this.qteNonConforme = qteNonConforme;
	}
	public CodeDefaut getCodeDefaut() {
		return codeDefaut;
	}
	public void setCodeDefaut(CodeDefaut codeDefaut) {
		this.codeDefaut = codeDefaut;
	}
	public Double getQteScrap() {
		return qteScrap;
	}
	public void setQteScrap(Double qteScrap) {
		this.qteScrap = qteScrap;
	}
	public CodeScrap getCodeScrap() {
		return codeScrap;
	}
	public void setCodeScrap(CodeScrap codeScrap) {
		this.codeScrap = codeScrap;
	}
	public LocalDateTime getDateDebutMatelassage() {
		return dateDebutMatelassage;
	}
	public void setDateDebutMatelassage(LocalDateTime dateDebutMatelassage) {
		this.dateDebutMatelassage = dateDebutMatelassage;
	}
	public LocalDateTime getDateFinMatelassage() {
		return dateFinMatelassage;
	}
	public void setDateFinMatelassage(LocalDateTime dateFinMatelassage) {
		this.dateFinMatelassage = dateFinMatelassage;
	}
	public Integer getNbrPiece() {
		return nbrPiece;
	}
	public void setNbrPiece(Integer nbrPiece) {
		this.nbrPiece = nbrPiece;
	}
	public Boolean getAutoCoupe() {
		return autoCoupe;
	}
	public void setAutoCoupe(Boolean autoCoupe) {
		this.autoCoupe = autoCoupe;
	}
	public String getStatusCoupe() {
		return statusCoupe;
	}
	public void setStatusCoupe(String statusCoupe) {
		this.statusCoupe = statusCoupe;
	}
	public String getStatusMatelassage() {
		return statusMatelassage;
	}
	public void setStatusMatelassage(String statusMatelassage) {
		this.statusMatelassage = statusMatelassage;
	}
	public Double getRetourMagasin() {
		return retourMagasin;
	}
	public void setRetourMagasin(Double retourMagasin) {
		this.retourMagasin = retourMagasin;
	}
	
	public String getZoneMatelassage() {
		return zoneMatelassage;
	}
	public void setZoneMatelassage(String zoneMatelassage) {
		this.zoneMatelassage = zoneMatelassage;
	}
	public String getTableMatelassage() {
		return tableMatelassage;
	}
	public void setTableMatelassage(String tableMatelassage) {
		this.tableMatelassage = tableMatelassage;
	}
	public String getZoneCoupe() {
		return zoneCoupe;
	}
	public void setZoneCoupe(String zoneCoupe) {
		this.zoneCoupe = zoneCoupe;
	}
	public String getCoupeur1() {
		return coupeur1;
	}
	public void setCoupeur1(String coupeur1) {
		this.coupeur1 = coupeur1;
	}
	public String getCoupeur2() {
		return coupeur2;
	}
	public void setCoupeur2(String coupeur2) {
		this.coupeur2 = coupeur2;
	}
	public LocalDateTime getDateDebutCoupe() {
		return dateDebutCoupe;
	}
	public void setDateDebutCoupe(LocalDateTime dateDebutCoupe) {
		this.dateDebutCoupe = dateDebutCoupe;
	}
	public LocalDateTime getDateFinCoupe() {
		return dateFinCoupe;
	}
	public void setDateFinCoupe(LocalDateTime dateFinCoupe) {
		this.dateFinCoupe = dateFinCoupe;
	}
	public String getTableCoupe() {
		return tableCoupe;
	}
	public void setTableCoupe(String tableCoupe) {
		this.tableCoupe = tableCoupe;
	}
	public String getMatelasseur1() {
		return matelasseur1;
	}
	public void setMatelasseur1(String matelasseur1) {
		this.matelasseur1 = matelasseur1;
	}
	public String getMatelasseur2() {
		return matelasseur2;
	}
	public void setMatelasseur2(String matelasseur2) {
		this.matelasseur2 = matelasseur2;
	}
	public List<CuttingRequestSerieRouleauInfo> getCuttingRequestSerieRouleaus() {
		return cuttingRequestSerieRouleaus;
	}
	public void setCuttingRequestSerieRouleaus(List<CuttingRequestSerieRouleauInfo> cuttingRequestSerieRouleaus) {
		this.cuttingRequestSerieRouleaus = cuttingRequestSerieRouleaus;
	}
	public Integer getInd() {
		return ind;
	}
	public void setInd(Integer ind) {
		this.ind = ind;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
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
	public CuttingRequestSerieInfo() {
		super();
	}
	public String getSerie() {
		return serie;
	}
	public void setSerie(String serie) {
		this.serie = serie;
	}
	public CuttingRequestInfo getCuttingRequest() {
		return cuttingRequest;
	}
	public void setCuttingRequest(CuttingRequestInfo cuttingRequest) {
		this.cuttingRequest = cuttingRequest;
	}
	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}
	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getMatelassageEndroit() {
		return matelassageEndroit;
	}
	public void setMatelassageEndroit(String matelassageEndroit) {
		this.matelassageEndroit = matelassageEndroit;
	}
	public Double getLongueur() {
		return longueur;
	}
	public void setLongueur(Double longueur) {
		this.longueur = longueur;
	}
	public String getPartNumbers() {
		return partNumbers;
	}
	public void setPartNumbers(String partNumbers) {
		this.partNumbers = partNumbers;
	}
	public Integer getGroupPlacement() {
		return groupPlacement;
	}
	public void setGroupPlacement(Integer groupPlacement) {
		this.groupPlacement = groupPlacement;
	}
	public Boolean getActivated() {
		return activated;
	}
	public void setActivated(Boolean activated) {
		this.activated = activated;
	}
	public String getMachine() {
		return machine;
	}
	public void setMachine(String machine) {
		this.machine = machine;
	}
	public Integer getMaxPlie() {
		return maxPlie;
	}
	public void setMaxPlie(Integer maxPlie) {
		this.maxPlie = maxPlie;
	}
	public Integer getMaxPlieDrill() {
		return maxPlieDrill;
	}
	public void setMaxPlieDrill(Integer maxPlieDrill) {
		this.maxPlieDrill = maxPlieDrill;
	}
	public Integer getMaxDrill() {
		return maxDrill;
	}
	public void setMaxDrill(Integer maxDrill) {
		this.maxDrill = maxDrill;
	}
	public Integer getNbrCouche() {
		return nbrCouche;
	}
	public void setNbrCouche(Integer nbrCouche) {
		this.nbrCouche = nbrCouche;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public Double getLaize() {
		return laize;
	}
	public void setLaize(Double laize) {
		this.laize = laize;
	}
	public String getConfig() {
		return config;
	}
	public void setConfig(String config) {
		this.config = config;
	}
	public String getDrill() {
		return drill;
	}
	public void setDrill(String drill) {
		this.drill = drill;
	}
	
	

}
