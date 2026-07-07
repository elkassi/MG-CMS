package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.MachineLsrRapport;
import com.lear.MGCMS.domain.MachineLsrRapportId;
import com.lear.MGCMS.domain.MaintenanceInterventionConfig;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MachineLsrRapportRepository extends CrudRepository<MachineLsrRapport, MachineLsrRapportId>, JpaSpecificationExecutor<MachineLsrRapport> {

	@Query("Select max(date) from MachineLsrRapport where machine = :machine")
	LocalDateTime findMaxDateByMachine(String machine);

	@Query("Select crs from MachineLsrRapport crs "
			+ " where crs.date <= :endDate and crs.date >= :startDate and crs.machine = :machine order by date")
	List<MachineLsrRapport> findBetween(LocalDateTime startDate, LocalDateTime endDate, String machine);
	
}
