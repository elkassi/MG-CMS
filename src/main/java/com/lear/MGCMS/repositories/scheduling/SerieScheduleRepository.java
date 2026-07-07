package com.lear.MGCMS.repositories.scheduling;

import com.lear.MGCMS.domain.scheduling.SerieSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SerieScheduleRepository extends JpaRepository<SerieSchedule, Long>, JpaSpecificationExecutor<SerieSchedule> {

    Optional<SerieSchedule> findBySerieId(String serieId);
    
    List<SerieSchedule> findBySequenceId(String sequenceId);
    
    List<SerieSchedule> findBySpreadingTable(String spreadingTable);
    
    List<SerieSchedule> findByCuttingMachineName(String cuttingMachineName);
    
    List<SerieSchedule> findBySpreadingStatus(String spreadingStatus);
    
    List<SerieSchedule> findByCuttingStatus(String cuttingStatus);
    
    List<SerieSchedule> findByPartNumberMaterial(String partNumberMaterial);
    
    @Query("SELECT ss FROM SerieSchedule ss WHERE ss.cuttingMachineName = :machineName AND ss.cuttingStartEstimated >= :startTime AND ss.cuttingStartEstimated <= :endTime ORDER BY ss.cuttingStartEstimated ASC")
    List<SerieSchedule> findScheduledForMachineInRange(String machineName, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT ss FROM SerieSchedule ss WHERE ss.spreadingTable = :tableName AND ss.spreadingStartEstimated >= :startTime AND ss.spreadingStartEstimated <= :endTime ORDER BY ss.spreadingStartEstimated ASC")
    List<SerieSchedule> findScheduledForSpreadingTableInRange(String tableName, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT ss FROM SerieSchedule ss WHERE ss.spreadingStatus = 'WAITING' ORDER BY ss.schedulingOrder ASC")
    List<SerieSchedule> findWaitingForSpreading();
    
    @Query("SELECT ss FROM SerieSchedule ss WHERE ss.spreadingStatus = 'COMPLETE' AND ss.cuttingStatus = 'WAITING' ORDER BY ss.schedulingOrder ASC")
    List<SerieSchedule> findReadyForCutting();
    
    @Query("SELECT ss FROM SerieSchedule ss WHERE ss.sequenceId IN :sequenceIds ORDER BY ss.schedulingOrder ASC")
    List<SerieSchedule> findBySequenceIds(List<String> sequenceIds);
}

