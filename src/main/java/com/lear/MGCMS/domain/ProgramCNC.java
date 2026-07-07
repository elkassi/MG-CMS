package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ProgramCNC")
public class ProgramCNC {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partNumber")
    private String partNumber;

    @Column(name = "version")
    private String version;

    @Column(name = "row_num")
    private String row;

    @Column(name = "set_num")
    private String set;

    @Column(name = "panelNumber")
    private String panelNumber;

    @Column(name = "pattern")
    private String pattern;

    @Column(name = "programNumber")
    private String programNumber;

    @Column(name = "casette")
    private String casette;

    @Column(name = "coutureDecorativeCnc")
    private String coutureDecorativeCnc;

    @Column(name = "cavitePress")
    private String cavitePress;

    @Column(name = "blindStitch")
    private String blindStitch;

    @Column(name = "profil")
    private String profil;

    @Column(name = "type")
    private String type;

    @Column(name = "code1")
    private String code1;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "updatedBy")
    private String updatedBy;

    // Constructors
    public ProgramCNC() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getRow() { return row; }
    public void setRow(String row) { this.row = row; }

    public String getSet() { return set; }
    public void setSet(String set) { this.set = set; }

    public String getPanelNumber() { return panelNumber; }
    public void setPanelNumber(String panelNumber) { this.panelNumber = panelNumber; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getProgramNumber() { return programNumber; }
    public void setProgramNumber(String programNumber) { this.programNumber = programNumber; }

    public String getCasette() { return casette; }
    public void setCasette(String casette) { this.casette = casette; }

    public String getCoutureDecorativeCnc() { return coutureDecorativeCnc; }
    public void setCoutureDecorativeCnc(String coutureDecorativeCnc) { this.coutureDecorativeCnc = coutureDecorativeCnc; }

    public String getCavitePress() { return cavitePress; }
    public void setCavitePress(String cavitePress) { this.cavitePress = cavitePress; }

    public String getBlindStitch() { return blindStitch; }
    public void setBlindStitch(String blindStitch) { this.blindStitch = blindStitch; }

    public String getProfil() { return profil; }
    public void setProfil(String profil) { this.profil = profil; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCode1() { return code1; }
    public void setCode1(String code1) { this.code1 = code1; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    @Override
    public String toString() {
        return "ProgramCNC{id=" + id + ", partNumber=" + partNumber + ", version=" + version
                + ", row=" + row + ", set=" + set + ", panelNumber=" + panelNumber
                + ", pattern=" + pattern + ", programNumber=" + programNumber + ", casette=" + casette
                + ", coutureDecorativeCnc=" + coutureDecorativeCnc + ", cavitePress=" + cavitePress + ", blindStitch=" + blindStitch
                + ", profil=" + profil + ", type=" + type + ", code1=" + code1 + "}";
    }
}
