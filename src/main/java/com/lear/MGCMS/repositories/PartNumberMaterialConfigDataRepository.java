package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanLight2;
import com.lear.MGCMS.domain.PartNumberMaterialConfigData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PartNumberMaterialConfigDataRepository extends JpaRepository<PartNumberMaterialConfigData, String>, JpaSpecificationExecutor<PartNumberMaterialConfigData> {

    @Query("select partNumberMaterial from PartNumberMaterialConfigData")
    List<String> getPartNumberMaterial();

}
