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
 * Audit row for every change to a sequence's dispatched zone or chef-pin
 * status. Backed by table {@code dispatch_audit} (Flyway {@code V12_01}).
 *
 * <p>Written by every endpoint in {@code DispatcherController} that
 * mutates {@code CuttingRequest.dispatched_zone} or
 * {@code CuttingRequest.pinned_by_chef}. Read by the audit panel in the
 * Process Dispatcher page (§4.6 of MASTER_SCHEDULING_VISION_v3.md).</p>
 *
 * <p>Retention: pruned by {@code RetentionCronService} after
 * {@code mgcms.retention.days} (default 7).</p>
 */
@Entity
@Table(
    name = "dispatch_audit",
    indexes = {
        @Index(name = "IX_dispatch_audit_seq", columnList = "sequence, created_at"),
        @Index(name = "IX_dispatch_audit_at",  columnList = "created_at"),
        @Index(name = "IX_dispatch_audit_trg", columnList = "trigger_code, created_at")
    }
)
public class DispatchAudit {

    /**
     * Why an audit row was written. Used by the UI to colour the row
     * (manual = blue, auto = grey, pin = amber, mid-shift-reject = red).
     */
    public enum Trigger {
        /** Auto-tick or shift-boundary publish. */
        AUTO,
        /** Process clicked Publish on the dispatcher page. */
        PUBLISH,
        /** Process clicked "Forcer ailleurs" on a single sequence. */
        MANUAL,
        /** Process clicked "Réquilibrer" — full re-greedy. */
        REBALANCE,
        /** Chef accepted a sequence. */
        CHEF_ACCEPT,
        /** Chef rejected a sequence (with reason). */
        CHEF_REJECT,
        /** Chef pinned a sequence to their zone. */
        CHEF_PIN,
        /** Chef pulled a SHARED-overflow sequence into their STRICT zone. */
        CHEF_PULL,
        /** Process or Admin removed a chef pin. */
        UNPIN,
        /** Mid-shift re-dispatch triggered by an event (chef toggled a machine off). */
        MID_SHIFT_REJECT,
        /** Engine auto-tick re-published. */
        ENGINE_TICK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence", length = 64, nullable = false)
    private String sequence;

    @Column(name = "from_zone", length = 64)
    private String fromZone;

    @Column(name = "to_zone", length = 64)
    private String toZone;

    @Column(name = "reason", length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_code", length = 32, nullable = false)
    private Trigger trigger;

    @Column(name = "matricule", length = 32)
    private String matricule;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public DispatchAudit() {}

    public DispatchAudit(String sequence, String fromZone, String toZone,
                         String reason, Trigger trigger, String matricule) {
        this.sequence = sequence;
        this.fromZone = fromZone;
        this.toZone = toZone;
        this.reason = reason;
        this.trigger = trigger;
        this.matricule = matricule;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSequence() { return sequence; }
    public void setSequence(String sequence) { this.sequence = sequence; }

    public String getFromZone() { return fromZone; }
    public void setFromZone(String fromZone) { this.fromZone = fromZone; }

    public String getToZone() { return toZone; }
    public void setToZone(String toZone) { this.toZone = toZone; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Trigger getTrigger() { return trigger; }
    public void setTrigger(Trigger trigger) { this.trigger = trigger; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
