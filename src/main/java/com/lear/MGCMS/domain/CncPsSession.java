package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CncPsSession", uniqueConstraints = @UniqueConstraint(columnNames = "boxId"))
public class CncPsSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "boxId", unique = true)
    private String boxId;

    @Column(name = "nSequenceImp")
    private String nSequenceImp;

    @Column(name = "partNumberImp")
    private String partNumberImp;

    @Column(name = "code1Imp")
    private String code1Imp;

    @Column(name = "code3Imp")
    private String code3Imp;

    @Column(name = "quantiteImp")
    private String quantiteImp;

    @Column(name = "operator")
    private String operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "labelPrinted")
    private Boolean labelPrinted = false;

    // Production machine (set by the production flow). Quality-control machines are now
    // recorded per control row on CncControl, not on the session.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "machineCncId")
    private MachineCnc machineCnc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "startProductionDate")
    private LocalDateTime startProductionDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "endProductionDate")
    private LocalDateTime endProductionDate;

    @Column(name = "productionStatus")
    private String productionStatus; // Waiting / In progress / Complete

    @Column(name = "productionOperator")
    private String productionOperator;

    // Quality control tracking
    @Column(name = "qualiteStatus")
    private String qualiteStatus; // null (not started) / En cours / Terminé

    @Column(name = "userQualite")
    private String userQualite;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "startDateControl")
    private LocalDateTime startDateControl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "endDateControl")
    private LocalDateTime endDateControl;

    @JsonManagedReference("session-consumptions")
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<CncPsLeatherConsumption> consumptions = new HashSet<>();

    @JsonManagedReference("session-controls")
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<CncControl> controls = new HashSet<>();

    @JsonManagedReference("session-productions")
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CncProduction> productions = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Constructors
    public CncPsSession() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBoxId() { return boxId; }
    public void setBoxId(String boxId) { this.boxId = boxId; }

    public String getnSequenceImp() { return nSequenceImp; }
    public void setnSequenceImp(String nSequenceImp) { this.nSequenceImp = nSequenceImp; }

    public String getPartNumberImp() { return partNumberImp; }
    public void setPartNumberImp(String partNumberImp) { this.partNumberImp = partNumberImp; }

    public String getCode1Imp() { return code1Imp; }
    public void setCode1Imp(String code1Imp) { this.code1Imp = code1Imp; }

    public String getCode3Imp() { return code3Imp; }
    public void setCode3Imp(String code3Imp) { this.code3Imp = code3Imp; }

    public String getQuantiteImp() { return quantiteImp; }
    public void setQuantiteImp(String quantiteImp) { this.quantiteImp = quantiteImp; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public Boolean getLabelPrinted() { return labelPrinted; }
    public void setLabelPrinted(Boolean labelPrinted) { this.labelPrinted = labelPrinted; }

    public MachineCnc getMachineCnc() { return machineCnc; }
    public void setMachineCnc(MachineCnc machineCnc) { this.machineCnc = machineCnc; }

    public LocalDateTime getStartProductionDate() { return startProductionDate; }
    public void setStartProductionDate(LocalDateTime startProductionDate) { this.startProductionDate = startProductionDate; }

    public LocalDateTime getEndProductionDate() { return endProductionDate; }
    public void setEndProductionDate(LocalDateTime endProductionDate) { this.endProductionDate = endProductionDate; }

    public String getProductionStatus() { return productionStatus; }
    public void setProductionStatus(String productionStatus) { this.productionStatus = productionStatus; }

    public String getProductionOperator() { return productionOperator; }
    public void setProductionOperator(String productionOperator) { this.productionOperator = productionOperator; }

    public String getQualiteStatus() { return qualiteStatus; }
    public void setQualiteStatus(String qualiteStatus) { this.qualiteStatus = qualiteStatus; }

    public String getUserQualite() { return userQualite; }
    public void setUserQualite(String userQualite) { this.userQualite = userQualite; }

    public LocalDateTime getStartDateControl() { return startDateControl; }
    public void setStartDateControl(LocalDateTime startDateControl) { this.startDateControl = startDateControl; }

    public LocalDateTime getEndDateControl() { return endDateControl; }
    public void setEndDateControl(LocalDateTime endDateControl) { this.endDateControl = endDateControl; }

    public Set<CncPsLeatherConsumption> getConsumptions() { return consumptions; }
    public void setConsumptions(Set<CncPsLeatherConsumption> consumptions) { this.consumptions = consumptions; }

    public Set<CncControl> getControls() { return controls; }
    public void setControls(Set<CncControl> controls) { this.controls = controls; }

    public Set<CncProduction> getProductions() { return productions; }
    public void setProductions(Set<CncProduction> productions) { this.productions = productions; }
}
