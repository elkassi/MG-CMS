package com.lear.MGCMS.utils;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StatusQualite {

    /*
    SELECT TOP 1000
  cuttingRequest_sequence,
  serie,
  partNumberMaterial,
  placement,
  tableMatelassage,
  tableCoupe,
  tableQualite,
  nbrPiece,
  nbrCouche,
  nbrPieceTotal,
  qteNonConforme,
  crs.codeDefaut_code,
  cd.description AS codeDefaut_description, -- Add the description column
  lieuDetection,
  qteScrap,
  crs.codeScrap_code,
  cs.description AS codeScrap_description, -- Add the description column,
  dateDebutCoupe
FROM [dbo].[CuttingRequestSerie] crs
LEFT JOIN [dbo].[CodeDefaut] cd ON crs.codeDefaut_code = cd.code
LEFT JOIN [dbo].[CodeScrap] cs ON crs.codeScrap_code = cs.code
WHERE statusCoupe = 'Complete'
ORDER BY dateDebutCoupe DESC;
     */

    private String cuttingRequestSequence;
    private String serie;
    private String partNumberMaterial;
    private String placement;
    private String tableMatelassage;
    private String tableCoupe;
    private String tableQualite;
    private int nbrPiece;
    private int nbrCouche;
    private int nbrPieceTotal;
    private int qteNonConforme;
    private String codeDefautCode;
    private String codeDefautDescription;
    private String lieuDetection;
    private int qteScrap;
    private String codeScrapCode;
    private String codeScrapDescription;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateDebutCoupe;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateValidationQualite;

    /*
      premierPaquet,
  milieuPaquet,
  dernierPaquet,
  verificationDrill,

     */

    private String premierPaquet;
    private String milieuPaquet;
    private String dernierPaquet;
    private String verificationDrill;
    private String verificationDrill2;

    public String getVerificationDrill2() {
        return verificationDrill2;
    }

    public void setVerificationDrill2(String verificationDrill2) {
        this.verificationDrill2 = verificationDrill2;
    }

    public String getPremierPaquet() {
        return premierPaquet;
    }

    public void setPremierPaquet(String premierPaquet) {
        this.premierPaquet = premierPaquet;
    }

    public String getMilieuPaquet() {
        return milieuPaquet;
    }

    public void setMilieuPaquet(String milieuPaquet) {
        this.milieuPaquet = milieuPaquet;
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

    public void setDateDebutCoupe(LocalDateTime dateDebutCoupe) {
        this.dateDebutCoupe = dateDebutCoupe;
    }

    public LocalDateTime getDateValidationQualite() {
        return dateValidationQualite;
    }

    public void setDateValidationQualite(LocalDateTime dateValidationQualite) {
        this.dateValidationQualite = dateValidationQualite;
    }

    public String getCuttingRequestSequence() {
        return cuttingRequestSequence;
    }

    public void setCuttingRequestSequence(String cuttingRequestSequence) {
        this.cuttingRequestSequence = cuttingRequestSequence;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getPartNumberMaterial() {
        return partNumberMaterial;
    }

    public void setPartNumberMaterial(String partNumberMaterial) {
        this.partNumberMaterial = partNumberMaterial;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public String getTableMatelassage() {
        return tableMatelassage;
    }

    public void setTableMatelassage(String tableMatelassage) {
        this.tableMatelassage = tableMatelassage;
    }

    public String getTableCoupe() {
        return tableCoupe;
    }

    public void setTableCoupe(String tableCoupe) {
        this.tableCoupe = tableCoupe;
    }

    public String getTableQualite() {
        return tableQualite;
    }

    public void setTableQualite(String tableQualite) {
        this.tableQualite = tableQualite;
    }

    public int getNbrPiece() {
        return nbrPiece;
    }

    public void setNbrPiece(int nbrPiece) {
        this.nbrPiece = nbrPiece;
    }

    public int getNbrCouche() {
        return nbrCouche;
    }

    public void setNbrCouche(int nbrCouche) {
        this.nbrCouche = nbrCouche;
    }

    public int getNbrPieceTotal() {
        return nbrPieceTotal;
    }

    public void setNbrPieceTotal(int nbrPieceTotal) {
        this.nbrPieceTotal = nbrPieceTotal;
    }

    public int getQteNonConforme() {
        return qteNonConforme;
    }

    public void setQteNonConforme(int qteNonConforme) {
        this.qteNonConforme = qteNonConforme;
    }

    public String getCodeDefautCode() {
        return codeDefautCode;
    }

    public void setCodeDefautCode(String codeDefautCode) {
        this.codeDefautCode = codeDefautCode;
    }

    public String getCodeDefautDescription() {
        return codeDefautDescription;
    }

    public void setCodeDefautDescription(String codeDefautDescription) {
        this.codeDefautDescription = codeDefautDescription;
    }

    public String getLieuDetection() {
        return lieuDetection;
    }

    public void setLieuDetection(String lieuDetection) {
        this.lieuDetection = lieuDetection;
    }

    public int getQteScrap() {
        return qteScrap;
    }

    public void setQteScrap(int qteScrap) {
        this.qteScrap = qteScrap;
    }

    public String getCodeScrapCode() {
        return codeScrapCode;
    }

    public void setCodeScrapCode(String codeScrapCode) {
        this.codeScrapCode = codeScrapCode;
    }

    public String getCodeScrapDescription() {
        return codeScrapDescription;
    }

    public void setCodeScrapDescription(String codeScrapDescription) {
        this.codeScrapDescription = codeScrapDescription;
    }

    public LocalDateTime getDateDebutCoupe() {
        return dateDebutCoupe;
    }
}
