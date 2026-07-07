package com.lear.MGCMS.controller;

import com.lear.MGCMS.services.MaterialDemandForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Material Demand Forecast Controller — Phase 2 of Optimal Scheduling.
 *
 * Provides endpoints for:
 * - Forecasting material demand per zone
 * - Deferring series with material shortage
 * - Changing sequence status (IMPORTED, RELEASED, STARTED, COMPLETED, MATERIAL_MISSING, INCOMPLETE)
 */
@RestController
@RequestMapping("/api/material-demand")
public class MaterialDemandController {

    @Autowired
    private MaterialDemandForecastService forecastService;

    /**
     * Get material demand forecast for a zone over the next N hours.
     *
     * @param zone Zone name (e.g., "TFZ")
     * @param hours Forecast horizon in hours (default: 4)
     * @return JSON with demand/stock/deficit per material
     */
    @GetMapping("/forecast")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN', 'VALID_QN_LOGISTIQUE')")
    public ResponseEntity<Map<String, Object>> forecast(
            @RequestParam String zone,
            @RequestParam(defaultValue = "4") int hours) {
        return ResponseEntity.ok(forecastService.forecastDemand(zone, hours));
    }

    /**
     * Defer all series of a specific material in a zone.
     * Sets statusMatelassage to "Incomplete" for waiting series of that material.
     *
     * @param zone Zone name
     * @param material Material reference (with or without "P" prefix)
     */
    @PostMapping("/defer")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> deferMaterial(
            @RequestParam String zone,
            @RequestParam String material) {
        return ResponseEntity.ok(forecastService.deferMaterialSeries(zone, material));
    }

    /**
     * Change a sequence's status.
     * Allowed values: IMPORTED, RELEASED, STARTED, COMPLETED, MATERIAL_MISSING, INCOMPLETE
     */
    @PostMapping("/sequence/status")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> changeSequenceStatus(
            @RequestParam String sequenceId,
            @RequestParam String status) {
        return ResponseEntity.ok(forecastService.changeSequenceStatus(sequenceId, status));
    }
}
