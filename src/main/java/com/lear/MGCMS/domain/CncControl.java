package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CncControl")
public class CncControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference("session-controls")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessionId")
    private CncPsSession session;

    @Column(name = "quantite")
    private Integer quantite;

    @Column(name = "result")
    private String result; // OK or NOK

    @Column(name = "codeDefaut")
    private String codeDefaut; // Code starting with CNC from CodeDefaut entity

    @Column(name = "codeScrap")
    private String codeScrap; // Code starting with CNC from CodeScrap entity

    @Column(name = "matricule")
    private String matricule;

    @Column(name = "panelNumber")
    private String panelNumber;

    @Column(name = "pattern")
    private String pattern;

    @Column(name = "programNumber")
    private String programNumber; // Programme CNC chosen on a NOK control

    @Column(name = "stage")
    private String stage; // CNC, PRESS, BLIND

    @Column(name = "numBonScrap")
    private String numBonScrap; // scrap voucher number (optional, scrap rows only)

    @Column(name = "scrapStatus")
    private String scrapStatus; // EN_ATTENTE_VALIDATION / EN_ATTENTE_MATIERE / REMPLACE / NON_REMPLACABLE (optional)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "machineId")
    private MachineCnc machine; // machine used for this control's stage (per-section selection)

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Constructors
    public CncControl() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CncPsSession getSession() { return session; }
    public void setSession(CncPsSession session) { this.session = session; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getCodeDefaut() { return codeDefaut; }
    public void setCodeDefaut(String codeDefaut) { this.codeDefaut = codeDefaut; }

    public String getCodeScrap() { return codeScrap; }
    public void setCodeScrap(String codeScrap) { this.codeScrap = codeScrap; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getPanelNumber() { return panelNumber; }
    public void setPanelNumber(String panelNumber) { this.panelNumber = panelNumber; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getProgramNumber() { return programNumber; }
    public void setProgramNumber(String programNumber) { this.programNumber = programNumber; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getNumBonScrap() { return numBonScrap; }
    public void setNumBonScrap(String numBonScrap) { this.numBonScrap = numBonScrap; }

    public String getScrapStatus() { return scrapStatus; }
    public void setScrapStatus(String scrapStatus) { this.scrapStatus = scrapStatus; }

    public MachineCnc getMachine() { return machine; }
    public void setMachine(MachineCnc machine) { this.machine = machine; }
}
