package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ScanXPL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ScanXPLRepository extends JpaRepository<ScanXPL, Long>, JpaSpecificationExecutor<ScanXPL> {

    @Query("SELECT s FROM ScanXPL s WHERE s.serie = :serie")
    List<ScanXPL> findBySerie(String serie);

    @Query("SELECT s FROM ScanXPL s WHERE s.machine = :machine")
    List<ScanXPL> findByMachine(String machine);

    @Query("SELECT s FROM ScanXPL s WHERE s.machine = :machine AND s.scanDate >= :startDate AND s.scanDate <= :endDate")
    List<ScanXPL> findByMachineAndDateRange(String machine, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT s FROM ScanXPL s WHERE s.machine IN :machines AND s.scanDate >= :startDate AND s.scanDate <= :endDate")
    List<ScanXPL> findByMachinesAndDateRange(List<String> machines, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT s FROM ScanXPL s WHERE s.serie IN :series")
    List<ScanXPL> findBySerieIn(List<String> series);

    @Query("SELECT DISTINCT s.serie FROM ScanXPL s WHERE s.machine IN :machines AND s.scanDate >= :startDate AND s.scanDate <= :endDate")
    List<String> findDistinctSeriesByMachinesAndDateRange(List<String> machines, LocalDateTime startDate, LocalDateTime endDate);
}
