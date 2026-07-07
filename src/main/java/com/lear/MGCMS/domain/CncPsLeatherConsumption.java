package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CncPsLeatherConsumption")
public class CncPsLeatherConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference("session-consumptions")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessionId")
    private CncPsSession session;

    @Column(name = "leatherPartNumber")
    private String leatherPartNumber;

    @Column(name = "serial")
    private String serial;

    @Column(name = "lot")
    private String lot;

    @Column(name = "quantiteInitial")
    private Double quantiteInitial;

    @Column(name = "quantiteConsumed")
    private Double quantiteConsumed;

    @Column(name = "quantiteRetour")
    private Double quantiteRetour;

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
    public CncPsLeatherConsumption() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CncPsSession getSession() { return session; }
    public void setSession(CncPsSession session) { this.session = session; }

    public String getLeatherPartNumber() { return leatherPartNumber; }
    public void setLeatherPartNumber(String leatherPartNumber) { this.leatherPartNumber = leatherPartNumber; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public String getLot() { return lot; }
    public void setLot(String lot) { this.lot = lot; }

    public Double getQuantiteInitial() { return quantiteInitial; }
    public void setQuantiteInitial(Double quantiteInitial) { this.quantiteInitial = quantiteInitial; }

    public Double getQuantiteConsumed() { return quantiteConsumed; }
    public void setQuantiteConsumed(Double quantiteConsumed) { this.quantiteConsumed = quantiteConsumed; }

    public Double getQuantiteRetour() { return quantiteRetour; }
    public void setQuantiteRetour(Double quantiteRetour) { this.quantiteRetour = quantiteRetour; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
