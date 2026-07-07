package com.lear.MGCMS.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.PlanningDetails;
import com.lear.MGCMS.domain.PlanningId;
import com.lear.MGCMS.payload.StatsInfo2;
import com.lear.MGCMS.payload.StatsInfoDTO;

public interface PlanningDetailsRepository extends CrudRepository<PlanningDetails, PlanningId> {

	@Query("select new com.lear.MGCMS.payload.StatsInfoDTO(pd.groupName, count(pd), sum(pd.quantity)) from PlanningDetails pd "
	        + "where pd.planningDate = :planningDate and pd.shift = :shift and (pd.sequenceCoupe is null and pd.color = 'FFFFFFFF') "
	        + "group by pd.groupName")
	List<StatsInfoDTO> getStatNotImported(LocalDate planningDate, String shift);
	
	//pd.sequenceCoupe is not null
	@Query("select new com.lear.MGCMS.payload.StatsInfoDTO(pd.groupName, count(pd), sum(pd.quantity)) from PlanningDetails pd "
	        + "where pd.planningDate = :planningDate and pd.shift = :shift and (pd.sequenceCoupe is not null or pd.color != 'FFFFFFFF') "
	        + "group by pd.groupName")
	List<StatsInfoDTO> getStatImported(LocalDate planningDate, String shift);
	
	@Query("select new com.lear.MGCMS.payload.StatsInfoDTO(pd.color, count(pd)) from PlanningDetails pd "
	        + "where pd.planningDate = :planningDate and pd.shift = :shift "
	        + "group by pd.color")
	List<StatsInfoDTO> getStat(LocalDate planningDate, String shift);

	
}
