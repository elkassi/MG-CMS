package com.lear.MGCMS.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Consumable")
public class Consumable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String machine;
    private LocalDateTime date;
    private String name;
    private String type;
    private Integer count;
    private Double value;
    private String unit;
    private String mobileSet;
    private Boolean isBroken;
    private LocalDateTime firstDate;
    private Integer mountingCount;
    private String valueName1;
    private Double value1;
    private String unit1;
    private String valueName2;
    private Integer value2;
    private String unit2;
    private String valueName3;
    private Double value3;
    private String unit3;
    private String valueName4;
    private Double value4;
    private String unit4;

    @Override
    public String toString() {
        return "Consumable{" +
                "id=" + id +
                ", machine='" + machine + '\'' +
                ", date=" + date +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", count=" + count +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", mobileSet='" + mobileSet + '\'' +
                ", isBroken=" + isBroken +
                ", firstDate=" + firstDate +
                ", mountingCount=" + mountingCount +
                ", valueName1='" + valueName1 + '\'' +
                ", value1=" + value1 +
                ", unit1='" + unit1 + '\'' +
                ", valueName2='" + valueName2 + '\'' +
                ", value2=" + value2 +
                ", unit2='" + unit2 + '\'' +
                ", valueName3='" + valueName3 + '\'' +
                ", value3=" + value3 +
                ", unit3='" + unit3 + '\'' +
                ", valueName4='" + valueName4 + '\'' +
                ", value4=" + value4 +
                ", unit4='" + unit4 + '\'' +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getMobileSet() {
        return mobileSet;
    }

    public void setMobileSet(String mobileSet) {
        this.mobileSet = mobileSet;
    }

    public Boolean getIsBroken() {
        return isBroken;
    }

    public void setIsBroken(Boolean broken) {
        isBroken = broken;
    }

    public LocalDateTime getFirstDate() {
        return firstDate;
    }

    public void setFirstDate(LocalDateTime firstDate) {
        this.firstDate = firstDate;
    }

    public Integer getMountingCount() {
        return mountingCount;
    }

    public void setMountingCount(Integer mountingCount) {
        this.mountingCount = mountingCount;
    }

    public String getValueName1() {
        return valueName1;
    }

    public void setValueName1(String valueName1) {
        this.valueName1 = valueName1;
    }

    public Double getValue1() {
        return value1;
    }

    public void setValue1(Double value1) {
        this.value1 = value1;
    }

    public String getUnit1() {
        return unit1;
    }

    public void setUnit1(String unit1) {
        this.unit1 = unit1;
    }

    public String getValueName2() {
        return valueName2;
    }

    public void setValueName2(String valueName2) {
        this.valueName2 = valueName2;
    }

    public Integer getValue2() {
        return value2;
    }

    public void setValue2(Integer value2) {
        this.value2 = value2;
    }

    public String getUnit2() {
        return unit2;
    }

    public void setUnit2(String unit2) {
        this.unit2 = unit2;
    }

    public String getValueName3() {
        return valueName3;
    }

    public void setValueName3(String valueName3) {
        this.valueName3 = valueName3;
    }

    public Double getValue3() {
        return value3;
    }

    public void setValue3(Double value3) {
        this.value3 = value3;
    }

    public String getUnit3() {
        return unit3;
    }

    public void setUnit3(String unit3) {
        this.unit3 = unit3;
    }

    public String getValueName4() {
        return valueName4;
    }

    public void setValueName4(String valueName4) {
        this.valueName4 = valueName4;
    }

    public Double getValue4() {
        return value4;
    }

    public void setValue4(Double value4) {
        this.value4 = value4;
    }

    public String getUnit4() {
        return unit4;
    }

    public void setUnit4(String unit4) {
        this.unit4 = unit4;
    }
}
