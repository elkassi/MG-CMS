package com.lear.MGCMS.repositories.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieLight;

public interface CuttingRequestSerieLightRepository extends CrudRepository<CuttingRequestSerieLight, String> {
	
	@Query("Select p from CuttingRequestSerieLight p where (:date is null or p.planningDate = :date) and (:shift is null or p.shift = :shift)")
	List<CuttingRequestSerieLight> findAll(LocalDate date, String shift);
	
	@Query("Select crs from CuttingRequestSerieLight crs "
			+ " where crs.dateDebutCoupe <= :date2 and crs.dateFinCoupe >= :date1 and crs.tableCoupe = :machine")
	List<CuttingRequestSerieLight> findBetween(LocalDateTime date1, LocalDateTime date2, String machine);

	CuttingRequestSerieLight findBySerie(String serie);
	@Query("Select crs from CuttingRequestSerieLight crs "
			+ " where crs.dateDebutCoupe is null and crs.dateFinCoupe is null order by crs.dateDebutCoupe")
	List<CuttingRequestSerieLight> findAllNotYet();
	
	@Query("Select crs from CuttingRequestSerieLight crs "
			+ " where crs.dateDebutCoupe is not null and crs.dateFinCoupe is null order by crs.dateDebutCoupe")
	List<CuttingRequestSerieLight> findAllInProgress();
	@Query("Select crs from CuttingRequestSerieLight crs "
			+ "where ((crs.dateDebutCoupe != null and crs.dateDebutCoupe < :date2 and crs.dateDebutCoupe >= :date1) "
			+ "or (crs.dateFinCoupe != null and crs.dateFinCoupe < :date2 and crs.dateFinCoupe >= :date1)) "
			+ " and crs.tableCoupe in (:machines)")
	List<CuttingRequestSerieLight> getHistorique(LocalDateTime date1, LocalDateTime date2, List<String> machines);
	
}
