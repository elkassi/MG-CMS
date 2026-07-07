package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.PlanDeChargeSnapshot;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.ShiftLoadCalculation;
import com.lear.MGCMS.services.PlanDeChargeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planDeCharge")
public class PlanDeChargeController {

    @Autowired
    private PlanDeChargeService service;

    /**
     * Get the full plan de charge data for a date range.
     * Includes machines grouped by zone, status grid, and load calculations.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> result = new HashMap<>();
        
        // Get machines grouped by zone
        Map<String, List<ProductionTable>> machinesByZone = service.getMachinesGroupedByZone();
        result.put("machinesByZone", machinesByZone);
        
        // Get status grid
        Map<String, Map<LocalDate, Map<Integer, String>>> statusGrid = 
                service.getMachineStatusGrid(startDate, endDate);
        result.put("statusGrid", statusGrid);
        
        // Get saved load calculations
        List<ShiftLoadCalculation> loadCalculations = 
                service.getShiftLoadCalculations(startDate, endDate);
        result.put("loadCalculations", loadCalculations);
        
        // Get current shift info
        Map<String, Object> currentShiftInfo = service.getCurrentShiftInfo();
        result.put("currentShift", currentShiftInfo);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get machines grouped by zone.
     */
    @GetMapping("/machines")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, List<ProductionTable>>> getMachines() {
        return ResponseEntity.ok(service.getMachinesGroupedByZone());
    }

    /**
     * Get shift times for a specific date and shift number.
     */
    @GetMapping("/shiftTimes")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, LocalDateTime>> getShiftTimes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shiftNumber) {
        return ResponseEntity.ok(service.getShiftTimes(date, shiftNumber));
    }

    /**
     * Get current shift information.
     */
    @GetMapping("/currentShift")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentShift() {
        return ResponseEntity.ok(service.getCurrentShiftInfo());
    }

    /**
     * Calculate load for a specific shift.
     */
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<ShiftLoadCalculation>> calculateShiftLoad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shiftNumber,
            Authentication authentication) {
        String username = authentication.getName();
        List<ShiftLoadCalculation> calculations = service.calculateShiftLoad(date, shiftNumber, username);
        return ResponseEntity.ok(calculations);
    }

    /**
     * Get saved load calculations for a date range.
     */
    @GetMapping("/loadCalculations")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<ShiftLoadCalculation>> getLoadCalculations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getShiftLoadCalculations(startDate, endDate));
    }
    
    /**
     * Get aggregated cutting time by machine for a date range - lightweight endpoint.
     */
    @GetMapping("/aggregatedCuttingTime")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Map<LocalDate, Map<String, Double>>>> getAggregatedCuttingTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getAggregatedCuttingTimeByMachine(startDate, endDate));
    }
    
    /**
     * Get cutting times from TimingModel for specific placements.
     */
    @PostMapping("/cuttingTimesByPlacements")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Double>> getCuttingTimesByPlacements(
            @RequestBody List<String> placements) {
        return ResponseEntity.ok(service.getCuttingTimesByPlacements(placements));
    }
    
    /**
     * Get distinct placements for a date range - lightweight for batch lookup.
     */
    @GetMapping("/distinctPlacements")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<String>> getDistinctPlacements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getDistinctPlacements(startDate, endDate));
    }

    /**
     * Get detailed series for a specific date and shift.
     * Returns series with cutting times from TimingModel (validated > real > tempsDeCoupe).
     */
    @GetMapping("/detailedSeries")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getDetailedSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam String dueShift) {
        return ResponseEntity.ok(service.getDetailedSeriesForShift(dueDate, dueShift));
    }

    /**
     * Get shift charge summary with breakdown by machine type.
     * Includes total charge, cut time, carryover, and series grouped by machine type.
     */
    @GetMapping("/chargeSummary")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getChargeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam String dueShift) {
        return ResponseEntity.ok(service.calculateShiftChargeSummary(dueDate, dueShift));
    }

    /**
     * Get aggregated cutting time with completion status (cut vs not cut) for retard calculation.
     * Returns: machine -> date -> shift -> { total, cut, notCut }
     */
    @GetMapping("/aggregatedCuttingTimeWithStatus")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Map<LocalDate, Map<String, Map<String, Double>>>>> getAggregatedCuttingTimeWithStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getAggregatedCuttingTimeWithStatus(startDate, endDate));
    }

    /**
     * Get all series for a date range with resolved effective cutting times.
     * Lightweight endpoint for frontend indicator calculations.
     */
    @GetMapping("/seriesForDateRange")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getSeriesForDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getSeriesForDateRange(startDate, endDate));
    }

    /**
     * Get non-imported charge from Order_Schedule (status = 'F') for a specific shift.
     * Returns total estimated minutes and per-item details.
     */
    @GetMapping("/nonImportedCharge")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getNonImportedCharge(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String shift) {
        return ResponseEntity.ok(service.getNonImportedCharge(date, shift));
    }

    /**
     * Part-number cutting-time report for a shift: project/version/part number/quantity/
     * CMS plan id/sequence/perimeter/% of plan/cutting time (minutes), imported + non-imported.
     */
    @GetMapping("/partNumberReport")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getPartNumberReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String shift) {
        return ResponseEntity.ok(service.getPartNumberCuttingTimeReport(date, shift));
    }

    /**
     * Get the persisted "Détails de charge" snapshot for a past shift.
     * Returns 204 when no snapshot exists yet (the caller then computes live).
     */
    @GetMapping("/snapshot")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<PlanDeChargeSnapshot> getSnapshot(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift) {
        return service.getShiftSnapshot(date, shift)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Persist / overwrite the "Détails de charge" snapshot for a past shift.
     * The body is the raw JSON payload the frontend built for that shift.
     */
    @PostMapping("/snapshot")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<PlanDeChargeSnapshot> saveSnapshot(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift,
            @RequestBody String snapshotJson,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(service.saveShiftSnapshot(date, shift, snapshotJson, username));
    }
}
