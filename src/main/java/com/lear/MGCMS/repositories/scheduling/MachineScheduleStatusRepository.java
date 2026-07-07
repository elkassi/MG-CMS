package com.lear.MGCMS.repositories.scheduling;

import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.scheduling.MachineScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineScheduleStatusRepository extends JpaRepository<MachineScheduleStatus, Long>, JpaSpecificationExecutor<MachineScheduleStatus> {

    List<MachineScheduleStatus> findByMachine(ProductionTable machine);
    
    List<MachineScheduleStatus> findByEffectiveDateAndShiftNumber(LocalDate effectiveDate, Integer shiftNumber);
    
    Optional<MachineScheduleStatus> findByMachineAndEffectiveDateAndShiftNumber(ProductionTable machine, LocalDate effectiveDate, Integer shiftNumber);
    
    List<MachineScheduleStatus> findByAvailable(Boolean available);
    
    @Query("SELECT mss FROM MachineScheduleStatus mss WHERE mss.machine.zone = :zone AND mss.effectiveDate = :effectiveDate AND mss.shiftNumber = :shiftNumber")
    List<MachineScheduleStatus> findByZoneAndDateAndShift(Zone zone, LocalDate effectiveDate, Integer shiftNumber);
    
    @Query("SELECT mss FROM MachineScheduleStatus mss WHERE mss.overrideZone = :zone AND mss.effectiveDate = :effectiveDate AND mss.shiftNumber = :shiftNumber")
    List<MachineScheduleStatus> findByOverrideZoneAndDateAndShift(Zone zone, LocalDate effectiveDate, Integer shiftNumber);
    
    @Query("SELECT mss FROM MachineScheduleStatus mss WHERE mss.available = false AND mss.effectiveDate >= :startDate")
    List<MachineScheduleStatus> findUnavailableMachines(LocalDate startDate);
    
    @Query("SELECT mss FROM MachineScheduleStatus mss WHERE mss.unavailableReason = :reason AND mss.effectiveDate = :effectiveDate")
    List<MachineScheduleStatus> findByReasonAndDate(String reason, LocalDate effectiveDate);
}

