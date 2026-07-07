package com.lear.splice.repositories;

import com.lear.splice.domain.CalibrationLog;
import com.lear.splice.domain.MarkerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CalibrationLogRepository extends JpaRepository<CalibrationLog, Long>, JpaSpecificationExecutor<CalibrationLog> {

    @Query("from CalibrationLog cl where cl.createdAt >= :startDate and cl.createdAt <= :endDate ")
    List<CalibrationLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
