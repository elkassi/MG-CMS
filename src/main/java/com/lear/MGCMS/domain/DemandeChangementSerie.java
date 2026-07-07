package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.tomcat.jni.Local;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class DemandeChangementSerie {

    @Id
    private String id;
    private Integer ind;
    private String serie;
    private String sequence;
    private String projet;
    private String partNumberMaterial;
    private String partNumbers;
    private String placement;
    private Double laizeOld;
    private Double laizeContracuelle;

    private String typeDemande;
    private Double laize;
    @ManyToOne
    private MachineType machine;
    private String config;
    private String autreChangement;
    private String description;
    private String creePar;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;
    private String statut;

    private String departementValidation;
    private String reponseDepartement;
    private String confirmeParDepartement;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateConfirmationDepartement;

    private String cause;

    private String newPlacement;
    private String reponse;
    private String confirmePar;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateConfirmation;
    private Boolean active = true;

    public Double getLaizeOld() {
        return laizeOld;
    }

    public void setLaizeOld(Double laizeOld) {
        this.laizeOld = laizeOld;
    }

    public Double getLaizeContracuelle() {
        return laizeContracuelle;
    }

    public void setLaizeContracuelle(Double laizeContracuelle) {
        this.laizeContracuelle = laizeContracuelle;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getProjet() {
        return projet;
    }

    public void setProjet(String projet) {
        this.projet = projet;
    }

    public String getTypeDemande() {
        return typeDemande;
    }

    public void setTypeDemande(String typeDemande) {
        this.typeDemande = typeDemande;
    }

    public String getNewPlacement() {
        return newPlacement;
    }

    public void setNewPlacement(String newPlacement) {
        this.newPlacement = newPlacement;
    }


    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public String getAutreChangement() {
        return autreChangement;
    }

    public void setAutreChangement(String autreChangement) {
        this.autreChangement = autreChangement;
    }

    public String getPartNumbers() {
        return partNumbers;
    }

    public void setPartNumbers(String partNumbers) {
        this.partNumbers = partNumbers;
    }

    public String getPartNumberMaterial() {
        return partNumberMaterial;
    }

    public void setPartNumberMaterial(String partNumberMaterial) {
        this.partNumberMaterial = partNumberMaterial;
    }

    public String getDepartementValidation() {
        return departementValidation;
    }

    public void setDepartementValidation(String departementValidation) {
        this.departementValidation = departementValidation;
    }

    public String getReponseDepartement() {
        return reponseDepartement;
    }

    public void setReponseDepartement(String reponseDepartement) {
        this.reponseDepartement = reponseDepartement;
    }

    public String getConfirmeParDepartement() {
        return confirmeParDepartement;
    }

    public void setConfirmeParDepartement(String confirmeParDepartement) {
        this.confirmeParDepartement = confirmeParDepartement;
    }

    public LocalDateTime getDateConfirmationDepartement() {
        return dateConfirmationDepartement;
    }

    public void setDateConfirmationDepartement(LocalDateTime dateConfirmationDepartement) {
        this.dateConfirmationDepartement = dateConfirmationDepartement;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getInd() {
        return ind;
    }

    public void setInd(Integer ind) {
        this.ind = ind;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public Double getLaize() {
        return laize;
    }

    public void setLaize(Double laize) {
        this.laize = laize;
    }

    public MachineType getMachine() {
        return machine;
    }

    public void setMachine(MachineType machine) {
        this.machine = machine;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreePar() {
        return creePar;
    }

    public void setCreePar(String creePar) {
        this.creePar = creePar;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getConfirmePar() {
        return confirmePar;
    }

    public void setConfirmePar(String confirmePar) {
        this.confirmePar = confirmePar;
    }

    public LocalDateTime getDateConfirmation() {
        return dateConfirmation;
    }

    public void setDateConfirmation(LocalDateTime dateConfirmation) {
        this.dateConfirmation = dateConfirmation;
    }
}
