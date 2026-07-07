package com.lear.MGCMS.domain;

import javax.persistence.*;

@Entity
@Table(name = "PartNumberMaterialConfig")
public class PartNumberMaterialConfigData {

    @Id
    private String partNumberMaterial;
    private String description;
    private Integer vitesse;
    private String rotation; // 90 180 FIX
    private Double plaque;
    private Double tauxScrap;
    private String matelassageEndroit;
    @Lob
    private String commentaire;
    private Double margeLaizeMin;
    private Double margeLaizeMax;
    private Boolean validated0BF;
    private Boolean validatedIP6;
    private String buffer1IP6;
    private String buffer2IP6;
    private Boolean fipDev;
    @Column(name = "weight_unit")
    private Double weightUnit; // kg per m² of this material

    public Double getWeightUnit() {
        return weightUnit;
    }

    public String getBuffer1IP6() {
        return buffer1IP6;
    }

    public void setBuffer1IP6(String buffer1IP6) {
        this.buffer1IP6 = buffer1IP6;
    }

    public String getBuffer2IP6() {
        return buffer2IP6;
    }

    public void setBuffer2IP6(String buffer2IP6) {
        this.buffer2IP6 = buffer2IP6;
    }

    public Boolean getValidatedIP6() {
        return validatedIP6;
    }

    public void setValidatedIP6(Boolean validatedIP6) {
        this.validatedIP6 = validatedIP6;
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

    public Integer getVitesse() {
        return vitesse;
    }

    public void setVitesse(Integer vitesse) {
        this.vitesse = vitesse;
    }

    public String getRotation() {
        return rotation;
    }

    public void setRotation(String rotation) {
        this.rotation = rotation;
    }

    public Double getPlaque() {
        return plaque;
    }

    public void setPlaque(Double plaque) {
        this.plaque = plaque;
    }

    public Double getTauxScrap() {
        return tauxScrap;
    }

    public void setTauxScrap(Double tauxScrap) {
        this.tauxScrap = tauxScrap;
    }

    public String getMatelassageEndroit() {
        return matelassageEndroit;
    }

    public void setMatelassageEndroit(String matelassageEndroit) {
        this.matelassageEndroit = matelassageEndroit;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public Double getMargeLaizeMin() {
        return margeLaizeMin;
    }

    public void setMargeLaizeMin(Double margeLaizeMin) {
        this.margeLaizeMin = margeLaizeMin;
    }

    public Double getMargeLaizeMax() {
        return margeLaizeMax;
    }

    public void setMargeLaizeMax(Double margeLaizeMax) {
        this.margeLaizeMax = margeLaizeMax;
    }

    public Boolean getValidated0BF() {
        return validated0BF;
    }

    public void setValidated0BF(Boolean validated0BF) {
        this.validated0BF = validated0BF;
    }

    public Boolean getFipDev() {
        return fipDev;
    }

    public void setFipDev(Boolean fipDev) {
        this.fipDev = fipDev;
    }
}
