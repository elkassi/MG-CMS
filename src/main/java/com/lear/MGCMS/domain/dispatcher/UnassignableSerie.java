package com.lear.MGCMS.domain.dispatcher;

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
 * Audit row for every serie the dispatcher couldn't place into any zone for
 * a given (date, shift). Written by the upcoming {@code SchedulableSerieFilter}
 * (Phase 3). The Process page surfaces these as "needs manual intervention".
 *
 * <p>Backed by table {@code unassignable_serie} (Flyway {@code V2_05}).
 * A 7-day retention cron prunes old rows (Phase 11).</p>
 */
@Entity
@Table(
    name = "unassignable_serie",
    indexes = {
        @Index(name = "IX_unassignable_serie_id_created", columnList = "serie_id, created_at"),
        @Index(name = "IX_unassignable_created_at", columnList = "created_at")
    }
)
public class UnassignableSerie {

    /** Well-known reasons a serie can't be placed. Stored as VARCHAR. */
    public enum ReasonCode {
        /** No zone in the plant hosts this machine type at all (misconfig). */
        NO_ZONE_ACCEPTING_TYPE,
        /** Zones exist but no ShiftZoneConfirmation has been written for this (date, shift). */
        ALL_ZONES_CLOSED_FOR_SHIFT,
        /** Zone is confirmed but every machine in it is flagged is_up = 0. */
        NO_ACTIVE_MACHINE_IN_ZONE,
        /** Free-form fallback — always pair with a human-readable reason_detail. */
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serie_id", length = 100, nullable = false)
    private String serieId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 64, nullable = false)
    private ReasonCode reasonCode;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public UnassignableSerie() {}

    public UnassignableSerie(String serieId, ReasonCode reasonCode, String reasonDetail) {
        this.serieId = serieId;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSerieId() { return serieId; }
    public void setSerieId(String serieId) { this.serieId = serieId; }

    public ReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(ReasonCode reasonCode) { this.reasonCode = reasonCode; }

    public String getReasonDetail() { return reasonDetail; }
    public void setReasonDetail(String reasonDetail) { this.reasonDetail = reasonDetail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
