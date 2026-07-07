package com.lear.cms.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.PlanCoupe;

import java.time.LocalDate;
import java.util.List;

public interface PlanCoupeRepository extends CrudRepository<PlanCoupe, Long> {

	@Query("select max(idPlanCoupe) from PlanCoupe pc")
	Long findMaxId();
	// dateCreatedPlanCoupe , Date_Modified_PlanCoupe , dateEnabledPlanCoupe , dateDisabledPlanCoupe
	@Query("from PlanCoupe pc where (pc.dateCreatedPlanCoupe >= :date1 and pc.dateCreatedPlanCoupe <= :date2) " +
			"or (pc.dateModifiedPlanCoupe >= :date1 and pc.dateModifiedPlanCoupe <= :date2) " +
			"or (pc.dateEnabledPlanCoupe >= :date1 and pc.dateEnabledPlanCoupe <= :date2) " +
			"or (pc.dateDisabledPlanCoupe >= :date1 and pc.dateDisabledPlanCoupe <= :date2)")
	List<PlanCoupe> findAllBetween(LocalDate date1, LocalDate date2);
	@Query("from PlanCoupe pc where not exists(select 1 from TimingModel tm where tm.idPlanCoupeTimingModel = pc.idPlanCoupe)")
    List<PlanCoupe> findPlanWithNoTimingModel();
}
