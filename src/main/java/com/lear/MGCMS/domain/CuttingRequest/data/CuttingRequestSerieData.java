package com.lear.MGCMS.domain.CuttingRequest.data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMax;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.CodeDefaut;
import com.lear.MGCMS.domain.CodeScrap;
import com.lear.MGCMS.utils.CustomSerieGenerator;

@Entity
@Table(name = "CuttingRequestSerie")
public class CuttingRequestSerieData implements Cloneable {

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}


	@Id
//	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custom_serie")
//	@GenericGenerator(name = "custom_serie", strategy = "com.lear.MGCMS.utils.CustomSerieGenerator", parameters = {
//			@Parameter(name = CustomSerieGenerator.INCREMENT_PARAM, value = "1"), })
	private String serie;

	@Column(name = "cuttingRequest_sequence")
	private String sequence;

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
	private String zoneMatelassage;
	private String tableMatelassage;
	private String matelasseur1;
	private String matelasseur2;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateDebutMatelassage;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateFinMatelassage;
	private String statusMatelassage = "Waiting";
	private String zoneCoupe;
	private String tableCoupe;
	private String coupeur1;
	private String coupeur2;
	private String statusCoupe = "Waiting";
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateDebutCoupe;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
	@DecimalMax(value = "9999999.999", inclusive = true)
	private Double retourMagasin;

	private String quantite;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime premierPaquetDate;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime milieuPaquetDate;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime dernierPaquetDate;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime verificationDrillDate;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")private LocalDateTime verificationDrill2Date;



	@Override
	public String toString() {
		String str = "CuttingRequestSerieData{" +
				"serie='" + serie + '\'' +
				", sequence='" + sequence + '\'' +
				", partNumberMaterial='" + partNumberMaterial + '\'' +
				", description='" + description + '\'' +
				", matelassageEndroit='" + matelassageEndroit + '\'' +
				", longueur=" + longueur +
				", partNumbers='" + partNumbers + '\'' +
				", groupPlacement=" + groupPlacement +
				", activated=" + activated +
				", machine='" + machine + '\'' +
				", maxPlie=" + maxPlie +
				", maxPlieDrill=" + maxPlieDrill +
				", maxDrill=" + maxDrill +
				", nbrCouche=" + nbrCouche +
				", placement='" + placement + '\'' +
				", laize=" + laize +
				", config='" + config + '\'' +
				", drill='" + drill + '\'' +
				", createdAt=" + createdAt +
				", planningDate=" + planningDate +
				", shift='" + shift + '\'' +
				", ind=" + ind +
				", zoneMatelassage='" + zoneMatelassage + '\'' +
				", tableMatelassage='" + tableMatelassage + '\'' +
				", matelasseur1='" + matelasseur1 + '\'' +
				", matelasseur2='" + matelasseur2 + '\'' +
				", dateDebutMatelassage=" + dateDebutMatelassage +
				", dateFinMatelassage=" + dateFinMatelassage +
				", statusMatelassage='" + statusMatelassage + '\'' +
				", zoneCoupe='" + zoneCoupe + '\'' +
				", tableCoupe='" + tableCoupe + '\'' +
				", coupeur1='" + coupeur1 + '\'' +
				", coupeur2='" + coupeur2 + '\'' +
				", statusCoupe='" + statusCoupe + '\'' +
				", dateDebutCoupe=" + dateDebutCoupe +
				", dateFinCoupe=" + dateFinCoupe +
				", autoCoupe=" + autoCoupe +
				", nbrPiece=" + nbrPiece +
				", tableQualite='" + tableQualite + '\'' +
				", controlleur='" + controlleur + '\'' +
				", matriculePicking='" + matriculePicking + '\'' +
				", qteNonConforme=" + qteNonConforme +
				", qteScrap=" + qteScrap +
				", nbrPieceTotal=" + nbrPieceTotal +
				", lieuDetection='" + lieuDetection + '\'' +
				", premierPaquet='" + premierPaquet + '\'' +
				", milieuPaquet='" + milieuPaquet + '\'' +
				", dernierPaquet='" + dernierPaquet + '\'' +
				", verificationDrill='" + verificationDrill + '\'' +
				", verificationDrill2='" + verificationDrill2 + '\'' +
				", retourMagasin=" + retourMagasin +
				", quantite='" + quantite + '\'' +
				", premierPaquetDate=" + premierPaquetDate +
				", milieuPaquetDate=" + milieuPaquetDate +
				", dernierPaquetDate=" + dernierPaquetDate +
				", verificationDrillDate=" + verificationDrillDate +
				", verificationDrill2Date=" + verificationDrill2Date ;
		// add the cases of @ManyToOne like codeDefaut, codeScrap, codeDefautAdditionnel
		if (codeDefaut != null) {
			str += ", codeDefaut=" + codeDefaut.toString();
		}
		if (codeScrap != null) {
			str += ", codeScrap=" + codeScrap.toString();
		}
		if (codeDefautAdditionnel != null) {
			str += ", codeDefautAdditionnel=" + codeDefautAdditionnel.toString();
		}
		str += '}';
		return str;
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

	public String getSerie() {
		return serie;
	}

	public void setSerie(String serie) {
		this.serie = serie;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
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

	public Integer getInd() {
		return ind;
	}

	public void setInd(Integer ind) {
		this.ind = ind;
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

	public String getStatusMatelassage() {
		return statusMatelassage;
	}

	public void setStatusMatelassage(String statusMatelassage) {
		this.statusMatelassage = statusMatelassage;
	}

	public String getZoneCoupe() {
		return zoneCoupe;
	}

	public void setZoneCoupe(String zoneCoupe) {
		this.zoneCoupe = zoneCoupe;
	}

	public String getTableCoupe() {
		return tableCoupe;
	}

	public void setTableCoupe(String tableCoupe) {
		this.tableCoupe = tableCoupe;
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

	public String getStatusCoupe() {
		return statusCoupe;
	}

	public void setStatusCoupe(String statusCoupe) {
		this.statusCoupe = statusCoupe;
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

	public Boolean getAutoCoupe() {
		return autoCoupe;
	}

	public void setAutoCoupe(Boolean autoCoupe) {
		this.autoCoupe = autoCoupe;
	}

	public Integer getNbrPiece() {
		return nbrPiece;
	}

	public void setNbrPiece(Integer nbrPiece) {
		this.nbrPiece = nbrPiece;
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

	public Double getRetourMagasin() {
		return retourMagasin;
	}

	public void setRetourMagasin(Double retourMagasin) {
		this.retourMagasin = retourMagasin;
	}

	
	
}
