package com.lear.MGCMS.domain.dispatcher;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;

/**
 * Many-to-many link between a {@link User} (chef-de-zone) and a {@link Zone}.
 *
 * <p>Backed by table {@code user_zone} (see Flyway {@code V2_03}). Created and
 * managed by the upcoming {@code UserZoneService} (Phase 3); this class is
 * just the entity + accessors. See the class-level Javadoc on
 * {@link com.lear.MGCMS.domain.Zone.Category} for the STRICT/SHARED model the
 * links live against.</p>
 *
 * <p>A chef can own multiple zones; {@link #isDefault} marks the one that
 * opens first on their Chef-de-Zone page. {@link #revokedAt} soft-deletes
 * the link while keeping the audit trail.</p>
 */
@Entity
@Table(
    name = "user_zone",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_user_zone_user_zone",
        columnNames = {"user_id", "zone_nom"}
    )
)
public class UserZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "matricule", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_nom", referencedColumnName = "nom", nullable = false)
    private Zone zone;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /** {@code users.matricule} of the admin who created the link. Optional. */
    @Column(name = "assigned_by", length = 50)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    /** Soft-delete marker. {@code null} means the link is active. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = LocalDateTime.now();
        }
    }

    public UserZone() {}

    public UserZone(User user, Zone zone, boolean isDefault, String assignedBy) {
        this.user = user;
        this.zone = zone;
        this.isDefault = isDefault;
        this.assignedBy = assignedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { this.isDefault = aDefault; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    /** Convenience: a link is active iff it has not been revoked. */
    public boolean isActive() {
        return this.revokedAt == null;
    }
}
