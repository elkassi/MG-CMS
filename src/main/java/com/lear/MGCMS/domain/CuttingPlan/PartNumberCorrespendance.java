package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class PartNumberCorrespendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partNumber;
    private String partNumberCorrespondance;

    private String pattern;
    private String patternCorrespondance;

    private String placement;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPatternCorrespondance() {
        return patternCorrespondance;
    }

    public void setPatternCorrespondance(String patternCorrespondance) {
        this.patternCorrespondance = patternCorrespondance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getPartNumberCorrespondance() {
        return partNumberCorrespondance;
    }

    public void setPartNumberCorrespondance(String partNumberCorrespondance) {
        this.partNumberCorrespondance = partNumberCorrespondance;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }
}
