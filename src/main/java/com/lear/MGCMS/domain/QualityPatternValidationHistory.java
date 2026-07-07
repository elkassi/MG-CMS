package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QualityPatternValidationHistory")
public class QualityPatternValidationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serie;

    @Column(nullable = false)
    private String machine;

    @Column(nullable = false)
    private String placement;

    private String partNumberMaterial;

    private String pattern;

    @Column(nullable = false)
    private String validatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime validationDate;

    private String qualityCode;

    private String comments;

    // Constructors
    public QualityPatternValidationHistory() {
        this.validationDate = LocalDateTime.now();
    }

    public QualityPatternValidationHistory(String serie, String machine, String placement, String partNumberMaterial, 
                                  String pattern, String validatedBy, String qualityCode, String comments) {
        this.serie = serie;
        this.machine = machine;
        this.placement = placement;
        this.partNumberMaterial = partNumberMaterial;
        this.pattern = pattern;
        this.validatedBy = validatedBy;
        this.qualityCode = qualityCode;
        this.comments = comments;
        this.validationDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
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

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(String validatedBy) {
        this.validatedBy = validatedBy;
    }

    public LocalDateTime getValidationDate() {
        return validationDate;
    }

    public void setValidationDate(LocalDateTime validationDate) {
        this.validationDate = validationDate;
    }

    public String getQualityCode() {
        return qualityCode;
    }

    public void setQualityCode(String qualityCode) {
        this.qualityCode = qualityCode;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "QualityPatternValidationHistory{" +
                "id=" + id +
                ", serie='" + serie + '\'' +
                ", machine='" + machine + '\'' +
                ", placement='" + placement + '\'' +
                ", partNumberMaterial='" + partNumberMaterial + '\'' +
                ", pattern='" + pattern + '\'' +
                ", validatedBy='" + validatedBy + '\'' +
                ", validationDate=" + validationDate +
                ", qualityCode='" + qualityCode + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}