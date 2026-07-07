package com.lear.cms.repositories;

import java.util.List;

import com.lear.cms.domain.TimingModel;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

public interface TimingModelRepository extends CrudRepository<TimingModel, Integer> {
    @Query("select max(idTimingModel) from TimingModel")
    Long maxId();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM [dbo].[Timing_Model] where ID_PlanCoupe_Timing_Model = ?1"
            , nativeQuery = true)
    void deletePlanById(Long id);

    List<TimingModel> findByIdPlanCoupeTimingModel(Long idPlanCoupeTimingModel);

    /**
     * Find TimingModels by placement name - lightweight query for Plan de Charge.
     */
    List<TimingModel> findByPlacementTimingModel(String placement);
    
    /**
     * Find TimingModels by multiple placements - optimized for batch loading.
     */
    @Query("SELECT t FROM TimingModel t WHERE t.placementTimingModel IN :placements")
    List<TimingModel> findByPlacementTimingModelIn(List<String> placements);

    /**
     * Lightweight projection: returns only (placement, validated, real) tuples.
     * Avoids hydrating the full 23-column TimingModel entity for Plan de Charge aggregation.
     * Returns: Object[] { placementTimingModel, validatedCuttingtimeTimingModel, realCuttingtimeTimingModel }
     */
    @Query("SELECT t.placementTimingModel, t.validatedCuttingtimeTimingModel, t.realCuttingtimeTimingModel " +
           "FROM TimingModel t WHERE t.placementTimingModel IN :placements")
    List<Object[]> findValidatedAndRealTimesByPlacements(List<String> placements);
    
    /**
     * Get only cutting times for specific placements - lightweight projection for load calculation.
     * Returns placement and validated cutting time (or real cutting time if validated is null).
     */
    @Query("SELECT t.placementTimingModel, COALESCE(t.validatedCuttingtimeTimingModel, t.realCuttingtimeTimingModel) " +
           "FROM TimingModel t WHERE t.placementTimingModel IN :placements")
    List<Object[]> findCuttingTimesByPlacements(List<String> placements);
    
    /**
     * Get cutting time for a single placement.
     */
    @Query("SELECT COALESCE(t.validatedCuttingtimeTimingModel, t.realCuttingtimeTimingModel) " +
           "FROM TimingModel t WHERE t.placementTimingModel = :placement")
    Double findCuttingTimeByPlacement(String placement);

    /**
     * Find the best cutting time for an item number.
     * Returns the minimum validated or real cutting time across all placements for this item.
     */
    @Query("SELECT COALESCE(t.validatedCuttingtimeTimingModel, t.realCuttingtimeTimingModel) " +
           "FROM TimingModel t WHERE t.itemNumberTimingModel = :itemNumber " +
           "ORDER BY COALESCE(t.validatedCuttingtimeTimingModel, t.realCuttingtimeTimingModel) ASC")
    List<Double> findCuttingTimesByItemNumber(String itemNumber);

}
