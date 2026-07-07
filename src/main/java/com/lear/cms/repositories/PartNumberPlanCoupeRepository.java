package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.PartNumberPlanCoupe;

import javax.transaction.Transactional;

public interface PartNumberPlanCoupeRepository extends CrudRepository<PartNumberPlanCoupe, Long> {
	
	List<PartNumberPlanCoupe> findByIdPartNumberPlanForeignPlanCoupe(Long idPartNumberPlanForeignPlanCoupe);

    @Query("select max(idPartNumberPlanCoupe) from PartNumberPlanCoupe")
    Long maxId();

    //DELETE FROM [dbo].[PartNumber_PlanCoupe] where ID_PartNumber_PlanForeign_PlanCoupe = @id
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM [dbo].[PartNumber_PlanCoupe] where ID_PartNumber_PlanForeign_PlanCoupe = ?1"
            , nativeQuery = true)
    void deletePlanById(Long id);
}
