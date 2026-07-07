package com.lear.MGCMS.payload;

public class SerieReport {

    private String serie;
    private String placement;
    private String partNumberMaterial;
    private Double longueur;
    private Double laize;
    private Integer nbrCouche;
    private Double perimetre;
    private Double longueurTotal;
    private Double indicateur;

    public SerieReport() {
    }

    public SerieReport(String serie, String placement, String partNumberMaterial, Double longueur, Double laize, Integer nbrCouche, Double perimetre, Double longueurTotal, Double indicateur) {
        this.serie = serie;
        this.placement = placement;
        this.partNumberMaterial = partNumberMaterial;
        this.longueur = longueur;
        this.laize = laize;
        this.nbrCouche = nbrCouche;
        this.perimetre = perimetre;
        this.longueurTotal = longueurTotal;
        this.indicateur = indicateur;
    }

    // Getters and Setters


    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public String getPartNumberMaterial() {
        return partNumberMaterial;
    }

    public void setPartNumberMaterial(String partNumberMaterial) {
        this.partNumberMaterial = partNumberMaterial;
    }

    public Double getLongueur() {
        return longueur;
    }

    public void setLongueur(Double longueur) {
        this.longueur = longueur;
    }

    public Double getLaize() {
        return laize;
    }

    public void setLaize(Double laize) {
        this.laize = laize;
    }

    public Integer getNbrCouche() {
        return nbrCouche;
    }

    public void setNbrCouche(Integer nbrCouche) {
        this.nbrCouche = nbrCouche;
    }

    public Double getPerimetre() {
        return perimetre;
    }

    public void setPerimetre(Double perimetre) {
        this.perimetre = perimetre;
    }

    public Double getLongueurTotal() {
        return longueurTotal;
    }

    public void setLongueurTotal(Double longueurTotal) {
        this.longueurTotal = longueurTotal;
    }

    public Double getIndicateur() {
        return indicateur;
    }

    public void setIndicateur(Double indicateur) {
        this.indicateur = indicateur;
    }
}
