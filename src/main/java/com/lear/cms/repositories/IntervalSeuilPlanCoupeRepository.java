package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.IntervalSeuilPlanCoupe;

public interface IntervalSeuilPlanCoupeRepository extends CrudRepository<IntervalSeuilPlanCoupe, Integer> {

	List<IntervalSeuilPlanCoupe> findByIdSeuilForeignPlan(Integer idSeuilForeignPlan);

	@Query("SELECT MAX(i.idIntervalSeuilPlan) FROM IntervalSeuilPlanCoupe i")
	Integer findMaxId();

	void deleteByIdSeuilForeignPlan(Integer idSeuilForeignPlan);
}
