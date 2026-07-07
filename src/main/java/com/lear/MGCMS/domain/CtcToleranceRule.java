package com.lear.MGCMS.domain;

import javax.persistence.*;

@Entity
@Table(name = "CtcToleranceRule")
public class CtcToleranceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projet; // Project code (can be null for default rules)
    
    private String type; // Type like "supplier kit leather", "fabric", "CNC", etc. (can be null for default rules)
    
    private String laminateFilter; // "all", "laminate_only", "non_laminate_only" (pattern ends with L or not)
    
    private String applyOn; // "width", "height", "max" (max between width and height)
    
    private Double heightMin; // Min height for this rule (0 = start from 0)
    
    private Double heightMax; // Max height for this rule (null = infinity)
    
    private Double toleranceMin1; // Min tolerance value (e.g., -1.0)
    
    private Double toleranceMax1; // Max tolerance value (e.g., 1.0)
    
    private Double toleranceMin2; // Min tolerance for second axis
    
    private Double toleranceMax2; // Max tolerance for second axis
    
    private Integer toleranceDrill; // Drill tolerance
    
    private Boolean active = true;
    
    private Integer priority; // Higher priority rules are applied first

    // Constructors
    public CtcToleranceRule() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjet() {
        return projet;
    }

    public void setProjet(String projet) {
        this.projet = projet;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String getLaminateFilter() {
        return laminateFilter;
    }

    public void setLaminateFilter(String laminateFilter) {
        this.laminateFilter = laminateFilter;
    }

    public String getApplyOn() {
        return applyOn;
    }

    public void setApplyOn(String applyOn) {
        this.applyOn = applyOn;
    }

    public Double getHeightMin() {
        return heightMin;
    }

    public void setHeightMin(Double heightMin) {
        this.heightMin = heightMin;
    }

    public Double getHeightMax() {
        return heightMax;
    }

    public void setHeightMax(Double heightMax) {
        this.heightMax = heightMax;
    }

    public Double getToleranceMin1() {
        return toleranceMin1;
    }

    public void setToleranceMin1(Double toleranceMin1) {
        this.toleranceMin1 = toleranceMin1;
    }

    public Double getToleranceMax1() {
        return toleranceMax1;
    }

    public void setToleranceMax1(Double toleranceMax1) {
        this.toleranceMax1 = toleranceMax1;
    }

    public Double getToleranceMin2() {
        return toleranceMin2;
    }

    public void setToleranceMin2(Double toleranceMin2) {
        this.toleranceMin2 = toleranceMin2;
    }

    public Double getToleranceMax2() {
        return toleranceMax2;
    }

    public void setToleranceMax2(Double toleranceMax2) {
        this.toleranceMax2 = toleranceMax2;
    }

    public Integer getToleranceDrill() {
        return toleranceDrill;
    }

    public void setToleranceDrill(Integer toleranceDrill) {
        this.toleranceDrill = toleranceDrill;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
