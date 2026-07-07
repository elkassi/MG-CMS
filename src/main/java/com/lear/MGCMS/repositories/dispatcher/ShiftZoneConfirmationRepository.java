package com.lear.MGCMS.repositories.dispatcher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.dispatcher.ShiftZoneConfirmation;

/**
 * Plain CRUD over {@link ShiftZoneConfirmation}. Business logic (shift-gate
 * evaluation, admission audit) lives in Phase 3's {@code ActiveMachineResolver}.
 */
public interface ShiftZoneConfirmationRepository extends JpaRepository<ShiftZoneConfirmation, Long> {

    /** The single confirmation row for (date, shift, zone), if any. */
    @Query("SELECT szc FROM ShiftZoneConfirmation szc "
         + "WHERE szc.dateProduction = :date AND szc.shiftNumber = :shift "
         + "  AND szc.zone.nom = :zoneNom")
    Optional<ShiftZoneConfirmation> findForTriple(LocalDate date, int shift, String zoneNom);

    /** Every zone confirmation for a given (date, shift) — used by the Process page. */
    @Query("SELECT szc FROM ShiftZoneConfirmation szc "
         + "WHERE szc.dateProduction = :date AND szc.shiftNumber = :shift")
    List<ShiftZoneConfirmation> findForShift(LocalDate date, int shift);
}
