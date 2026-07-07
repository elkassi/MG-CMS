package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.ReftissuCategory;
import com.lear.MGCMS.domain.ReftissuCategoryId;

import javax.transaction.Transactional;

public interface ReftissuCategoryRepository extends CrudRepository<ReftissuCategory, ReftissuCategoryId> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM [dbo].[ReftissuCategory] where partNumberMaterialConfig_partNumberMaterial = :itemNumberPlan", nativeQuery = true)
    void deleteByPartnumber(String itemNumberPlan);
}
