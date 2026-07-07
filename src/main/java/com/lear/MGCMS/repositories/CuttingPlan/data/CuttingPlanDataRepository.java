package com.lear.MGCMS.repositories.CuttingPlan.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanData;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CuttingPlanDataRepository extends JpaRepository<CuttingPlanData, Long>, JpaSpecificationExecutor<CuttingPlanData>  {

    @Query("Select cpd from CuttingPlanData cpd where cpd.id in (:listId)")
    List<CuttingPlanData> findByListId(List<Long> listId);

    CuttingPlanData findFirstByCmsId(Long idPlanCoupe);

    @Query("from CuttingPlanData cpd where description = :description and enabled = true")
    CuttingPlanData findByDescription(String description);
}
