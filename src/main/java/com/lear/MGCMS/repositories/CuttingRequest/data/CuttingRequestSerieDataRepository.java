package com.lear.MGCMS.repositories.CuttingRequest.data;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieDataLight;
import com.lear.MGCMS.payload.SerieReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CuttingRequestSerieDataRepository  extends JpaRepository<CuttingRequestSerieData, String>, JpaSpecificationExecutor<CuttingRequestSerieData> {

    // ======================== ORDONNANCEMENT QUERIES ========================

    CuttingRequestSerieData findBySerie(String serie);

    @Query("Select crs from CuttingRequestSerieData crs "
            + " where crs.tableCoupe is not null "
            + "   and (crs.statusCoupe = 'In progress' or crs.statusCoupe = 'Complete') "
            + " order by crs.tableCoupe asc, crs.dateDebutCoupe asc")
    List<CuttingRequestSerieData> findActiveOrCompletedCoupe();

    @Query("Select crs from CuttingRequestSerieData crs "
            + " where crs.tableCoupe is not null "
            + "   and crs.dateDebutCoupe is not null "
            + "   and (crs.statusCoupe is null or crs.statusCoupe <> 'Waiting') "
            + " order by crs.tableCoupe asc, crs.dateDebutCoupe asc, crs.serie asc")
    List<CuttingRequestSerieData> findStartedCoupeForAutoClose();

    @Query(value =
            "SELECT cur.serie, MIN(nxt.dateDebutCoupe) " +
            "FROM CuttingRequestSerie cur " +
            "JOIN CuttingRequestSerie nxt " +
            "  ON nxt.tableCoupe = cur.tableCoupe " +
            " AND nxt.dateDebutCoupe > cur.dateDebutCoupe " +
            " AND nxt.dateDebutCoupe IS NOT NULL " +
            " AND (nxt.statusCoupe IS NULL OR nxt.statusCoupe <> 'Waiting') " +
            "WHERE cur.tableCoupe IS NOT NULL " +
            "  AND cur.dateDebutCoupe IS NOT NULL " +
            "  AND cur.statusCoupe = 'In progress' " +
            "GROUP BY cur.serie",
            nativeQuery = true)
    List<Object[]> findCoupePassThroughClosures();

    @Modifying
    @Transactional
    @Query("UPDATE CuttingRequestSerieData s SET s.statusCoupe = 'Complete', s.dateFinCoupe = :dateFin "
            + "WHERE s.serie = :serie AND s.statusCoupe = 'In progress'")
    int closeCoupePassThrough(@Param("serie") String serie, @Param("dateFin") LocalDateTime dateFin);

    @Query("Select crs from CuttingRequestSerieData crs "
            + " where crs.tableMatelassage is not null "
            + "   and crs.dateDebutMatelassage is not null "
            + "   and (crs.statusMatelassage is null or crs.statusMatelassage <> 'Waiting') "
            + " order by crs.tableMatelassage asc, crs.dateDebutMatelassage asc, crs.serie asc")
    List<CuttingRequestSerieData> findStartedMatelassageForAutoClose();

    @Query(value =
            "SELECT cur.serie, MIN(nxt.dateDebutMatelassage) " +
            "FROM CuttingRequestSerie cur " +
            "JOIN CuttingRequestSerie nxt " +
            "  ON nxt.tableMatelassage = cur.tableMatelassage " +
            " AND nxt.dateDebutMatelassage > cur.dateDebutMatelassage " +
            " AND nxt.dateDebutMatelassage IS NOT NULL " +
            " AND (nxt.statusMatelassage IS NULL OR nxt.statusMatelassage <> 'Waiting') " +
            "WHERE cur.tableMatelassage IS NOT NULL " +
            "  AND cur.dateDebutMatelassage IS NOT NULL " +
            "  AND cur.statusMatelassage = 'In progress' " +
            "GROUP BY cur.serie",
            nativeQuery = true)
    List<Object[]> findMatelassagePassThroughClosures();

    @Modifying
    @Transactional
    @Query("UPDATE CuttingRequestSerieData s SET s.statusMatelassage = 'Complete', s.dateFinMatelassage = :dateFin "
            + "WHERE s.serie = :serie AND s.statusMatelassage = 'In progress'")
    int closeMatelassagePassThrough(@Param("serie") String serie, @Param("dateFin") LocalDateTime dateFin);

    @Query("Select crs from CuttingRequestSerieData crs "
            + " where crs.dateDebutCoupe is null and crs.dateFinCoupe is null order by crs.planningDate")
    List<CuttingRequestSerieData> findAllNotYet();

    @Query("Select crs from CuttingRequestSerieData crs "
            + " where crs.dateDebutCoupe is not null and crs.dateFinCoupe is null order by crs.dateDebutCoupe")
    List<CuttingRequestSerieData> findAllInProgress();

    @Query("Select crs from CuttingRequestSerieData crs "
            + " where crs.dateDebutCoupe <= :date2 and crs.dateFinCoupe >= :date1 and (:machine is null or crs.tableCoupe = :machine)")
    List<CuttingRequestSerieData> findBetween(LocalDateTime date1, LocalDateTime date2, String machine);

    @Query("Select crs from CuttingRequestSerieData crs "
            + " where crs.dateDebutMatelassage <= :date2 and crs.dateFinMatelassage >= :date1 and (:machine is null or crs.tableMatelassage = :machine)")
    List<CuttingRequestSerieData> findBetween2(LocalDateTime date1, LocalDateTime date2, String machine);

    @Query("Select crs.statusCoupe from CuttingRequestSerieData crs where crs.sequence = :sequence")
    List<String> getStatusCoupeBySequence(String sequence);

    @Query("Select count(*) from CuttingRequestSerieData where sequence = :sequence and statusMatelassage != 'Waiting'")
    Integer countStartedBySequence(String sequence);

    @Query("Select count(*) from CuttingRequestSerieData where sequence = :sequence and statusCoupe != 'Complete'")
    Integer countNonFinishedBySequence(String sequence);

    @Query("from CuttingRequestSerieData where serie in (:series)")
    List<CuttingRequestSerieData> findSeries(List<String> series);

    @Query("select placement from CuttingRequestSerieData where nbrPieceTotal is null or nbrPieceTotal = 0 group by placement")
    List<String> findDistinctPlacement();
    @Modifying
    @Transactional
    @Query("update CuttingRequestSerieData set nbrPiece = :countNbrPiece, nbrPieceTotal = nbrCouche * :countNbrPiece where placement = :placement")
    void updateNbrPiece(String placement, Integer countNbrPiece);

    @Query("select serie from CuttingRequestSerieData where sequence = :sequence and partNumberMaterial = :partNumberMaterial")
    List<String> findSeries(String sequence, String partNumberMaterial);
    @Query("from CuttingRequestSerieData where sequence = :sequence")
    List<CuttingRequestSerieData> findBySequence(String sequence);

    @Query("from CuttingRequestSerieData where sequence in (:sequencesArr)")
    List<CuttingRequestSerieData> findBySequencesArr(List<String> sequencesArr);

    /**
     * Material already promised to production but not yet spread: series still
     * Waiting on matelassage whose sequence is RELEASED or STARTED. Σ of
     * (longueur × nbrCouche) per material. Used by the logistics release recap to
     * subtract committed-but-unspread meters from rack availability.
     * Columns: 0=partNumberMaterial, 1=committedMeters.
     */
    @Query("SELECT s.partNumberMaterial, SUM(s.longueur * s.nbrCouche) FROM CuttingRequestSerieData s "
            + "WHERE s.statusMatelassage = 'Waiting' AND s.partNumberMaterial IS NOT NULL "
            + "AND s.sequence IN (SELECT cr.sequence FROM CuttingRequest cr WHERE cr.sequenceStatus IN ('RELEASED','STARTED')) "
            + "GROUP BY s.partNumberMaterial")
    List<Object[]> sumCommittedWaitingMetersByMaterial();

    /**
     * Row-level detail behind {@link #sumCommittedWaitingMetersByMaterial()} — one
     * row per committed-but-unspread serie, for the logistics recap "Engagé"
     * drill-down. Same WHERE as the aggregate so the rows sum back to its total.
     * Columns: 0=partNumberMaterial, 1=sequence, 2=serie, 3=longueur, 4=nbrCouche.
     */
    @Query("SELECT s.partNumberMaterial, s.sequence, s.serie, s.longueur, s.nbrCouche FROM CuttingRequestSerieData s "
            + "WHERE s.statusMatelassage = 'Waiting' AND s.partNumberMaterial IS NOT NULL "
            + "AND s.sequence IN (SELECT cr.sequence FROM CuttingRequest cr WHERE cr.sequenceStatus IN ('RELEASED','STARTED'))")
    List<Object[]> findCommittedWaitingSeries();

    @Query("SELECT s.sequence FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences "
            + "GROUP BY s.sequence "
            + "HAVING SUM(CASE WHEN LOWER(COALESCE(s.statusCoupe, '')) <> 'complete' THEN 1 ELSE 0 END) = 0")
    List<String> findSequencesWhereAllCoupeComplete(@Param("sequences") List<String> sequences);

    /**
     * Serie progress per sequence for the chef rectification screen.
     * Columns: 0=sequence, 1=total series, 2=series with statusCoupe=Complete.
     */
    @Query("SELECT s.sequence, COUNT(s), "
            + "SUM(CASE WHEN LOWER(COALESCE(s.statusCoupe, '')) = 'complete' THEN 1 ELSE 0 END) "
            + "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences GROUP BY s.sequence")
    List<Object[]> countSerieProgressBySequences(@Param("sequences") List<String> sequences);

    /**
     * Lock-resolution inputs for the zone auto-correction job — the same
     * fields {@code LockResolver.SerieLockInput} reads, restricted to series
     * with cutting activity ({@code statusCoupe != Waiting}) on a known table.
     * Columns: 0=sequence, 1=serie, 2=statusCoupe, 3=tableCoupe, 4=dateDebutCoupe.
     */
    @Query("SELECT s.sequence, s.serie, s.statusCoupe, s.tableCoupe, s.dateDebutCoupe "
            + "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences "
            + "AND s.tableCoupe IS NOT NULL AND s.statusCoupe IS NOT NULL "
            + "AND s.statusCoupe <> 'Waiting'")
    List<Object[]> findLockInputsBySequences(@Param("sequences") List<String> sequences);

    //serie is string , so i think you need to convert it to Long
    @Query("select max(cast(serie as long)) from CuttingRequestSerieData")
    Long getMaxNserie();

    @Query("SELECT COUNT(s) FROM CuttingRequestSerieData s WHERE " +
           "(s.dateDebutMatelassage IS NOT NULL AND s.dateDebutMatelassage > :since) OR " +
           "(s.dateFinMatelassage IS NOT NULL AND s.dateFinMatelassage > :since) OR " +
           "(s.dateDebutCoupe IS NOT NULL AND s.dateDebutCoupe > :since) OR " +
           "(s.dateFinCoupe IS NOT NULL AND s.dateFinCoupe > :since)")
    Long countChangedSince(@Param("since") java.time.LocalDateTime since);

    @Query("from CuttingRequestSerieDataLight where sequence in :sequences")
    List<CuttingRequestSerieDataLight> findBySequences(List<String> sequences);

    // ======================== ORDONNANCEMENT LIGHT QUERIES (19 columns, no @ManyToOne joins) ========================

    /**
     * Find relevant sequences: series with activity in the last 12h window.
     * Uses bounded date range (> since AND < now) to avoid pulling old data.
     * Requires at least one status != 'Waiting' (i.e. work has started on the serie).
     */
    @Query("SELECT DISTINCT s.sequence FROM CuttingRequestSerieData s " +
           "WHERE ((s.dateDebutMatelassage > :since AND s.dateDebutMatelassage < :now) " +
           "OR (s.dateFinMatelassage > :since AND s.dateFinMatelassage < :now) " +
           "OR (s.dateDebutCoupe > :since AND s.dateDebutCoupe < :now) " +
           "OR (s.dateFinCoupe > :since AND s.dateFinCoupe < :now)) " +
           "AND (s.statusCoupe != 'Waiting' OR s.statusMatelassage != 'Waiting')")
    List<String> findRelevantSequences(@Param("since") LocalDateTime since, @Param("now") LocalDateTime now);

    /**
     * Load all series for given sequences (light 20-column projection).
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.partNumberMaterial, s.description, s.longueur, " +
           "s.nbrCouche, s.placement, s.tempsDeCoupe, s.tableCoupe, s.tableMatelassage, " +
           "s.statusCoupe, s.statusMatelassage, s.dateDebutCoupe, s.dateFinCoupe, " +
           "s.dateDebutMatelassage, s.dateFinMatelassage, s.zoneCoupe, s.zoneMatelassage, s.planningDate, " +
           "s.nbrPiece, s.nbrPieceTotal " +
           "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences ORDER BY s.sequence, s.serie")
    List<Object[]> findSeriesBySequencesLight(@Param("sequences") List<String> sequences);

    /**
     * Count series with recent changes (for quick refresh check).
     */
    @Query("SELECT COUNT(s) FROM CuttingRequestSerieData s " +
           "WHERE s.dateDebutCoupe >= :since OR s.dateFinCoupe >= :since " +
           "OR s.dateDebutMatelassage >= :since OR s.dateFinMatelassage >= :since")
    long countRecentChanges(@Param("since") LocalDateTime since);

    /**
     * Load all series for a given (date, shift) — light projection for dispatcher.
     * Columns: 0=serie, 1=sequence, 2=machine, 3=tempsDeCoupe, 4=nbrCouche, 5=placement
     *
     * Filters via the parent CuttingRequest's dueDate/dueShift because serie
     * rows have planningDate/shift mostly null in production data; the parent
     * carries the authoritative due-shift values that Plan de Charge also uses.
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.tempsDeCoupe, s.nbrCouche, s.placement " +
           "FROM CuttingRequestSerieData s WHERE s.sequence IN " +
           "(SELECT cr.sequence FROM CuttingRequest cr WHERE cr.dueDate = :date AND cr.dueShift = :shift)")
    List<Object[]> findSeriesByDateShiftLight(@Param("date") LocalDate date, @Param("shift") String shift);

    /**
     * Load all series for a given list of sequences — same 6-column projection as
     * {@link #findSeriesByDateShiftLight} but without the due-date filter.
     * Used by the Workbench to load all active sequences regardless of due date.
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.tempsDeCoupe, s.nbrCouche, s.placement " +
           "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences")
    List<Object[]> findSeriesBySequencesLightProjection(@Param("sequences") List<String> sequences);

    /**
     * Sequences that have at least one serie already started — i.e. any serie
     * whose statusMatelassage or statusCoupe is not "Waiting". Used by the
     * Continuous Dispatch Optimizer to freeze sequences that the floor has
     * already begun executing (spec SEQUENCE_DISPATCHER_PLAN §13.1).
     *
     * Filters via the parent CuttingRequest's dueDate/dueShift because serie
     * rows have planningDate/shift mostly null in production data.
     */
    @Query("SELECT DISTINCT s.sequence FROM CuttingRequestSerieData s " +
           "WHERE s.sequence IN " +
           "(SELECT cr.sequence FROM CuttingRequest cr WHERE cr.dueDate = :date AND cr.dueShift = :shift) " +
           "AND ((s.statusMatelassage IS NOT NULL AND s.statusMatelassage <> 'Waiting') " +
           "  OR (s.statusCoupe       IS NOT NULL AND s.statusCoupe       <> 'Waiting'))")
    List<String> findStartedSequenceIdsForDateShift(@Param("date") LocalDate date,
                                                    @Param("shift") String shift);

    /**
     * Load series assigned to a specific machine (for occupancy calculation in write ops).
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.partNumberMaterial, s.description, s.longueur, " +
           "s.nbrCouche, s.placement, s.tempsDeCoupe, s.tableCoupe, s.tableMatelassage, " +
           "s.statusCoupe, s.statusMatelassage, s.dateDebutCoupe, s.dateFinCoupe, " +
           "s.dateDebutMatelassage, s.dateFinMatelassage, s.zoneCoupe, s.zoneMatelassage, s.planningDate " +
           "FROM CuttingRequestSerieData s " +
           "WHERE s.tableCoupe = :machineNom AND s.statusCoupe != 'Complete' ORDER BY s.serie")
    List<Object[]> findSeriesOnMachineLight(@Param("machineNom") String machineNom);

    /**
     * Series for a given list of sequences, excluding those already completed.
     * Used by the real-time active-sequences workflow.
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.tempsDeCoupe, s.nbrCouche, s.placement " +
           "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences AND s.statusCoupe <> 'Complete'")
    List<Object[]> findActiveSeriesBySequencesLight(@Param("sequences") List<String> sequences);

    /**
     * Sequences in the given list that have at least one serie already started.
     * Used by the Continuous Dispatch Optimizer to freeze in-progress sequences.
     */
    @Query("SELECT DISTINCT s.sequence FROM CuttingRequestSerieData s " +
           "WHERE s.sequence IN :sequences " +
           "AND ((s.statusMatelassage IS NOT NULL AND s.statusMatelassage <> 'Waiting') " +
           "  OR (s.statusCoupe       IS NOT NULL AND s.statusCoupe       <> 'Waiting'))")
    List<String> findStartedSequenceIdsIn(@Param("sequences") List<String> sequences);

    /**
     * Sequences in the given list that have at least one serie with statusCoupe = 'Complete'.
     * These sequences are considered "fixed" to their current zone and must not be moved.
     */
    @Query("SELECT DISTINCT s.sequence FROM CuttingRequestSerieData s " +
           "WHERE s.sequence IN :sequences AND s.statusCoupe = 'Complete'")
    List<String> findSequenceIdsWithCompleteSeriesIn(@Param("sequences") List<String> sequences);

    /**
     * Rich live-charge projection — every column the dispatcher's live page
     * needs to compute remaining-time math, detect implicit zone locks
     * (statusCoupe != 'Waiting' + tableCoupe → STRICT zone), and render
     * full per-serie detail without an N+1 fetch.
     *
     * <p>Excludes Complete series — they contribute zero remaining minutes.
     * The dispatcher still needs to know a sequence has Complete series for
     * the "all complete → drop" filter, but that's tracked separately via
     * {@link #findSequenceIdsWithCompleteSeriesIn}.</p>
     *
     * <p>Columns:
     * 0=serie, 1=sequence, 2=machine, 3=tempsDeCoupe, 4=nbrCouche, 5=placement,
     * 6=statusCoupe, 7=statusMatelassage, 8=tableCoupe, 9=tableMatelassage,
     * 10=dateDebutCoupe, 11=dateFinCoupe, 12=dateDebutMatelassage, 13=dateFinMatelassage.</p>
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.tempsDeCoupe, s.nbrCouche, s.placement, " +
           "s.statusCoupe, s.statusMatelassage, s.tableCoupe, s.tableMatelassage, " +
           "s.dateDebutCoupe, s.dateFinCoupe, s.dateDebutMatelassage, s.dateFinMatelassage, " +
           "s.partNumberMaterial, s.longueur " +
           "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences " +
           "ORDER BY s.sequence, s.serie")
    List<Object[]> findLiveChargeSeriesBySequences(@Param("sequences") List<String> sequences);

    // ======================== INCREMENTAL CACHE QUERIES ========================

    /**
     * Load series whose date fields changed since the given timestamp.
     * Used by the incremental workbench cache to avoid full reloads.
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.tempsDeCoupe, s.nbrCouche, s.placement " +
           "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences AND " +
           "(s.dateDebutCoupe >= :since OR s.dateFinCoupe >= :since " +
           " OR s.dateDebutMatelassage >= :since OR s.dateFinMatelassage >= :since)")
    List<Object[]> findChangedSeriesBySequencesSince(@Param("sequences") List<String> sequences,
                                                      @Param("since") LocalDateTime since);

    /**
     * Load LIVE CHARGE projection for series whose date fields changed since.
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.tempsDeCoupe, s.nbrCouche, s.placement, " +
           "s.statusCoupe, s.statusMatelassage, s.tableCoupe, s.tableMatelassage, " +
           "s.dateDebutCoupe, s.dateFinCoupe, s.dateDebutMatelassage, s.dateFinMatelassage " +
           "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences AND " +
           "(s.dateDebutCoupe >= :since OR s.dateFinCoupe >= :since " +
           " OR s.dateDebutMatelassage >= :since OR s.dateFinMatelassage >= :since) " +
           "ORDER BY s.sequence, s.serie")
    List<Object[]> findChangedLiveChargeSeriesBySequencesSince(@Param("sequences") List<String> sequences,
                                                                @Param("since") LocalDateTime since);

    /**
     * Find sequences that have at least one serie changed since the given time.
     * Returns only the sequence IDs so the cache can decide which sequence blocks
     * need rebuilding.
     */
    @Query("SELECT DISTINCT s.sequence FROM CuttingRequestSerieData s WHERE " +
           "s.dateDebutCoupe >= :since OR s.dateFinCoupe >= :since " +
           "OR s.dateDebutMatelassage >= :since OR s.dateFinMatelassage >= :since")
    List<String> findSequencesWithChangesSince(@Param("since") LocalDateTime since);

    /**
     * Find new sequences whose sequence string is lexicographically greater than
     * the max sequence already cached. This catches newly imported sequences.
     */
    @Query("SELECT DISTINCT s.sequence FROM CuttingRequestSerieData s WHERE s.sequence > :maxSequence")
    List<String> findSequencesGreaterThan(@Param("maxSequence") String maxSequence);

    /**
     * Find new sequences whose sequence string is greater than max, with their series
     * (light projection). Used to append new sequences to the incremental cache.
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.tempsDeCoupe, s.nbrCouche, s.placement " +
           "FROM CuttingRequestSerieData s WHERE s.sequence > :maxSequence")
    List<Object[]> findNewSeriesGreaterThan(@Param("maxSequence") String maxSequence);

    /**
     * Find new sequences with live-charge projection.
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.tempsDeCoupe, s.nbrCouche, s.placement, " +
           "s.statusCoupe, s.statusMatelassage, s.tableCoupe, s.tableMatelassage, " +
           "s.dateDebutCoupe, s.dateFinCoupe, s.dateDebutMatelassage, s.dateFinMatelassage " +
           "FROM CuttingRequestSerieData s WHERE s.sequence > :maxSequence " +
           "ORDER BY s.sequence, s.serie")
    List<Object[]> findNewLiveChargeSeriesGreaterThan(@Param("maxSequence") String maxSequence);

    /**
     * Count how many sequences exist whose ID is greater than the given max.
     * Fast existence check before doing the heavy load.
     */
    @Query("SELECT COUNT(DISTINCT s.sequence) FROM CuttingRequestSerieData s WHERE s.sequence > :maxSequence")
    Long countSequencesGreaterThan(@Param("maxSequence") String maxSequence);

    /**
     * Load series whose zoneCoupe or zoneMatelassage matches the given zone.
     * Used by MaterialDemandForecastService to find series explicitly assigned
     * to a zone (including SHARED zone overflow and direct table assignments).
     */
    @Query("SELECT s.serie, s.sequence, s.machine, s.partNumberMaterial, s.description, s.longueur, " +
           "s.nbrCouche, s.placement, s.tempsDeCoupe, s.tableCoupe, s.tableMatelassage, " +
           "s.statusCoupe, s.statusMatelassage, s.dateDebutCoupe, s.dateFinCoupe, " +
           "s.dateDebutMatelassage, s.dateFinMatelassage, s.zoneCoupe, s.zoneMatelassage, s.planningDate " +
           "FROM CuttingRequestSerieData s WHERE s.zoneCoupe = :zoneNom OR s.zoneMatelassage = :zoneNom")
    List<Object[]> findSeriesByZoneCoupeOrMatelassage(@Param("zoneNom") String zoneNom);

    /**
     * Find sequences that have at least one incomplete serie (coupe or matelassage not Complete).
     * Used by computeShiftCompletion to avoid loading series for fully-completed sequences.
     */
    @Query("SELECT DISTINCT s.sequence FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences AND " +
           "(LOWER(s.statusCoupe) != 'complete' OR LOWER(s.statusMatelassage) != 'complete')")
    List<String> findIncompleteSequences(@Param("sequences") List<String> sequences);

    /**
     * Light projection for shift-completion computation.
     * Columns: 0=serie, 1=sequence, 2=statusCoupe, 3=statusMatelassage, 4=tempsDeCoupe,
     * 5=tableCoupe, 6=tableMatelassage, 7=dateDebutCoupe, 8=dateFinCoupe.
     */
    @Query("SELECT s.serie, s.sequence, s.statusCoupe, s.statusMatelassage, s.tempsDeCoupe, " +
           "s.tableCoupe, s.tableMatelassage, s.dateDebutCoupe, s.dateFinCoupe " +
           "FROM CuttingRequestSerieData s WHERE s.sequence IN :sequences " +
           "ORDER BY s.sequence, s.serie")
    List<Object[]> findShiftCompletionSeriesBySequences(@Param("sequences") List<String> sequences);

}
