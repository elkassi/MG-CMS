package com.lear.MGCMS.domain.logistics;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * Stable printable snapshot of a logistics release.
 *
 * <p>The picklist write touches two databases ({@code suiviplanning} in the CMS
 * datasource and MG-CMS' local lifecycle / ledger tables). Keeping the printed
 * payload here gives logistics a stable reprint trail even if live stock or
 * recommendations change minutes later.</p>
 */
@Entity
@Table(name = "logistics_picklist")
public class LogisticsPicklist {

    @Id
    @Column(length = 64)
    private String id;

    private LocalDate releaseDate;

    private Integer shift;

    private Integer sequenceCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 64)
    private String createdBy;

    @Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String snapshotJson;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public Integer getShift() { return shift; }
    public void setShift(Integer shift) { this.shift = shift; }
    public Integer getSequenceCount() { return sequenceCount; }
    public void setSequenceCount(Integer sequenceCount) { this.sequenceCount = sequenceCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
}
