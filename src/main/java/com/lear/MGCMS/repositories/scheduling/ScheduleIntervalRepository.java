package com.lear.MGCMS.repositories.scheduling;

import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.scheduling.ScheduleInterval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleIntervalRepository extends JpaRepository<ScheduleInterval, Long>, JpaSpecificationExecutor<ScheduleInterval> {

    List<ScheduleInterval> findByMachine(ProductionTable machine);
    
    List<ScheduleInterval> findByMachineName(String machineName);
    
    List<ScheduleInterval> findByZone(Zone zone);
    
    List<ScheduleInterval> findByIntervalType(String intervalType);
    
    @Query("SELECT si FROM ScheduleInterval si WHERE si.startTime <= :endTime AND si.endTime >= :startTime")
    List<ScheduleInterval> findOverlappingIntervals(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT si FROM ScheduleInterval si WHERE si.startTime >= :startTime AND si.endTime <= :endTime")
    List<ScheduleInterval> findIntervalsInRange(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT si FROM ScheduleInterval si WHERE (si.machineName = :machineName OR si.machineName IS NULL) AND si.startTime <= :endTime AND si.endTime >= :startTime")
    List<ScheduleInterval> findIntervalsForMachineInRange(String machineName, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT si FROM ScheduleInterval si WHERE si.recurring = true")
    List<ScheduleInterval> findRecurringIntervals();
    
    List<ScheduleInterval> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}

