package com.lear.MGCMS.repositories.CuttingPlan.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialId;
import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

public interface CuttingPlanMaterialDataRepository extends JpaRepository<CuttingPlanMaterialData, CuttingPlanMaterialId>, JpaSpecificationExecutor<CuttingPlanMaterialData>  {

    @Query("SELECT partNumberMaterial FROM CuttingPlanMaterialData WHERE cuttingPlan = :id")
    List<String> getPartNumberMaterialsByCuttingPlanId(Long id);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CuttingPlanMaterialData WHERE cuttingPlan = :cuttingPlanId AND partNumberMaterial IN :partNumberMaterials")
    void deleteByCuttingPlanIdAndPartNumberMaterialIn(Long cuttingPlanId, List<String> partNumberMaterials);
}
