package com.lear.splice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.splice.domain.Marker;
import com.lear.splice.domain.MarkerLog;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MarkerLogRepository extends JpaRepository<MarkerLog, Long>, JpaSpecificationExecutor<MarkerLog> {

    @Query("from MarkerLog where workOrderCode = :serie order by createdAt desc")
    List<MarkerLog> findBySerie(String serie);
    @Query("from MarkerLog where createdAt >= :startDate and createdAt <= :endDate and marker like '%TEST%' and numberOfLayersDone > 0")
    List<MarkerLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
