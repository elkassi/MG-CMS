package com.lear.MGCMS.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.lear.MGCMS.domain.Intervention;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.Intervention;

public interface InterventionRepository  extends JpaRepository<Intervention, String>, JpaSpecificationExecutor<Intervention>{

	@Query("SELECT t from Intervention t where t.id like :id and (:of is not null)")
	Page<Intervention> findByFilter(String id, PageRequest of);
	@Query("SELECT t from Intervention t where t.id = :id")
	Intervention findByObjId(String id);
	@Query("SELECT t from Intervention t where debutArret <= :date2 and (finIntervention is null or finIntervention >= :date1) and machine = :machineName and type='coupe'")
	List<Intervention> findBetween(LocalDateTime date1, LocalDateTime date2, String machineName);
	List<Intervention> findBySerie(String serie);
	@Query("SELECT t from Intervention t where t.validerPar is null and t.departement not like 'Production%' order by t.departement, t.createdAt")
	List<Intervention> findNonValider();

	@Query("select cr from Intervention cr where cr.id in (:arr)")
	List<Intervention> getList(List<String> arr);

	@Query("SELECT t FROM Intervention t WHERE t.departement = 'Maintenance' " +
		   "AND (:startDate IS NULL OR t.date >= :startDate) " +
		   "AND (:endDate IS NULL OR t.date <= :endDate) " +
		   "AND (:machine IS NULL OR :machine = '' OR t.machine = :machine) " +
		   "ORDER BY t.createdAt DESC")
	List<Intervention> findMaintenanceKpi(
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate, 
		@Param("machine") String machine
	);
}
