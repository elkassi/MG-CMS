package com.lear.MGCMS.repositories.dispatcher;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry;

/**
 * Persistence for the engine's current best schedule (Flyway V15_01).
 *
 * <p>The table is treated as a single "current snapshot" — writes from
 * the engine clear and replace, reads serve external integrations
 * (notably {@code /api/public/next-series}).</p>
 */
public interface EngineScheduleEntryRepository
        extends JpaRepository<EngineScheduleEntry, EngineScheduleEntry.Pk> {

    /** Ordered list for a given machine, earliest planned slot first. */
    @Query("SELECT e FROM EngineScheduleEntry e WHERE e.machineNom = :machineNom "
            + "AND e.id.phase = com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry$Phase.COUPE "
            + "ORDER BY e.plannedStart NULLS LAST, e.id.serieId")
    List<EngineScheduleEntry> findCoupeQueueForMachine(@Param("machineNom") String machineNom);

    /**
     * Public next-series queue, defensively scoped to active sequences due in
     * the current plant bucket or older. This prevents a stale persisted
     * schedule from exposing next-shift work before the engine rebuilds.
     */
    @Query("SELECT e FROM EngineScheduleEntry e, CuttingRequest cr "
            + "WHERE e.sequenceId = cr.sequence "
            + "AND e.machineNom = :machineNom "
            + "AND e.id.phase = com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry$Phase.COUPE "
            + "AND cr.sequenceStatus IN ('RELEASED', 'STARTED') "
            + "AND cr.dueDate IS NOT NULL "
            + "AND cr.dueShift IS NOT NULL "
            + "AND (cr.dueDate < :date "
            + "     OR (cr.dueDate = :date AND ( "
            + "         (:shift = '1' AND cr.dueShift = '1') "
            + "      OR (:shift = '2' AND cr.dueShift IN ('1', '2')) "
            + "      OR (:shift = '3' AND cr.dueShift IN ('1', '2', '3')) "
            + "     ))) "
            + "ORDER BY e.plannedStart NULLS LAST, e.id.serieId")
    List<EngineScheduleEntry> findCurrentCoupeQueueForMachine(@Param("machineNom") String machineNom,
                                                              @Param("date") java.time.LocalDate date,
                                                              @Param("shift") String shift);

    @Query("SELECT e FROM EngineScheduleEntry e "
            + "WHERE e.id.phase = com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry$Phase.COUPE "
            + "AND (:zoneNom IS NULL OR e.zoneNom = :zoneNom) "
            + "AND (e.plannedStart IS NULL OR e.plannedStart <= :horizonEnd) "
            + "ORDER BY e.plannedStart NULLS LAST, e.machineNom, e.id.serieId")
    List<EngineScheduleEntry> findCoupeEntriesForForecast(@Param("zoneNom") String zoneNom,
                                                          @Param("horizonEnd") LocalDateTime horizonEnd);

    /**
     * Lightweight workbench focus query. The caller already scopes sequences
     * to active current-or-overdue due buckets, so this reads only the current
     * balanced plan entries needed for the two-hour chef/logistics horizon.
     */
    @Query("SELECT e FROM EngineScheduleEntry e "
            + "WHERE e.sequenceId IN :sequences "
            + "AND e.id.phase = com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry$Phase.COUPE "
            + "AND e.plannedStart IS NOT NULL "
            + "AND e.plannedStart <= :horizonEnd "
            + "ORDER BY e.plannedStart, e.machineNom, e.id.serieId")
    List<EngineScheduleEntry> findCoupeEntriesForSequencesWithinHorizon(
            @Param("sequences") List<String> sequences,
            @Param("horizonEnd") LocalDateTime horizonEnd);

    /** All planned entries for a sequence (both phases). */
    @Query("SELECT e FROM EngineScheduleEntry e WHERE e.sequenceId = :sequenceId")
    List<EngineScheduleEntry> findBySequence(@Param("sequenceId") String sequenceId);

    /** Batched schedule overlay read for the Workbench Gantt/table. */
    @Query("SELECT e FROM EngineScheduleEntry e "
            + "WHERE e.sequenceId IN :sequences "
            + "ORDER BY e.sequenceId, e.id.serieId, e.id.phase")
    List<EngineScheduleEntry> findBySequences(@Param("sequences") List<String> sequences);

    /** Wipe-all — engine replaces the table on each persist cycle. */
    @Modifying
    @Transactional
    @Query("DELETE FROM EngineScheduleEntry")
    void deleteAllInBatchFast();
}
