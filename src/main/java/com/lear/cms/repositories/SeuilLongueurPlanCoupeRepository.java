package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.SeuilLongueurPlanCoupe;

public interface SeuilLongueurPlanCoupeRepository extends CrudRepository<SeuilLongueurPlanCoupe, Integer> {

	List<SeuilLongueurPlanCoupe> findByIdItemForeign1Plan(Integer idItemForeign1Plan);

	@Query("SELECT MAX(s.idSeuil_Plan) FROM SeuilLongueurPlanCoupe s")
	Integer findMaxId();

	void deleteByIdItemForeign1Plan(Integer idItemForeign1Plan);
}
