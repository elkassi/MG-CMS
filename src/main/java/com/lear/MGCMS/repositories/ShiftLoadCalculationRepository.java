package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ShiftLoadCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftLoadCalculationRepository extends JpaRepository<ShiftLoadCalculation, Long> {

    /**
     * Find calculation for a specific shift and date.
     */
    Optional<ShiftLoadCalculation> findByShiftDateAndShiftNumberAndMachineType(
            LocalDate shiftDate, Integer shiftNumber, String machineType);

    /**
     * Find all calculations for a specific date.
     */
    List<ShiftLoadCalculation> findByShiftDate(LocalDate shiftDate);

    /**
     * Find all calculations for a date range.
     */
    @Query("SELECT s FROM ShiftLoadCalculation s WHERE s.shiftDate BETWEEN :startDate AND :endDate ORDER BY s.shiftDate, s.shiftNumber")
    List<ShiftLoadCalculation> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find all calculations for a specific shift number across dates.
     */
    List<ShiftLoadCalculation> findByShiftNumber(Integer shiftNumber);

    /**
     * Find all calculations for a specific machine type.
     */
    List<ShiftLoadCalculation> findByMachineType(String machineType);

    /**
     * Find calculations for a date range and machine type.
     */
    @Query("SELECT s FROM ShiftLoadCalculation s WHERE " +
           "s.shiftDate BETWEEN :startDate AND :endDate AND " +
           "(s.machineType = :machineType OR :machineType IS NULL) " +
           "ORDER BY s.shiftDate, s.shiftNumber")
    List<ShiftLoadCalculation> findByDateRangeAndMachineType(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("machineType") String machineType);

    /**
     * Delete all calculations for a specific date and shift.
     */
    void deleteByShiftDateAndShiftNumber(LocalDate shiftDate, Integer shiftNumber);

    /**
     * Find global calculation (machineType is null or 'GLOBAL').
     */
    @Query("SELECT s FROM ShiftLoadCalculation s WHERE s.shiftDate = :shiftDate AND s.shiftNumber = :shiftNumber AND (s.machineType IS NULL OR s.machineType = 'GLOBAL')")
    Optional<ShiftLoadCalculation> findGlobalCalculation(
            @Param("shiftDate") LocalDate shiftDate,
            @Param("shiftNumber") Integer shiftNumber);
}
