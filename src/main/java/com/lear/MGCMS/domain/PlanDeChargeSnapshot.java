package com.lear.MGCMS.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persisted snapshot of the Plan de Charge "Détails de charge" for one past shift.
 *
 * <p>The page recomputes charge/counters/indicators live only for the current and
 * next shift. Any older shift is served from this snapshot instead of re-reading
 * series + cut files (the expensive part), keeping the page fast. The payload is
 * the JSON the frontend builds in {@code loadChargeDetails} (chargeSummary +
 * detailedSeries + partNumberReport + nonImportedCharge), following the
 * {@link com.lear.MGCMS.domain.logistics.LogisticsPicklist#getSnapshotJson()}
 * precedent — the entity just stores the string.</p>
 */
@Entity
@Table(name = "plan_de_charge_snapshot", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"shift_date", "shift_number"})
})
public class PlanDeChargeSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "shift_number", nullable = false)
    private Integer shiftNumber;

    @Lob
    @Column(name = "snapshot_json", columnDefinition = "NVARCHAR(MAX)")
    private String snapshotJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public PlanDeChargeSnapshot() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public Integer getShiftNumber() {
        return shiftNumber;
    }

    public void setShiftNumber(Integer shiftNumber) {
        this.shiftNumber = shiftNumber;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
