package com.lear.cms.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.cms.domain.SpreadingCuttingPlanCoupe;

import javax.transaction.Transactional;

public interface SpreadingCuttingPlanCoupeRepository extends CrudRepository<SpreadingCuttingPlanCoupe, Long> {

	List<SpreadingCuttingPlanCoupe> findByIdSpreadingPlanForeignPlanCoupe(Long idSpreadingPlanForeignPlanCoupe);

    @Query("select max(idSpreadingCuttingPlanCoupe) from SpreadingCuttingPlanCoupe")
    Long maxId();
    @Query("select max(idSpreadingCuttingParentPlanCoupe) from SpreadingCuttingPlanCoupe")
    Long maxIdParent();
    //delete FROM [dbo].[Spreading_Cutting_PlanCoupe] where ID_Spreading_Cutting_PlanForeign_PlanCoupe = @id order by ID_SpreadingCutting_Parent_PlanCoupe
    @Modifying
    @Transactional
    @Query(value = "delete FROM [dbo].[Spreading_Cutting_PlanCoupe] where ID_Spreading_Cutting_PlanForeign_PlanCoupe = ?1"
            , nativeQuery = true)
    void deletePlanById(Long id);

    List<SpreadingCuttingPlanCoupe> findByIdSpreadingPlanForeignPlanCoupeAndPlacementPlanCoupe(Long idPlanCoupe, String placement);

    @Modifying
    @Transactional
    @Query(value = "update [dbo].[Spreading_Cutting_PlanCoupe] set Longueur_Placement_PlanCoupe = ?2 , Longueur_Matelas_PlanCoupe = ?3 "+
            "where Placement_PlanCoupe = ?1"
            , nativeQuery = true)
    void updatePlacement(String placement, Double newLongueur, Double longueurMatelas);

    @Query("from SpreadingCuttingPlanCoupe where placementPlanCoupe = :placement and idSpreadingPlanForeignPlanCoupe = :idPlan")
    SpreadingCuttingPlanCoupe findByPlacementAndIdSpreadingPlanForeignPlanCoupe(String placement, Long idPlan);
}
