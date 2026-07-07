package com.lear.MGCMS.repositories.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;

public interface CuttingRequestRepository extends CrudRepository<CuttingRequest, String> {

	@Modifying
	@Transactional
	@Query(value = "DELETE FROM CuttingRequestSerie WHERE cuttingRequest_sequence = :sequence ;" + 
			"DELETE FROM CuttingRequestPartNumber WHERE cuttingRequest_sequence = :sequence ;" + 
			"DELETE FROM CuttingRequestBox WHERE cuttingRequest_sequence = :sequence ;" + 
			"DELETE FROM CuttingRequest WHERE sequence = :sequence", nativeQuery = true)
	void deleteBySequence(String sequence);

	@Query("Select p from CuttingRequest p where (:date is null or p.planningDate = :date) and (:shift is null or p.shift = :shift)")
	List<CuttingRequest> findAll(LocalDate date, String shift);

	/**
	 * In-production sequences ({@code RELEASED, STARTED, MATERIAL_MISSING}) —
	 * the candidate set the auto-COMPLETED corrector scans. Pre-release
	 * ({@code IMPORTED}) and terminal ({@code COMPLETED}, {@code INCOMPLETE})
	 * sequences are excluded. Legacy {@code null} rows are still included so a
	 * pre-cutover row that finishes still gets flipped to {@code COMPLETED}.
	 */
	@Query("SELECT p.sequence FROM CuttingRequest p WHERE p.sequenceStatus IS NULL OR p.sequenceStatus IN ('RELEASED', 'STARTED', 'MATERIAL_MISSING')")
	List<String> findNonCompletedSequences();

	/**
	 * Distinct fabric references ({@code partNumberMaterial}) still needed by
	 * in-production sequences ({@code RELEASED, STARTED, MATERIAL_MISSING}, plus
	 * legacy null), restricted to series that are not yet {@code Complete}. No
	 * due-date filter — this is the BROAD demand set the logistics advisor uses
	 * to decide which rack rolls have no serie that needs them (RULE 3,
	 * return-to-magasin).
	 */
	@Query("SELECT DISTINCT s.partNumberMaterial FROM CuttingRequest p JOIN p.cuttingRequestSeries s "
	     + "WHERE (p.sequenceStatus IS NULL OR p.sequenceStatus IN ('RELEASED', 'STARTED', 'MATERIAL_MISSING')) "
	     + "  AND (s.statusCoupe IS NULL OR s.statusCoupe <> 'Complete') "
	     + "  AND s.partNumberMaterial IS NOT NULL")
	List<String> findInProductionMaterials();

	/**
	 * Lightweight projection for the dispatcher hot path.
	 * Returns only scalar fields — no eager collection loading.
	 * Columns: 0=sequence, 1=dispatchedZone, 2=zoneAcceptanceStatus, 3=pinnedByChef, 4=zoneNom, 5=dueDate
	 *
	 * Filters on dueDate/dueShift (not planningDate/shift) so the dispatcher
	 * matches the same "due this shift" semantic Plan de Charge uses; serie
	 * rows have planningDate null in production data.
	 */
	@Query("SELECT p.sequence, p.dispatchedZone, p.zoneAcceptanceStatus, p.pinnedByChef, p.zone.nom, p.dueDate " +
	       "FROM CuttingRequest p WHERE p.dueDate = :date AND p.dueShift = :shift")
	List<Object[]> findAllLight(@Param("date") LocalDate date, @Param("shift") String shift);

