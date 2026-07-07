package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.ReftissuMargin;
import com.lear.MGCMS.domain.ReftissuMarginId;

import javax.transaction.Transactional;

public interface ReftissuMarginRepository extends CrudRepository<ReftissuMargin, ReftissuMarginId> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM [dbo].[ReftissuMargin] where partNumberMaterialConfig_partNumberMaterial = :itemNumberPlan", nativeQuery = true)
    void deleteByPartnumber(String itemNumberPlan);
}
