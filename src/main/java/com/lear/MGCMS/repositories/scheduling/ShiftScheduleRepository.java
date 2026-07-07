package com.lear.MGCMS.repositories.scheduling;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.scheduling.ShiftSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, Long>, JpaSpecificationExecutor<ShiftSchedule> {

    List<ShiftSchedule> findByZone(Zone zone);
    
    List<ShiftSchedule> findByShiftDate(LocalDate shiftDate);
    
    List<ShiftSchedule> findByShiftDateAndShiftNumber(LocalDate shiftDate, Integer shiftNumber);
    
    Optional<ShiftSchedule> findByZoneAndShiftDateAndShiftNumber(Zone zone, LocalDate shiftDate, Integer shiftNumber);
    
    List<ShiftSchedule> findByShiftDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<ShiftSchedule> findByStatus(String status);
    
    @Query("SELECT ss FROM ShiftSchedule ss WHERE ss.zone.nom = :zoneName AND ss.shiftDate = :shiftDate AND ss.shiftNumber = :shiftNumber")
    Optional<ShiftSchedule> findByZoneNameAndDateAndShift(String zoneName, LocalDate shiftDate, Integer shiftNumber);
    
    @Query("SELECT ss FROM ShiftSchedule ss WHERE ss.shiftDate >= :startDate ORDER BY ss.shiftDate ASC, ss.shiftNumber ASC")
    List<ShiftSchedule> findUpcomingSchedules(LocalDate startDate);
}

