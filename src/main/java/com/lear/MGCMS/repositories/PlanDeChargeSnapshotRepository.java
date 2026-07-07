package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.PlanDeChargeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PlanDeChargeSnapshotRepository extends JpaRepository<PlanDeChargeSnapshot, Long> {

    /**
     * Find the persisted snapshot for a specific (date, shift), if one exists.
     */
    Optional<PlanDeChargeSnapshot> findByShiftDateAndShiftNumber(LocalDate shiftDate, Integer shiftNumber);
}
