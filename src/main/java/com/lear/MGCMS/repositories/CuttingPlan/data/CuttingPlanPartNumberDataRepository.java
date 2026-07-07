package com.lear.MGCMS.repositories.CuttingPlan.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberId;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanPartNumberData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

public interface CuttingPlanPartNumberDataRepository extends JpaRepository<CuttingPlanPartNumberData, CuttingPlanPartNumberId>, JpaSpecificationExecutor<CuttingPlanPartNumberData>  {

    @Query("Select partNumber from CuttingPlanPartNumberData where cuttingPlan = :id")
    List<String> getPartNumbersByCuttingPlanId(Long id);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CuttingPlanPartNumberData WHERE cuttingPlan = :cuttingPlanId AND partNumber IN :partNumbers")
    void deleteByCuttingPlanIdAndPartNumberIn(Long cuttingPlanId, List<String> partNumbers);
}
