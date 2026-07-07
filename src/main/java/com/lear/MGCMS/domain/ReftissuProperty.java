package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(ReftissuPropertyId.class)
public class ReftissuProperty {

    @Id
    private String reftissu;
    @Id
    private String property;
    private String value;
    private String value2;

    public ReftissuProperty(String reftissu, String property, String value) {
        super();
        this.reftissu = reftissu;
        this.property = property;
        this.value = value;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    public ReftissuProperty() {
        super();
    }

    public String getReftissu() {
        return reftissu;
    }

    public void setReftissu(String reftissu) {
        this.reftissu = reftissu;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
