package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CoupeMachineHistory;
import com.lear.MGCMS.domain.CoupePerformance;
import com.lear.MGCMS.domain.CoupePerformanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface CoupePerformanceRepository extends JpaRepository<CoupePerformance, CoupePerformanceId>, JpaSpecificationExecutor<CoupePerformance> {

    @Query("Select crs from CoupePerformance crs "
            + " where crs.date = :date and crs.shift = :shift and crs.machine in :machines order by date")
    List<CoupePerformance> findFilter(LocalDate date, String shift, List<String> machines);
}
