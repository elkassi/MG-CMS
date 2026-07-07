package com.lear.MGCMS.domain.dispatcher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Child of {@link ShiftZoneConfirmation} — one row per machine the chef
 * flags up/down for a given (date, shift, zone) triple.
 *
 * <p>{@link #isUp} is the authoritative "this machine is running this shift"
 * signal that {@code ActiveMachineResolver} (Phase 3) reads to gate the
 * engine. Machines missing from this child list are treated as "down".</p>
 *
 * <p>Backed by table {@code shift_zone_confirmation_machine} (Flyway
 * {@code V2_04}). Stores {@code machine_nom} as a plain string rather than a
 * FK to {@code ProductionTable} — machines can be renamed/re-provisioned
 * and we want the confirmation history to survive that.</p>
 */
@Entity
@Table(
    name = "shift_zone_confirmation_machine",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_szcm_confirmation_machine",
        columnNames = {"confirmation_id", "machine_nom"}
    )
)
public class ShiftZoneConfirmationMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmation_id", nullable = false)
    private ShiftZoneConfirmation confirmation;

    @Column(name = "machine_nom", length = 100, nullable = false)
    private String machineNom;

    @Column(name = "is_up", nullable = false)
    private boolean isUp = true;

    public ShiftZoneConfirmationMachine() {}

    public ShiftZoneConfirmationMachine(ShiftZoneConfirmation confirmation, String machineNom, boolean isUp) {
        this.confirmation = confirmation;
        this.machineNom = machineNom;
        this.isUp = isUp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ShiftZoneConfirmation getConfirmation() { return confirmation; }
    public void setConfirmation(ShiftZoneConfirmation confirmation) { this.confirmation = confirmation; }

    public String getMachineNom() { return machineNom; }
    public void setMachineNom(String machineNom) { this.machineNom = machineNom; }

    public boolean isUp() { return isUp; }
    public void setUp(boolean up) { this.isUp = up; }
}
