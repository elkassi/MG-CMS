package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
public class QualityNotice {

    @Id
    private String numeroQn;
    private Integer ind;

    private String coordinateur;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @NotEmpty(message = "WO est obligatoire")
    private String wo;
    private String sequence;
    private String partnumber;
    private String projet;
    private String site;
    private String numEmp;
    private Integer quantite;
    @NotEmpty(message = "Référence tissu est obligatoire")
    private String reftissu;
    private String reftissuDescription;
    private String nomFournisseur;

    private String description;
    @NotEmpty(message = "WO est obligatoire")
    private String typeDefaut;
    @ManyToOne
    @NotNull(message = "Code defaut est obligatoire")
    private CodeDefaut codeDefaut;
    private String extraEmails;
    private String image1;
    private String image2;

    private String idRouleau;
    private String lotFrs;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime dateCoupe;
    private Double metrageEcarte;

    @ManyToOne
    private CodeDefaut correctDefaut;
    private Integer qteRecu;
    private Integer qteRecuCoiffe;
    private Double qteRecuMetrage;

    private String machine;
    private String traiterPar;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTraitement;
    private String reponse;
    private String decision;
    private String securisation;
    private String remarque;
    private String extraEmailsReponse;
    private String fichier;
    private Boolean qrqc = false;

    private Boolean active = true;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime sendNotificationDate;
    private String notificationBy;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime coupeValidationDate;
    private String coupeValidationBY;
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime superviseurConfirmationDate;
    private String superviseurConfirmationBy;
    public LocalDateTime getSuperviseurConfirmationDate() {
        return superviseurConfirmationDate;
    }

    public Double getQteRecuMetrage() {
        return qteRecuMetrage;
    }

    public void setQteRecuMetrage(Double qteRecuMetrage) {
        this.qteRecuMetrage = qteRecuMetrage;
    }

    public void setSuperviseurConfirmationDate(LocalDateTime superviseurConfirmationDate) {
        this.superviseurConfirmationDate = superviseurConfirmationDate;
    }

    public Boolean getQrqc() {
        return qrqc;
    }

    public void setQrqc(Boolean qrqc) {
        this.qrqc = qrqc;
    }

    public String getSuperviseurConfirmationBy() {
        return superviseurConfirmationBy;
    }

    public void setSuperviseurConfirmationBy(String superviseurConfirmationBy) {
        this.superviseurConfirmationBy = superviseurConfirmationBy;
    }

    public Integer getQteRecuCoiffe() {
        return qteRecuCoiffe;
    }

    public void setQteRecuCoiffe(Integer qteRecuCoiffe) {
        this.qteRecuCoiffe = qteRecuCoiffe;
    }

    public String getNotificationBy() {
        return notificationBy;
    }

    public void setNotificationBy(String notificationBy) {
        this.notificationBy = notificationBy;
    }

    public LocalDateTime getCoupeValidationDate() {
        return coupeValidationDate;
    }

    public void setCoupeValidationDate(LocalDateTime coupeValidationDate) {
        this.coupeValidationDate = coupeValidationDate;
    }

    public String getCoupeValidationBY() {
        return coupeValidationBY;
    }

    public void setCoupeValidationBY(String coupeValidationBY) {
        this.coupeValidationBY = coupeValidationBY;
    }

    public LocalDateTime getSendNotificationDate() {
        return sendNotificationDate;
    }

    public void setSendNotificationDate(LocalDateTime sendNotificationDate) {
        this.sendNotificationDate = sendNotificationDate;
    }

    public Integer getQteRecu() {
        return qteRecu;
    }

    public void setQteRecu(Integer qteRecu) {
        this.qteRecu = qteRecu;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Double getMetrageEcarte() {
        return metrageEcarte;
    }

    public void setMetrageEcarte(Double metrageEcarte) {
        this.metrageEcarte = metrageEcarte;
    }

    public String getReftissuDescription() {
        return reftissuDescription;
    }

    public void setReftissuDescription(String reftissuDescription) {
        this.reftissuDescription = reftissuDescription;
    }

    public String getIdRouleau() {
        return idRouleau;
    }

    public void setIdRouleau(String idRouleau) {
        this.idRouleau = idRouleau;
    }

    public String getLotFrs() {
        return lotFrs;
    }

    public void setLotFrs(String lotFrs) {
        this.lotFrs = lotFrs;
    }

    public LocalDateTime getDateCoupe() {
        return dateCoupe;
    }

    public void setDateCoupe(LocalDateTime dateCoupe) {
        this.dateCoupe = dateCoupe;
    }

    public String getNomFournisseur() {
        return nomFournisseur;
    }

    public void setNomFournisseur(String nomFournisseur) {
        this.nomFournisseur = nomFournisseur;
    }

    public LocalDateTime getDateTraitement() {
        return dateTraitement;
    }

    public void setDateTraitement(LocalDateTime dateTraitement) {
        this.dateTraitement = dateTraitement;
    }

    public String getFichier() {
        return fichier;
    }

    public void setFichier(String fichier) {
        this.fichier = fichier;
    }

    public String getImage1() {
        return image1;
    }

    public void setImage1(String image1) {
        this.image1 = image1;
    }

    public String getImage2() {
        return image2;
    }

    public void setImage2(String image2) {
        this.image2 = image2;
    }

    public String getExtraEmails() {
        return extraEmails;
    }

    public void setExtraEmails(String extraEmails) {
        this.extraEmails = extraEmails;
    }

    public String getExtraEmailsReponse() {
        return extraEmailsReponse;
    }

    public void setExtraEmailsReponse(String extraEmailsReponse) {
        this.extraEmailsReponse = extraEmailsReponse;
    }

    public String getNumeroQn() {
        return numeroQn;
    }

    public void setNumeroQn(String numeroQn) {
        this.numeroQn = numeroQn;
    }

    public Integer getInd() {
        return ind;
    }

    public void setInd(Integer ind) {
        this.ind = ind;
    }

    public String getCoordinateur() {
        return coordinateur;
    }

    public void setCoordinateur(String coordinateur) {
        this.coordinateur = coordinateur;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getWo() {
        return wo;
    }

    public void setWo(String wo) {
        this.wo = wo;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getPartnumber() {
        return partnumber;
    }

    public void setPartnumber(String partnumber) {
        this.partnumber = partnumber;
    }

    public String getProjet() {
        return projet;
    }

    public void setProjet(String projet) {
        this.projet = projet;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getNumEmp() {
        return numEmp;
    }

    public void setNumEmp(String numEmp) {
        this.numEmp = numEmp;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public String getReftissu() {
        return reftissu;
    }

    public void setReftissu(String reftissu) {
        this.reftissu = reftissu;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTypeDefaut() {
        return typeDefaut;
    }

    public void setTypeDefaut(String typeDefaut) {
        this.typeDefaut = typeDefaut;
    }

    public CodeDefaut getCodeDefaut() {
        return codeDefaut;
    }

    public void setCodeDefaut(CodeDefaut codeDefaut) {
        this.codeDefaut = codeDefaut;
    }

    public CodeDefaut getCorrectDefaut() {
        return correctDefaut;
    }

    public void setCorrectDefaut(CodeDefaut correctDefaut) {
        this.correctDefaut = correctDefaut;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getTraiterPar() {
        return traiterPar;
    }

    public void setTraiterPar(String traiterPar) {
        this.traiterPar = traiterPar;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getSecurisation() {
        return securisation;
    }

    public void setSecurisation(String securisation) {
        this.securisation = securisation;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }
}
