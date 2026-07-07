package com.lear.MGCMS.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.CoupeMachineHistory;
import com.lear.MGCMS.domain.CoupeMachineHistoryId;
import com.lear.MGCMS.domain.Placement;
import com.lear.MGCMS.domain.PlacementId;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieInfo;

public interface CoupeMachineHistoryRepository extends JpaRepository<CoupeMachineHistory, CoupeMachineHistoryId>, JpaSpecificationExecutor<CoupeMachineHistory> {

	@Query("Select crs from CoupeMachineHistory crs "
			+ " where crs.lineDate <= :endDate and crs.lineDate >= :startDate and crs.machine in :machines order by lineDate")
	List<CoupeMachineHistory> findBetween(LocalDateTime startDate, LocalDateTime endDate, List<String> machines);

}
