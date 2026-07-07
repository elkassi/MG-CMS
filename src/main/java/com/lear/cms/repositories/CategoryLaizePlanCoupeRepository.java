package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.CategoryLaizePlanCoupe;

public interface CategoryLaizePlanCoupeRepository extends CrudRepository<CategoryLaizePlanCoupe, Integer> {

	List<CategoryLaizePlanCoupe> findByIdItemForeignPlan (Integer idItemForeignPlan);

	@Query("SELECT MAX(c.idCategoryPlan) FROM CategoryLaizePlanCoupe c")
	Integer findMaxId();

	void deleteByIdItemForeignPlan(Integer idItemForeignPlan);
}
