package com.lear.MGCMS.repositories.CuttingRequest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestBoxInfo;

public interface CuttingRequestBoxInfoRepository extends CrudRepository<CuttingRequestBoxInfo, String> {

	@Query("Select p from CuttingRequestBoxInfo p where (:date is null or p.cuttingRequest.planningDate = :date) and (:shift is null or p.cuttingRequest.shift = :shift)")
	List<CuttingRequestBoxInfo> findAll(LocalDate date, String shift);

	/**
	 * Lightweight projection for workbench cache hot path.
	 * Avoids entity instantiation and CuttingRequest join overhead.
	 * Uses JPQL to stay portable — selects scalars only.
	 * Columns: 0=id, 1=sequence, 2=partNumber, 3=description, 4=item, 5=wo, 6=woid, 7=qtyBox, 8=gammePrinted
	 */
	@Query("SELECT b.id, cr.sequence, b.partNumber, b.description, b.item, b.wo, b.woid, b.qtyBox, b.gammePrinted " +
			"FROM CuttingRequestBoxInfo b LEFT JOIN b.cuttingRequest cr " +
			"WHERE (:date IS NULL OR cr.planningDate = :date) AND (:shift IS NULL OR cr.shift = :shift)")
	List<Object[]> findAllLight(LocalDate date, String shift);

	/**
	 * Count boxes per sequence for a given list of sequences.
	 * Columns: 0=sequence, 1=boxCount
	 */
	@Query("SELECT cr.sequence, COUNT(b.id) " +
			"FROM CuttingRequestBoxInfo b JOIN b.cuttingRequest cr " +
			"WHERE cr.sequence IN :sequences " +
			"GROUP BY cr.sequence")
	List<Object[]> countBoxesBySequences(@org.springframework.data.repository.query.Param("sequences") List<String> sequences);

}
