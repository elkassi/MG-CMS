package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.MaintenanceIntervention;
import com.lear.MGCMS.domain.MaintenanceInterventionConfig;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MaintenanceInterventionRepository  extends CrudRepository<MaintenanceIntervention, Long>, JpaSpecificationExecutor<MaintenanceIntervention> {
    @Query("select max(date) from MaintenanceIntervention where machine = :nom")
    LocalDateTime findMaxDateByMachine(String nom);
    @Query("from MaintenanceIntervention where machine in :machines")
    List<MaintenanceIntervention> findByMachineIn(List<String> machines);
}
