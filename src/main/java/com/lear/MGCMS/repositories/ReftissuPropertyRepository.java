package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CuttingPlan.PartNumberCorrespendance;
import com.lear.MGCMS.domain.PlanningDetails;
import com.lear.MGCMS.domain.PlanningId;
import com.lear.MGCMS.domain.ReftissuProperty;
import com.lear.MGCMS.domain.ReftissuPropertyId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReftissuPropertyRepository extends CrudRepository<ReftissuProperty, ReftissuPropertyId>, JpaSpecificationExecutor<ReftissuProperty> {
    @Query("From ReftissuProperty where (:reftissu is null or reftissu = :reftissu) and (:property is null or property = :property) ")
    List<ReftissuProperty> findAllByReftissuAndProperty(String reftissu, String property);
}
