package com.lear.MGCMS.repositories.CuttingRequest;

import java.time.LocalDate;
import java.util.List;

import javax.transaction.Transactional;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestV2;

public interface CuttingRequestV2Repository extends CrudRepository<CuttingRequestV2, String> {

	void deleteBySequence(String sequence);

	@Query("Select p from CuttingRequestV2 p where (:date is null or p.planningDate = :date) and (:shift is null or p.shift = :shift)")
	List<CuttingRequestV2> findAll(LocalDate date, String shift);

	CuttingRequestV2 findBySequence(String sequence);
	@Modifying
	@Transactional
	@Query(value = "DELETE FROM CuttingRequestSerie WHERE cuttingRequest_sequence = :sequence and serie not in (:goodSeriesArr) ", nativeQuery = true)
	void deleteSeriesOther(String sequence, List<String> goodSeriesArr);


}
