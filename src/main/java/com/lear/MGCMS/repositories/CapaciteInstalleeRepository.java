package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CapaciteInstallee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CapaciteInstalleeRepository extends JpaRepository<CapaciteInstallee, Long> {

    List<CapaciteInstallee> findByDateProductionBetween(LocalDate startDate, LocalDate endDate);

    List<CapaciteInstallee> findByDateProductionAndShiftNumberAndGroupe(LocalDate dateProduction, Integer shiftNumber, String groupe);

    List<CapaciteInstallee> findByDateProductionIsNullAndShiftNumberIsNull();

    @Query("SELECT c FROM CapaciteInstallee c WHERE c.dateProduction IS NULL AND c.shiftNumber IS NULL AND c.groupe = :groupe")
    List<CapaciteInstallee> findDefaultByGroupe(@Param("groupe") String groupe);
}
