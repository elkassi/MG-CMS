package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CncProduction")
public class CncProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference("session-productions")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessionId")
    private CncPsSession session;

    @Column(name = "panelNumber")
    private String panelNumber;

    @Column(name = "pattern")
    private String pattern;

    @Column(name = "programmeNumber")
    private String programmeNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "machineCncId")
    private MachineCnc machineCnc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "startDate")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "endDate")
    private LocalDateTime endDate;

    @Column(name = "status")
    private String status; // In progress / Complete

    @Column(name = "operator")
    private String operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public CncProduction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CncPsSession getSession() { return session; }
    public void setSession(CncPsSession session) { this.session = session; }

    public String getPanelNumber() { return panelNumber; }
    public void setPanelNumber(String panelNumber) { this.panelNumber = panelNumber; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getProgrammeNumber() { return programmeNumber; }
    public void setProgrammeNumber(String programmeNumber) { this.programmeNumber = programmeNumber; }

    public MachineCnc getMachineCnc() { return machineCnc; }
    public void setMachineCnc(MachineCnc machineCnc) { this.machineCnc = machineCnc; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
