package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.ReftissuMachine;
import com.lear.MGCMS.domain.ReftissuMachineId;

import javax.transaction.Transactional;

public interface ReftissuMachineRepository extends CrudRepository<ReftissuMachine, ReftissuMachineId> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM [dbo].[ReftissuMachine] where partNumberMaterialConfig_partNumberMaterial = :itemNumberPlan", nativeQuery = true)
    void deleteByPartnumber(String itemNumberPlan);
}
