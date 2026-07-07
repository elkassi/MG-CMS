package com.lear.MGCMS.domain.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;

/**
 * One row per (date, shift, zone) when a chef-de-zone says "my zone is up
 * for this shift". Until this row exists, the ordonnancement engine refuses
 * to schedule into that zone for that shift — see Phase 7's admission gate.
 *
 * <p>Backed by table {@code shift_zone_confirmation} (Flyway {@code V2_04}).
 * Child rows in {@link ShiftZoneConfirmationMachine} capture the chef's
 * up/down call per machine. Deleting a parent cascades to its children.</p>
 */
@Entity
@Table(
    name = "shift_zone_confirmation",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_szc_date_shift_zone",
        columnNames = {"date_production", "shift_number", "zone_nom"}
    )
)
public class ShiftZoneConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_production", nullable = false)
    private LocalDate dateProduction;

    @Column(name = "shift_number", nullable = false)
    private Integer shiftNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_nom", referencedColumnName = "nom", nullable = false)
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmed_by_user_id", referencedColumnName = "matricule", nullable = false)
    private User confirmedBy;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @OneToMany(mappedBy = "confirmation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShiftZoneConfirmationMachine> machines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.confirmedAt == null) {
            this.confirmedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDateProduction() { return dateProduction; }
    public void setDateProduction(LocalDate dateProduction) { this.dateProduction = dateProduction; }

    public Integer getShiftNumber() { return shiftNumber; }
    public void setShiftNumber(Integer shiftNumber) { this.shiftNumber = shiftNumber; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public User getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(User confirmedBy) { this.confirmedBy = confirmedBy; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public List<ShiftZoneConfirmationMachine> getMachines() { return machines; }
    public void setMachines(List<ShiftZoneConfirmationMachine> machines) { this.machines = machines; }

    /** Convenience: add a machine child row and back-link it. */
    public ShiftZoneConfirmationMachine addMachine(String machineNom, boolean isUp) {
        ShiftZoneConfirmationMachine m = new ShiftZoneConfirmationMachine(this, machineNom, isUp);
        this.machines.add(m);
        return m;
    }
}
