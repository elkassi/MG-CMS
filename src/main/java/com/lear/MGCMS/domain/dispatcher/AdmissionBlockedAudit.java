package com.lear.MGCMS.domain.dispatcher;

import java.time.LocalDate;
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
 * Audit row for every admission check the engine or kiosk rejected.
 *
 * <p>Written by {@code AdmissionService} in Phase 9 when a serie would
 * have been scheduled into a zone that isn't confirmed for the shift (or
 * whose target machine is down). The row survives for 7 days; Phase 11
 * schedules the retention sweep.</p>
 *
 * <p>{@code reason_code} shares its enum with {@link UnassignableSerie.ReasonCode}
 * plus a couple of admission-only reasons (pin conflict, capacity
 * exceeded) so downstream dashboards can reuse the same legend.</p>
 */
@Entity
@Table(
    name = "admission_blocked_audit",
    indexes = {
        @Index(name = "IX_aba_created_at", columnList = "created_at"),
        @Index(name = "IX_aba_serie_id", columnList = "serie_id, created_at")
    }
)
public class AdmissionBlockedAudit {

    public enum ReasonCode {
        NO_ZONE_ACCEPTING_TYPE,
        ALL_ZONES_CLOSED_FOR_SHIFT,
        NO_ACTIVE_MACHINE_IN_ZONE,
        PIN_CONFLICT,
        SHIFT_CAPACITY_EXCEEDED,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serie_id", length = 100, nullable = false)
    private String serieId;

    @Column(name = "zone_nom", length = 64, nullable = false)
    private String zoneNom;

    @Column(name = "date_production", nullable = false)
    private LocalDate dateProduction;

    @Column(name = "shift_number", nullable = false)
    private Integer shiftNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 64, nullable = false)
    private ReasonCode reasonCode;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "requested_by_matricule", length = 50)
    private String requestedByMatricule;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public AdmissionBlockedAudit() {}

    public AdmissionBlockedAudit(String serieId, String zoneNom, LocalDate date, int shift,
                                 ReasonCode reasonCode, String reasonDetail,
                                 String requestedByMatricule) {
        this.serieId = serieId;
        this.zoneNom = zoneNom;
        this.dateProduction = date;
        this.shiftNumber = shift;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.requestedByMatricule = requestedByMatricule;
    }

    public Long getId()                 { return id; }
    public void setId(Long id)          { this.id = id; }
    public String getSerieId()          { return serieId; }
    public void setSerieId(String s)    { this.serieId = s; }
    public String getZoneNom()          { return zoneNom; }
    public void setZoneNom(String s)    { this.zoneNom = s; }
    public LocalDate getDateProduction(){ return dateProduction; }
    public void setDateProduction(LocalDate d) { this.dateProduction = d; }
    public Integer getShiftNumber()     { return shiftNumber; }
    public void setShiftNumber(Integer s){ this.shiftNumber = s; }
    public ReasonCode getReasonCode()   { return reasonCode; }
    public void setReasonCode(ReasonCode r){ this.reasonCode = r; }
    public String getReasonDetail()     { return reasonDetail; }
    public void setReasonDetail(String r){ this.reasonDetail = r; }
    public String getRequestedByMatricule() { return requestedByMatricule; }
    public void setRequestedByMatricule(String s) { this.requestedByMatricule = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c){ this.createdAt = c; }
}
