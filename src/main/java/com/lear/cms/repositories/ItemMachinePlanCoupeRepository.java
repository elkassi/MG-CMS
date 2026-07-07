package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.ItemMachinePlanCoupe;

public interface ItemMachinePlanCoupeRepository extends CrudRepository<ItemMachinePlanCoupe, Integer> {

	public List<ItemMachinePlanCoupe> findByIdItemForeignPlan(Integer idItemForeignPlan);

	@Query("SELECT MAX(m.idItemMachinePlan) FROM ItemMachinePlanCoupe m")
	Integer findMaxId();

	void deleteByIdItemForeignPlan(Integer idItemForeignPlan);
}
