package com.lear.MGCMS.domain;

import javax.persistence.*;

@Entity
@Table(name = "BoxTypeConfig")
public class BoxTypeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String boxType;

    private Double emptyBoxWeight;

    public BoxTypeConfig() {}

    public BoxTypeConfig(String boxType, Double emptyBoxWeight) {
        this.boxType = boxType;
        this.emptyBoxWeight = emptyBoxWeight;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBoxType() {
        return boxType;
    }

    public void setBoxType(String boxType) {
        this.boxType = boxType;
    }

    public Double getEmptyBoxWeight() {
        return emptyBoxWeight;
    }

    public void setEmptyBoxWeight(Double emptyBoxWeight) {
        this.emptyBoxWeight = emptyBoxWeight;
    }
}
