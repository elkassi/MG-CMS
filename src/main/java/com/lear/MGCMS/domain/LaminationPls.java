package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class LaminationPls {

    @Id
    private String reftissu;
    private String plsId;

    public String getReftissu() {
        return reftissu;
    }

    public void setReftissu(String reftissu) {
        this.reftissu = reftissu;
    }

    public String getPlsId() {
        return plsId;
    }

    public void setPlsId(String plsId) {
        this.plsId = plsId;
    }
}