	/**
	 * In-production sequences — those whose {@code sequenceStatus} is in
	 * {@code RELEASED, STARTED, MATERIAL_MISSING} (or legacy null). Used by the
	 * real-time dispatcher that works on all open sequences regardless of
	 * due date/shift.
	 *
	 * <p>JPQL (not native) so the column name is resolved via the entity
	 * field mapping rather than hard-coded — the physical column on this
	 * project is {@code sequenceStatus} (camelCase) but a native query
	 * with {@code sequence_status} would assume snake_case and blow up
	 * with "Invalid column name" at runtime.</p>
	 *
	 * <p>Columns: 0=sequence, 1=dispatchedZone, 2=zoneAcceptanceStatus, 3=pinnedByChef, 4=zoneNom, 5=dueDate</p>
	 */
	@Query("SELECT p.sequence, p.dispatchedZone, p.zoneAcceptanceStatus, p.pinnedByChef, p.zone.nom, p.dueDate " +
	       "FROM CuttingRequest p WHERE (p.sequenceStatus IS NULL OR p.sequenceStatus IN ('RELEASED', 'STARTED', 'MATERIAL_MISSING'))")
	List<Object[]> findAllActiveLight();

	/** Batch {@code (sequence, sequenceStatus)} lookup for floor / chef views. */
	@Query("SELECT p.sequence, p.sequenceStatus FROM CuttingRequest p WHERE p.sequence IN :sequences")
	List<Object[]> findStatusBySequences(@Param("sequences") List<String> sequences);

	/**
	 * In-production sequences ({@code RELEASED, STARTED, MATERIAL_MISSING}, plus
	 * legacy null) that are due in the current plant bucket or an older bucket.
	 * Upcoming shifts are intentionally excluded so the workbench and public
	 * next-series queue don't pull tomorrow / next-shift demand before current
	 * and overdue boxes are finished.
	 *
	 * <p>Columns: 0=sequence, 1=dispatchedZone, 2=zoneAcceptanceStatus,
	 * 3=pinnedByChef, 4=zoneNom, 5=dueDate, 6=dueShift, 7=releaseZone,
	 * 8=sequenceStatus.</p>
	 */
	@Query("SELECT p.sequence, p.dispatchedZone, p.zoneAcceptanceStatus, p.pinnedByChef, p.zone.nom, p.dueDate, p.dueShift, p.releaseZone, p.sequenceStatus " +
	       "FROM CuttingRequest p " +
	       "WHERE p.sequenceStatus IN ('RELEASED', 'STARTED') " +
	       "  AND p.dueDate IS NOT NULL " +
	       "  AND p.dueShift IS NOT NULL " +
	       "  AND (p.dueDate < :date " +
	       "       OR (p.dueDate = :date AND ( " +
	       "           (:shift = '1' AND p.dueShift = '1') " +
	       "        OR (:shift = '2' AND p.dueShift IN ('1', '2')) " +
	       "        OR (:shift = '3' AND p.dueShift IN ('1', '2', '3')) " +
	       "       ))) " +
	       "ORDER BY p.dueDate ASC, p.dueShift ASC, p.sequence ASC")
	List<Object[]> findActiveDueOnOrBeforeLight(@Param("date") LocalDate date,
	                                            @Param("shift") String shift);

	@Query("SELECT p.sequence, p.dispatchedZone, p.zoneAcceptanceStatus, p.pinnedByChef, p.zone.nom, p.dueDate " +
	       "FROM CuttingRequest p WHERE p.sequence IN (:sequences)")
	List<Object[]> findBySequencesLight(List<String> sequences);

	/**
	 * (sequence, dispatchedAt, createdAt) for a batch of sequences — the release-age proxy
	 * for the next-serie wait-age (FIFO) tier. dispatchedAt is when the sequence was
	 * published/forced to a zone; createdAt is the import-time fallback for never-dispatched
	 * rows. Kept SEPARATE from findActiveDueOnOrBeforeLight (8 consumers) so that hot shared
	 * projection is untouched.
	 */
	@Query("SELECT p.sequence, p.dispatchedAt, p.createdAt FROM CuttingRequest p WHERE p.sequence IN (:sequences)")
	List<Object[]> findReleaseProxyBySequences(@Param("sequences") List<String> sequences);

	CuttingRequest findBySequence(String sequence);

	// ===== Phase 7 zone-aware engine =====

