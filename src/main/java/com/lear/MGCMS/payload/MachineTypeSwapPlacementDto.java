package com.lear.MGCMS.payload;

public class MachineTypeSwapPlacementDto {

    private String placement;
    private Long cuttingPlan;
    private String projet;
    private String partNumberMaterial;
    private String partNumbers;
    private Integer groupPlacement;
    private Boolean activated;
    private String machine;
    private String category;
    private Double laize;
    private Double longueur;

    public MachineTypeSwapPlacementDto(
            String placement,
            Long cuttingPlan,
            String projet,
            String partNumberMaterial,
            String partNumbers,
            Integer groupPlacement,
            Boolean activated,
            String machine,
            String category,
            Double laize,
            Double longueur) {
        this.placement = placement;
        this.cuttingPlan = cuttingPlan;
        this.projet = projet;
        this.partNumberMaterial = partNumberMaterial;
        this.partNumbers = partNumbers;
        this.groupPlacement = groupPlacement;
        this.activated = activated;
        this.machine = machine;
        this.category = category;
        this.laize = laize;
        this.longueur = longueur;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public Long getCuttingPlan() {
        return cuttingPlan;
    }

    public void setCuttingPlan(Long cuttingPlan) {
        this.cuttingPlan = cuttingPlan;
    }

    public String getProjet() {
        return projet;
    }

    public void setProjet(String projet) {
        this.projet = projet;
    }

    public String getPartNumberMaterial() {
        return partNumberMaterial;
    }

    public void setPartNumberMaterial(String partNumberMaterial) {
        this.partNumberMaterial = partNumberMaterial;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getLaize() {
        return laize;
    }

    public void setLaize(Double laize) {
        this.laize = laize;
    }

    public Double getLongueur() {
        return longueur;
    }

    public void setLongueur(Double longueur) {
        this.longueur = longueur;
    }
}
