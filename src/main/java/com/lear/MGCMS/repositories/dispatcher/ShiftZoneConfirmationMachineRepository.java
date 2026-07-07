package com.lear.MGCMS.repositories.dispatcher;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.dispatcher.ShiftZoneConfirmationMachine;

/**
 * Plain CRUD for the child rows. Phase 3's {@code ActiveMachineResolver}
 * uses {@link #findUpMachinesForTriple} to answer "which machines are
 * legitimately schedulable this shift for this zone?".
 */
public interface ShiftZoneConfirmationMachineRepository extends JpaRepository<ShiftZoneConfirmationMachine, Long> {

    /**
     * All machines the chef flagged {@code is_up = true} for a given
     * (date, shift, zone). Returns empty when no confirmation exists at all
     * — the engine should treat that case as "zone closed".
     */
    @Query("SELECT m FROM ShiftZoneConfirmationMachine m "
         + "WHERE m.confirmation.dateProduction = :date "
         + "  AND m.confirmation.shiftNumber = :shift "
         + "  AND m.confirmation.zone.nom = :zoneNom "
         + "  AND m.isUp = true")
    List<ShiftZoneConfirmationMachine> findUpMachinesForTriple(LocalDate date, int shift, String zoneNom);

    /** Every child row for a confirmation — handy for the chef's live page. */
    @Query("SELECT m FROM ShiftZoneConfirmationMachine m "
         + "WHERE m.confirmation.id = :confirmationId")
    List<ShiftZoneConfirmationMachine> findByConfirmationId(Long confirmationId);
}
