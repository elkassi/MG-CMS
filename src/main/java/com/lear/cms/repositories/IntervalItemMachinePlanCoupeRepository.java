package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.IntervalItemMachinePlanCoupe;

public interface IntervalItemMachinePlanCoupeRepository extends CrudRepository<IntervalItemMachinePlanCoupe, Integer> {

	public List<IntervalItemMachinePlanCoupe> findByIdItemMachineForeignPlan(Integer idItemMachineForeignPlan);

	@Query("SELECT MAX(i.idIntervalItemMachinePlan) FROM IntervalItemMachinePlanCoupe i")
	Integer findMaxId();

	void deleteByIdItemMachineForeignPlan(Integer idItemMachineForeignPlan);
}
