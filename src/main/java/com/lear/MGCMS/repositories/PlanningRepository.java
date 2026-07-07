package com.lear.MGCMS.repositories;

import java.time.LocalDate;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.Planning;
import com.lear.MGCMS.domain.PlanningDetails;
import com.lear.MGCMS.domain.PlanningId;
import com.lear.MGCMS.payload.StatsInfo2;
import com.lear.MGCMS.payload.StatsInfoDTO;

public interface PlanningRepository extends CrudRepository<Planning, PlanningId> {

	@Query("Select p from PlanningDetails p where p.planningDate = :date and p.shift = :shift")
	List<PlanningDetails> findAll(LocalDate date, String shift);
	
	@Query("Select p from PlanningDetails p where p.planningDate = :date and p.shift = :shift and p.sequenceCoupe is null")
	List<PlanningDetails> findAllWaiting(LocalDate date, String shift);

	@Modifying
	@Transactional
	@Query("delete from PlanningDetails pd where pd in :arrDB")
	void deleteAll(List<PlanningDetails> arrDB);

	@Query(value = "SELECT TOP (100000) * " + 
			"FROM Planning as pl  " + 
			"WHERE (color is null or color = 'FFFFFFFF') " + 
			"  AND NOT EXISTS ( " + 
			"    SELECT * FROM [MATNR-APP12].[plt_viewer].[dbo].[files] as f  " + 
			"    WHERE f.[part_number_cover] = pl.partNumber " + 
			"  ) " + 
			"  AND planningDate >= DATEADD(DAY, DATEDIFF(DAY, 0, GETDATE()) - 3, 0) " + 
			"ORDER BY planningDate DESC, shift DESC", nativeQuery = true)
	List<Planning> findNotFoundCtc();

	Planning findFirstByItem(String item);
	
//	@Query("select com.lear.MGCMS.payload.StatsInfo2(pd.groupName, count(pd), sum(pd.quantity)) from PlanningDetails pd "
//			+ "where pd.planningDate = :planningDate and pd.shift = :shift "
//			+ "group by pd.groupName")
//	List<StatsInfo2> getStat(LocalDate planningDate, String shift);


	
}
