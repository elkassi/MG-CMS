package com.lear.MGCMS.domain;

import javax.persistence.*;

@Entity
@Table(name = "PartNumberWeight")
public class PartNumberWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partnumber;

    private Double weightUnit;

    public PartNumberWeight() {}

    public PartNumberWeight(String partnumber, Double weightUnit) {
        this.partnumber = partnumber;
        this.weightUnit = weightUnit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartnumber() {
        return partnumber;
    }

    public void setPartnumber(String partnumber) {
        this.partnumber = partnumber;
    }

    public Double getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(Double weightUnit) {
        this.weightUnit = weightUnit;
    }
}
