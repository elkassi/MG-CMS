package com.lear.MGCMS.repositories.scheduling;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.scheduling.SequenceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SequenceScheduleRepository extends JpaRepository<SequenceSchedule, Long>, JpaSpecificationExecutor<SequenceSchedule> {

    Optional<SequenceSchedule> findBySequenceId(String sequenceId);
    
    List<SequenceSchedule> findByAssignedZone(Zone zone);
    
    List<SequenceSchedule> findByStatus(String status);
    
    List<SequenceSchedule> findByExcluded(Boolean excluded);
    
    List<SequenceSchedule> findByScheduledDateAndScheduledShift(LocalDate scheduledDate, Integer scheduledShift);
    
    @Query("SELECT ss FROM SequenceSchedule ss WHERE ss.assignedZone.nom = :zoneName AND ss.excluded = false ORDER BY ss.priority ASC")
    List<SequenceSchedule> findActiveByZoneOrderByPriority(String zoneName);
    
    @Query("SELECT ss FROM SequenceSchedule ss WHERE ss.excluded = false ORDER BY ss.priority ASC")
    List<SequenceSchedule> findAllActiveOrderByPriority();
    
    @Query("SELECT ss FROM SequenceSchedule ss WHERE ss.status = 'IN_PROGRESS'")
    List<SequenceSchedule> findInProgressSequences();
    
    @Query("SELECT ss FROM SequenceSchedule ss WHERE ss.scheduledDate = :date AND ss.excluded = false ORDER BY ss.priority ASC")
    List<SequenceSchedule> findByDateOrderByPriority(LocalDate date);
    
    @Query("SELECT COUNT(ss) FROM SequenceSchedule ss WHERE ss.assignedZone = :zone AND ss.status = :status")
    Long countByZoneAndStatus(Zone zone, String status);
}

