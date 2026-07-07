package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.MaintenanceInterventionConfig;
import com.lear.MGCMS.domain.PartNumberBoom;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MaintenanceInterventionConfigRepository  extends CrudRepository<MaintenanceInterventionConfig, Long>, JpaSpecificationExecutor<MaintenanceInterventionConfig> {
    @Query("FROM MaintenanceInterventionConfig WHERE active = 1")
    Iterable<MaintenanceInterventionConfig> findAllActive();

    @Query("SELECT t from MaintenanceInterventionConfig t where t.id = :id")
    PartNumberBoom findByObjId(Long id);

}
