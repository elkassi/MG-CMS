package com.lear.MGCMS.repositories.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;
import com.lear.MGCMS.payload.LectraStats;

public interface CuttingRequestSerieInfoRepository extends CrudRepository<CuttingRequestSerieInfo, String> {
	
	@Query("Select p from CuttingRequestSerieInfo p where (:date is null or p.planningDate = :date) and (:shift is null or p.shift = :shift)")
	List<CuttingRequestSerieInfo> findAll(LocalDate date, String shift);
	
	@Query("Select crs from CuttingRequestSerieInfo crs "
			+ " where crs.dateDebutCoupe <= :date2 and crs.dateFinCoupe >= :date1 and (:machine is null or crs.tableCoupe = :machine)")
	List<CuttingRequestSerieInfo> findBetween(LocalDateTime date1, LocalDateTime date2, String machine);
	@Query("Select crs from CuttingRequestSerieInfo crs "
			+ " where crs.dateDebutMatelassage <= :date2 and crs.dateFinMatelassage >= :date1 and (:machine is null or crs.tableMatelassage = :machine)")
	List<CuttingRequestSerieInfo> findBetween2(LocalDateTime date1, LocalDateTime date2, String machine);

//	@Query("Select new com.lear.MGCMS.payload.LectraStats(crs.tableCoupe, count(*), sum(crs.longueur)) from CuttingRequestSerieInfo as crs "
//			+ " where crs.dateDebutCoupe <= :date2 and crs.dateFinCoupe >= :date1 "
//			+ "Group by crs.tableCoupe")
//	List<LectraStats> findStats(LocalDateTime date1, LocalDateTime date2);
	@Query("Select crs.statusCoupe from CuttingRequestSerieInfo crs where crs.cuttingRequest.sequence = :sequence")
	List<String> getStatusCoupeBySequence(String sequence);

	@Query("Select crs.statusMatelassage from CuttingRequestSerieInfo crs where crs.cuttingRequest.sequence = :sequence")
	List<String> getStatusMatelassageBySequence(String sequence);


	CuttingRequestSerieInfo findBySerie(String serie);
	@Query("Select crs from CuttingRequestSerieInfo crs "
			+ " where crs.dateDebutCoupe is null and crs.dateFinCoupe is null order by crs.dateDebutCoupe")
	List<CuttingRequestSerieInfo> findAllNotYet();
	
	@Query("Select crs from CuttingRequestSerieInfo crs "
			+ " where crs.dateDebutCoupe is not null and crs.dateFinCoupe is null order by crs.dateDebutCoupe")
	List<CuttingRequestSerieInfo> findAllInProgress();
	@Query("Select crs from CuttingRequestSerieInfo crs "
			+ "where (crs.dateDebutCoupe != null and crs.dateDebutCoupe < :date2 and crs.dateDebutCoupe >= :date1) or (crs.dateFinCoupe != null and crs.dateFinCoupe < :date2 and crs.dateFinCoupe >= :date1)")
	List<CuttingRequestSerieInfo> getHistorique(LocalDateTime date1, LocalDateTime date2);

	@Query("Select crs from CuttingRequestSerieInfo crs where crs.serie in (:series)")
	List<CuttingRequestSerieInfo> findSeries(List<String> series);

	@Query("Select crs.tableCoupe from CuttingRequestSerieInfo crs where crs.cuttingRequest.sequence = :sequence and crs.partNumberMaterial in (:reftissuList) group by crs.tableCoupe")
    List<String> findMachines(String sequence, List<String> reftissuList);
    
    /**
     * Get series with placement and machine for a specific date range and shift - lightweight for Plan de Charge.
     * Uses cuttingRequest.dueDate/dueShift (the planning target date, not the serie-level planningDate).
     * Returns: serie, placement, tableCoupe (machine), tempsDeCoupe
     */
    @Query("SELECT crs.serie, crs.placement, crs.tableCoupe, crs.tempsDeCoupe " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate = :date AND crs.cuttingRequest.dueShift = :shift " +
           "AND crs.placement IS NOT NULL AND crs.tableCoupe IS NOT NULL")
    List<Object[]> findSeriesForPlanDeCharge(LocalDate date, String shift);
    
