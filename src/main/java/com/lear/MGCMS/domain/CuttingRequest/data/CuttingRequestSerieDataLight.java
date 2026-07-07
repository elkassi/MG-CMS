package com.lear.MGCMS.domain.CuttingRequest.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.CodeDefaut;
import com.lear.MGCMS.domain.CodeScrap;

import javax.persistence.*;
import javax.validation.constraints.DecimalMax;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CuttingRequestSerie")
public class CuttingRequestSerieDataLight {

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
    private String quantite;

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

    public String getQuantite() {
        return quantite;
    }

    public void setQuantite(String quantite) {
        this.quantite = quantite;
    }
}
