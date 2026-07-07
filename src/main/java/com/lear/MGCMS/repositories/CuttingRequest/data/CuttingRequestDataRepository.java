package com.lear.MGCMS.repositories.CuttingRequest.data;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;

import java.time.LocalDate;
import java.util.List;

public interface CuttingRequestDataRepository extends JpaRepository<CuttingRequestData, String>, JpaSpecificationExecutor<CuttingRequestData> {

    CuttingRequestData findBySequence(String sequence);

    @Query("Select p from CuttingRequestData p where (:date is null or p.planningDate = :date) and (:shift is null or p.shift = :shift)")
    List<CuttingRequestData> findList(LocalDate date, String shift);

    @Query("SELECT c FROM CuttingRequestData c WHERE c.sequence IN (:sequences)")
    List<CuttingRequestData> findBySequences(List<String> sequences);

    /**
     * Light query: only sequence, zone name, dueDate, dueShift. No User/Zone entity loading.
     */
    @Query("SELECT c.sequence, z.nom, c.dueDate, c.dueShift FROM CuttingRequestData c LEFT JOIN c.zone z WHERE c.sequence IN :sequences")
    List<Object[]> findSequenceInfoLight(@Param("sequences") List<String> sequences);

    /**
     * Find sequences due for current shift, previous shift, or the shift before previous.
     * dueShift is numeric: 1 (06:00-14:00), 2 (14:00-22:00), 3 (22:00-06:00).
     * Only returns sequences where ALL series are still waiting (not yet started).
     */
    @Query("SELECT DISTINCT s.sequence FROM CuttingRequestSerieData s " +
           "JOIN CuttingRequestData c ON s.sequence = c.sequence " +
           "WHERE (s.statusCoupe = 'Waiting' AND s.statusMatelassage = 'Waiting') " +
           "AND ((c.dueDate = :date1 AND c.dueShift = :shift1) " +
           "OR (c.dueDate = :date2 AND c.dueShift = :shift2) " +
           "OR (c.dueDate = :date3 AND c.dueShift = :shift3))")
    List<String> findSequencesByDueShifts(@Param("date1") LocalDate date1, @Param("shift1") String shift1,
                                         @Param("date2") LocalDate date2, @Param("shift2") String shift2,
                                         @Param("date3") LocalDate date3, @Param("shift3") String shift3);

    /**
     * Latest due-date / due-shift buckets present in the dataset.
     * Used as a fallback when a local historical copy has no rows around the wall clock.
     */
    @Query("SELECT c.dueDate, c.dueShift FROM CuttingRequestData c " +
           "WHERE c.dueDate IS NOT NULL AND c.dueShift IS NOT NULL " +
           "GROUP BY c.dueDate, c.dueShift ORDER BY c.dueDate DESC, c.dueShift DESC")
    List<Object[]> findLatestDueShiftBuckets(Pageable pageable);

    /**
     * Scalar projection feeding the chef rectification screen: every sequence
     * planned since {@code cutoff}, newest first, whatever its status.
     * Columns: 0=sequence, 1=sequenceStatus, 2=releaseZone, 3=projet,
     * 4=planningDate, 5=shift, 6=dueDate, 7=dueShift, 8=releaseZoneSource.
     */
    @Query("SELECT c.sequence, c.sequenceStatus, c.releaseZone, c.projet, " +
           "c.planningDate, c.shift, c.dueDate, c.dueShift, c.releaseZoneSource " +
           "FROM CuttingRequestData c WHERE c.planningDate >= :cutoff " +
           "ORDER BY c.planningDate DESC, c.sequence")
    List<Object[]> findRectificationRows(@Param("cutoff") LocalDate cutoff);

    /**
     * Candidates for the zone auto-correction job: in-flight or finished work
     * ({@code STARTED} / {@code COMPLETED}) planned since {@code cutoff}.
     * Columns: 0=sequence, 1=releaseZone, 2=releaseZoneSource.
     */
    @Query("SELECT c.sequence, c.releaseZone, c.releaseZoneSource FROM CuttingRequestData c " +
           "WHERE c.sequenceStatus IN ('STARTED', 'COMPLETED') AND c.planningDate >= :cutoff")
    List<Object[]> findZoneAutofixCandidates(@Param("cutoff") LocalDate cutoff);

    /**
     * Apply an inferred zone. The WHERE guard re-checks the lock at write time:
     * a zone fixed by the logistics release ({@code LOGISTICS}) or set manually
     * by a chef ({@code CHEF}) is never overwritten, even if it was written
     * between the candidate read and this update.
     *
     * @return 1 when applied, 0 when the row was locked meanwhile
     */
    @Modifying
    @Transactional
    @Query("UPDATE CuttingRequestData c SET c.releaseZone = :zone, c.releaseZoneSource = 'AUTO' " +
           "WHERE c.sequence = :sequence " +
           "AND (c.releaseZoneSource IS NULL OR c.releaseZoneSource = 'AUTO')")
    int applyAutoZone(@Param("sequence") String sequence, @Param("zone") String zone);

    /**
     * Reconcile sequenceStatus for a batch of sequences to a single {@code target}
     * value, driven by the suiviplanning status sync. Only rows that actually
     * differ are touched, and {@code MATERIAL_MISSING} / {@code INCOMPLETE} are
     * never overwritten — those are MG-CMS-internal exception states that
     * suiviplanning has no concept of.
     *
     * @return number of rows updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE CuttingRequestData c SET c.sequenceStatus = :target " +
           "WHERE c.sequence IN :sequences " +
           "AND (c.sequenceStatus IS NULL OR " +
           "     (c.sequenceStatus <> :target AND c.sequenceStatus NOT IN ('MATERIAL_MISSING', 'INCOMPLETE')))")
    int reconcileSequenceStatus(@Param("target") String target, @Param("sequences") List<String> sequences);
}
