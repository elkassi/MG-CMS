package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.DrillPlanCoupe;

import javax.transaction.Transactional;

public interface DrillPlanCoupeRepository extends CrudRepository<DrillPlanCoupe, Long> {
	
	List<DrillPlanCoupe> findByIdCuttingForeignPlanCoupe(Long idCuttingForeignPlanCoupe);

    @Query("select max(idDrillPlanCoupe) from DrillPlanCoupe")
    Long maxId();

    //DELETE FROM [dbo].Drill_PlanCoupe where ID_CuttingForeign_PlanCoupe in (SELECT ID_Spreading_Cutting_PlanCoupe FROM [dbo].[Spreading_Cutting_PlanCoupe] where ID_Spreading_Cutting_PlanForeign_PlanCoupe = @id)
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM [dbo].Drill_PlanCoupe where ID_CuttingForeign_PlanCoupe in (SELECT ID_Spreading_Cutting_PlanCoupe FROM [dbo].[Spreading_Cutting_PlanCoupe] where ID_Spreading_Cutting_PlanForeign_PlanCoupe = ?1)"
            , nativeQuery = true)
    void deletePlanById(Long id);

}
