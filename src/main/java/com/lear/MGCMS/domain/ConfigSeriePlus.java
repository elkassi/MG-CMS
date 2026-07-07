package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ConfigSeriePlus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pattern;
    private String partNumberMaterial;
    private String description;
    private String matelassageEndroit;
    private Double longueur;
    private String machine;
    private Integer nbrCouche;
    private Integer kits;
    private Integer maxPlie;
    private String placement;
    private Double laize;
    private String config;
    private String drill;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
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

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public Integer getNbrCouche() {
        return nbrCouche;
    }

    public void setNbrCouche(Integer nbrCouche) {
        this.nbrCouche = nbrCouche;
    }

    public Integer getKits() {
        return kits;
    }

    public void setKits(Integer kits) {
        this.kits = kits;
    }

    public Integer getMaxPlie() {
        return maxPlie;
    }

    public void setMaxPlie(Integer maxPlie) {
        this.maxPlie = maxPlie;
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
