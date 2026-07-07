package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.EtatMachineHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EtatMachineHistoriqueRepository extends JpaRepository<EtatMachineHistorique, Long> {

    List<EtatMachineHistorique> findByMachine(String machine);

    List<EtatMachineHistorique> findByMachineAndEndDateIsNull(String machine);

    /**
     * Find all EtatMachineHistorique records that overlap with the given date range.
     * A record overlaps if:
     * - startDate <= rangeEnd AND (endDate IS NULL OR endDate >= rangeStart)
     */
    @Query("SELECT e FROM EtatMachineHistorique e WHERE " +
           "e.startDate <= :rangeEnd AND (e.endDate IS NULL OR e.endDate >= :rangeStart) " +
           "ORDER BY e.machine, e.startDate")
    List<EtatMachineHistorique> findByDateRangeOverlap(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd);

    /**
     * Find all EtatMachineHistorique records for a specific machine that overlap with the given date range.
     */
    @Query("SELECT e FROM EtatMachineHistorique e WHERE " +
           "e.machine = :machine AND " +
           "e.startDate <= :rangeEnd AND (e.endDate IS NULL OR e.endDate >= :rangeStart) " +
           "ORDER BY e.startDate")
    List<EtatMachineHistorique> findByMachineAndDateRangeOverlap(
            @Param("machine") String machine,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd);

    /**
     * Find all active (unclosed) records for a machine.
     */
    @Query("SELECT e FROM EtatMachineHistorique e WHERE " +
           "e.machine = :machine AND e.endDate IS NULL " +
           "ORDER BY e.startDate DESC")
    List<EtatMachineHistorique> findActiveByMachine(@Param("machine") String machine);

    /**
     * Find the status of a machine at a specific point in time.
     */
    @Query("SELECT e FROM EtatMachineHistorique e WHERE " +
           "e.machine = :machine AND " +
           "e.startDate <= :dateTime AND (e.endDate IS NULL OR e.endDate >= :dateTime) " +
           "ORDER BY e.startDate DESC")
    List<EtatMachineHistorique> findByMachineAndDateTime(
            @Param("machine") String machine,
            @Param("dateTime") LocalDateTime dateTime);

    /**
     * Find all distinct machines that have history records.
     */
    @Query("SELECT DISTINCT e.machine FROM EtatMachineHistorique e ORDER BY e.machine")
    List<String> findDistinctMachines();

    /**
     * Find records by code etat.
     */
    List<EtatMachineHistorique> findByCodeEtat(String codeEtat);

    /**
     * Count active breakdowns (PN status with no end date).
     */
    @Query("SELECT COUNT(e) FROM EtatMachineHistorique e WHERE e.codeEtat = 'PN' AND e.endDate IS NULL")
    Long countActiveBreakdowns();

    /**
     * Find records where the status interval overlaps with [dateDebut, dateFin].
     * A record overlaps if it started before dateFin AND (has no end OR ended after dateDebut).
     */
    @Query("SELECT e FROM EtatMachineHistorique e WHERE " +
           "e.startDate < :dateFin AND (e.endDate IS NULL OR e.endDate > :dateDebut) " +
           "ORDER BY e.machine, e.startDate")
    List<EtatMachineHistorique> findListBetweenDate(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    /**
     * Batch get current status for ALL machines at a point in time.
     * Returns [machine, codeEtat] pairs. Much faster than per-machine queries.
     */
    @Query("SELECT e.machine, e.codeEtat FROM EtatMachineHistorique e WHERE " +
           "e.startDate <= :dateTime AND (e.endDate IS NULL OR e.endDate >= :dateTime)")
    List<Object[]> findAllCurrentStatuses(@Param("dateTime") LocalDateTime dateTime);
}
