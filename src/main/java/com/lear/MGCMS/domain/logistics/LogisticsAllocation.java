package com.lear.MGCMS.domain.logistics;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * One advised roll reservation in the logistics allocation ledger.
 *
 * <p>The keystone that makes rack stock TRUE: once a roll is {@code ADVISED}
 * (or {@code RELEASED}) for a {@code targetZone}, its {@code allocatedMeters}
 * are deducted from rack availability so two zones are never told to use the
 * same roll. Soft/advisory — it records intent, it does not lock physical
 * stock — and is the baseline for over-consumption alerts.</p>
 *
 * <p>Status lifecycle: {@code ADVISED -> RELEASED -> CONSUMED}, with
 * {@code RETURNED} / {@code CANCELLED} as off-ramps. Rows in
 * {@code (ADVISED, RELEASED)} count against availability.</p>
 *
 * <p>Backed by Flyway migration {@code V16_02}. Columns are camelCase in the
 * DB (no @Column name override), matching this project's naming strategy.</p>
 */
@Entity
@Table(
    name = "logistics_allocation",
    indexes = {
        @Index(name = "ix_logistics_allocation_serial", columnList = "serialId"),
        @Index(name = "ix_logistics_allocation_status", columnList = "status"),
        @Index(name = "ix_logistics_allocation_sequence", columnList = "sequence"),
        @Index(name = "ix_logistics_allocation_material_zone", columnList = "refTissus, targetZone")
    }
)
public class LogisticsAllocation {

    public enum Status {
        ADVISED,
        RELEASED,
        CONSUMED,
        RETURNED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String sequence;

    @Column(length = 64, nullable = false)
    private String serie;

    @Column(length = 64, nullable = false)
    private String refTissus;

    /** The roll (Scan_Rouleau.serialId). */
    @Column(length = 64, nullable = false)
    private String serialId;

    @Column(length = 64)
    private String sourceRack;

    @Column(length = 64)
    private String sourceZone;

    @Column(length = 64, nullable = false)
    private String targetZone;

    @Column(nullable = false)
    private Double allocatedMeters;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status;

    @Column(length = 64)
    private String picklistId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 64)
    private String createdBy;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = Status.ADVISED;
    }

    public LogisticsAllocation() {}

    public LogisticsAllocation(String sequence, String serie, String refTissus, String serialId,
                               String sourceRack, String sourceZone, String targetZone,
                               Double allocatedMeters, Status status, String createdBy) {
        this.sequence = sequence;
        this.serie = serie;
        this.refTissus = refTissus;
        this.serialId = serialId;
        this.sourceRack = sourceRack;
        this.sourceZone = sourceZone;
        this.targetZone = targetZone;
        this.allocatedMeters = allocatedMeters;
        this.status = status;
        this.createdBy = createdBy;
    }

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }
    public String getSequence()               { return sequence; }
    public void setSequence(String s)         { this.sequence = s; }
    public String getSerie()                  { return serie; }
    public void setSerie(String s)            { this.serie = s; }
    public String getRefTissus()              { return refTissus; }
    public void setRefTissus(String s)        { this.refTissus = s; }
    public String getSerialId()               { return serialId; }
    public void setSerialId(String s)         { this.serialId = s; }
    public String getSourceRack()             { return sourceRack; }
    public void setSourceRack(String s)       { this.sourceRack = s; }
    public String getSourceZone()             { return sourceZone; }
    public void setSourceZone(String s)       { this.sourceZone = s; }
    public String getTargetZone()             { return targetZone; }
    public void setTargetZone(String s)       { this.targetZone = s; }
    public Double getAllocatedMeters()        { return allocatedMeters; }
    public void setAllocatedMeters(Double m)  { this.allocatedMeters = m; }
    public Status getStatus()                 { return status; }
    public void setStatus(Status status)      { this.status = status; }
    public String getPicklistId()             { return picklistId; }
    public void setPicklistId(String p)       { this.picklistId = p; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public String getCreatedBy()              { return createdBy; }
    public void setCreatedBy(String c)        { this.createdBy = c; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }
    public void setUpdatedAt(LocalDateTime u) { this.updatedAt = u; }
}