	/**
	 * CuttingRequests whose chef-de-zone hasn't rejected them and that have
	 * been routed to {@code zoneNom} for the given (date, shift). Used by
	 * the engine when {@code mgcms.engine.zoneAware=true} to scope its
	 * view to one zone at a time.
	 */
	@Query("SELECT p FROM CuttingRequest p "
	     + "WHERE p.planningDate = :date AND p.shift = :shift "
	     + "  AND p.dispatchedZone = :zoneNom "
	     + "  AND (p.zoneAcceptanceStatus IS NULL OR p.zoneAcceptanceStatus <> 'REJECTED')")
	List<CuttingRequest> findAcceptedByZone(LocalDate date, String shift, String zoneNom);

	/**
	 * Pending (dispatched but not yet accepted/rejected) requests — chef-de-zone
	 * inbox for Phase 10's live page.
	 */
	@Query("SELECT p FROM CuttingRequest p "
	     + "WHERE p.planningDate = :date AND p.shift = :shift "
	     + "  AND p.dispatchedZone = :zoneNom "
	     + "  AND p.zoneAcceptanceStatus = 'PENDING'")
	List<CuttingRequest> findPendingByZone(LocalDate date, String shift, String zoneNom);

	/**
	 * Phase 11 self-heal — sequences that have been sitting in PENDING
	 * (i.e. dispatched but not yet ACCEPTED or REJECTED by the chef-de-zone)
	 * for longer than {@code cutoff}. Filters on {@code dispatchedAt}
	 * (the timestamp the dispatcher stamped on publish/force/rebalance),
	 * not {@code createdAt}, so a brand-new request that was just dispatched
	 * isn't flagged as stuck and an old request that was re-dispatched a
	 * minute ago isn't either.
	 *
	 * <p>Both filters are pushed into the DB so we never load the whole
	 * cutting_request table into memory (review C2).</p>
	 */
	@Query("SELECT p FROM CuttingRequest p "
	     + "WHERE p.zoneAcceptanceStatus = 'PENDING' "
	     + "  AND p.dispatchedAt IS NOT NULL "
	     + "  AND p.dispatchedAt < :cutoff")
	List<CuttingRequest> findStuckPending(LocalDateTime cutoff);

	/**
	 * Batch-load dispatchedZone for a set of sequences.
	 * Used by OrdonnancementService to respect dispatcher zone assignments
	 * when assigning series to physical machines.
	 */
	@Query("SELECT p.sequence, p.dispatchedZone FROM CuttingRequest p WHERE p.sequence IN :sequences")
	List<Object[]> findDispatchedZonesBySequences(@Param("sequences") List<String> sequences);

	/**
	 * IMPORTED (not-yet-released) sequences due in the current or an older bucket —
	 * the candidate set for the logistics release picklist. Mirrors the due-window
	 * of {@link #findActiveDueOnOrBeforeLight} but scoped to {@code sequenceStatus = 'IMPORTED'}.
	 * Uses a LEFT JOIN so an imported sequence with no preferred zone still appears.
	 * Columns: 0=sequence, 1=dispatchedZone, 2=zoneNom, 3=dueDate, 4=dueShift, 5=releaseZone.
	 */
	@Query("SELECT p.sequence, p.dispatchedZone, z.nom, p.dueDate, p.dueShift, p.releaseZone " +
	       "FROM CuttingRequest p LEFT JOIN p.zone z " +
	       "WHERE p.sequenceStatus = 'IMPORTED' " +
	       "  AND p.dueDate IS NOT NULL " +
	       "  AND p.dueShift IS NOT NULL " +
	       "  AND (p.dueDate < :date " +
	       "       OR (p.dueDate = :date AND ( " +
	       "           (:shift = '1' AND p.dueShift = '1') " +
	       "        OR (:shift = '2' AND p.dueShift IN ('1', '2')) " +
	       "        OR (:shift = '3' AND p.dueShift IN ('1', '2', '3')) " +
	       "       ))) " +
	       "ORDER BY p.dueDate ASC, p.dueShift ASC, p.sequence ASC")
	List<Object[]> findImportedDueOnOrBeforeLight(@Param("date") LocalDate date,
	                                              @Param("shift") String shift);
}