    /**
     * Get only distinct placements for a date range - very lightweight for batch TimingModel lookup.
     * Uses cuttingRequest.dueDate for consistency with other Plan de Charge queries.
     */
    @Query("SELECT DISTINCT crs.placement FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate BETWEEN :startDate AND :endDate " +
           "AND crs.placement IS NOT NULL")
    List<String> findDistinctPlacementsByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get series with placement, machine and cutting time for a date range - optimized for Plan de Charge.
     * Uses cuttingRequest.dueDate/dueShift for consistency.
     */
    @Query("SELECT crs.serie, crs.placement, crs.tableCoupe, crs.tempsDeCoupe, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate BETWEEN :startDate AND :endDate " +
           "AND crs.tableCoupe IS NOT NULL " +
           "ORDER BY crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift")
    List<Object[]> findSeriesForPlanDeChargeByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get aggregated cutting time by machine, date and shift - most optimized for load calculation.
     * Uses cuttingRequest.dueDate/dueShift so series planned for a shift appear correctly.
     * Returns both SUM(tempsDeCoupe) and SUM(tempsDeCoupe * nbrCouche) so the service layer
     * can pick the correct value based on machine type (LASER-DXF uses nbrCouche, others don't).
     * Returns: tableCoupe, dueDate, dueShift, SUM(tempsDeCoupe), SUM(tempsDeCoupe * COALESCE(nbrCouche, 1))
     */
    @Query("SELECT crs.tableCoupe, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, " +
           "SUM(crs.tempsDeCoupe), SUM(crs.tempsDeCoupe * COALESCE(crs.nbrCouche, 1)) " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate BETWEEN :startDate AND :endDate " +
           "AND crs.tableCoupe IS NOT NULL " +
           "GROUP BY crs.tableCoupe, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift")
    List<Object[]> findAggregatedCuttingTimeByMachineAndShiftWithNbrCouche(LocalDate startDate, LocalDate endDate);

    /**
     * Get aggregated cutting time by machine, date and shift (original, without nbrCouche factoring).
     * Returns: tableCoupe, dueDate, dueShift, SUM(tempsDeCoupe)
     */
    @Query("SELECT crs.tableCoupe, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, SUM(crs.tempsDeCoupe) " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate BETWEEN :startDate AND :endDate " +
           "AND crs.tableCoupe IS NOT NULL " +
           "GROUP BY crs.tableCoupe, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift")
    List<Object[]> findAggregatedCuttingTimeByMachineAndShift(LocalDate startDate, LocalDate endDate);

    /**
     * Get aggregated cutting time split by completion status for retard calculation.
     * Uses cuttingRequest.dueDate/dueShift so series planned for a shift appear correctly.
     * Returns both SUM(tempsDeCoupe) and SUM(tempsDeCoupe * nbrCouche) so the service layer
     * can pick the correct value based on machine type (LASER-DXF uses nbrCouche, others don't).
     * Returns: tableCoupe, dueDate, dueShift, SUM(tempsDeCoupe), SUM(tempsDeCoupe * COALESCE(nbrCouche, 1)), isCut (1=dateFinCoupe not null, 0=not cut)
     */
    @Query("SELECT crs.tableCoupe, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, " +
           "SUM(crs.tempsDeCoupe), SUM(crs.tempsDeCoupe * COALESCE(crs.nbrCouche, 1)), " +
           "CASE WHEN crs.dateFinCoupe IS NOT NULL THEN 1 ELSE 0 END as isCut " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate BETWEEN :startDate AND :endDate " +
           "AND crs.tableCoupe IS NOT NULL " +
           "GROUP BY crs.tableCoupe, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, " +
           "CASE WHEN crs.dateFinCoupe IS NOT NULL THEN 1 ELSE 0 END")
    List<Object[]> findAggregatedCuttingTimeWithStatus(LocalDate startDate, LocalDate endDate);

    /**
     * Get per-serie cutting time with timestamps for detailed retard calculation.
     * Returns individual series (not aggregated) so that partial retard can be computed
     * based on dateDebutCoupe/dateFinCoupe relative to shift boundaries.
     * Returns raw tempsDeCoupe and nbrCouche; the service layer multiplies for LASER-DXF machines.
     * Returns: tableCoupe, dueDate, dueShift, tempsDeCoupe, nbrCouche, dateDebutCoupe, dateFinCoupe
     */
    @Query("SELECT crs.tableCoupe, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, " +
           "crs.tempsDeCoupe, crs.nbrCouche, crs.dateDebutCoupe, crs.dateFinCoupe " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate BETWEEN :startDate AND :endDate " +
           "AND crs.tableCoupe IS NOT NULL")
    List<Object[]> findSeriesWithTimestampsForRetard(LocalDate startDate, LocalDate endDate);

    /**
     * Get per-serie data with machine, placement, timestamps for Plan de Charge aggregation.
     * Uses machine field (not tableCoupe) and includes all series (even with null tableCoupe).
     * Returns: machine, placement, dueDate, dueShift, tempsDeCoupe, nbrCouche, dateDebutCoupe, dateFinCoupe
     */
    @Query("SELECT crs.machine, crs.placement, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, " +
           "crs.tempsDeCoupe, crs.nbrCouche, crs.dateDebutCoupe, crs.dateFinCoupe " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate BETWEEN :startDate AND :endDate " +
           "AND crs.machine IS NOT NULL")
    List<Object[]> findSeriesForAggregation(LocalDate startDate, LocalDate endDate);

    /**
     * Get detailed series for a specific date and shift for Plan de Charge calculation.
     * Returns: serie(0), sequence(1), dueDate(2), dueShift(3), machine(4), placement(5), partNumberMaterial(6),
     *          description(7), dateDebutMatelassage(8), dateFinMatelassage(9), statusMatelassage(10), tableMatelassage(11),
     *          dateDebutCoupe(12), dateFinCoupe(13), statusCoupe(14), tableCoupe(15), tempsDeCoupe(16), nbrCouche(17),
     *          cuttingPlanId(18)
     */
    @Query("SELECT crs.serie, crs.cuttingRequest.sequence, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, " +
           "crs.machine, crs.placement, crs.partNumberMaterial, crs.description, " +
           "crs.dateDebutMatelassage, crs.dateFinMatelassage, crs.statusMatelassage, crs.tableMatelassage, " +
           "crs.dateDebutCoupe, crs.dateFinCoupe, crs.statusCoupe, crs.tableCoupe, crs.tempsDeCoupe, crs.nbrCouche, " +
           "crs.cuttingRequest.cuttingPlanId " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate = :dueDate AND crs.cuttingRequest.dueShift = :dueShift " +
           "ORDER BY crs.serie")
    List<Object[]> findDetailedSeriesForShift(LocalDate dueDate, String dueShift);

    /**
     * Get series for a date range to check what was planned vs cut.
     * Columns: serie(0), sequence(1), dueDate(2), dueShift(3), machine(4), placement(5),
     *          tableCoupe(6), tempsDeCoupe(7), dateDebutCoupe(8), dateFinCoupe(9), statusCoupe(10), nbrCouche(11), cuttingPlanId(12)
     */
    @Query("SELECT crs.serie, crs.cuttingRequest.sequence, crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, " +
           "crs.machine, crs.placement, crs.tableCoupe, crs.tempsDeCoupe, " +
           "crs.dateDebutCoupe, crs.dateFinCoupe, crs.statusCoupe, crs.nbrCouche, " +
           "crs.cuttingRequest.cuttingPlanId " +
           "FROM CuttingRequestSerieInfo crs " +
           "WHERE crs.cuttingRequest.dueDate BETWEEN :startDate AND :endDate " +
           "ORDER BY crs.cuttingRequest.dueDate, crs.cuttingRequest.dueShift, crs.serie")
    List<Object[]> findSeriesForDateRange(LocalDate startDate, LocalDate endDate);
}
