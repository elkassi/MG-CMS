package com.lear.MGCMS.controller.production;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.production.FloorStateService;

/**
 * Read-only API for the production-floor view ({@code ProductionFloor.js}).
 * Returns the whole floor — zones, machines (with live cutting status + queue),
 * matelassage tables, racks, and box occupancy — for a (date, shift).
 *
 * <p>{@code date} and {@code shift} are optional; when omitted the service defaults
 * to the plant's current slot via {@code ShiftClock}. Gated behind the same
 * {@code mgcms.dispatcher.enabled} kill-switch and the same read role set the other
 * floor/dispatcher read endpoints use.</p>
 */
@RestController
@RequestMapping("/api/production")
public class FloorStateController {

    @Autowired private FloorStateService floorStateService;
    @Autowired private DispatcherProperties dispatcherProperties;

    @GetMapping("/floor")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE','VARIANCE')")
    public ResponseEntity<Map<String, Object>> floor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer shift) {
        if (!dispatcherProperties.isEnabled()) return ResponseEntity.notFound().build();
        if (shift != null && (shift < 1 || shift > 3)) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(floorStateService.getFloorState(date, shift));
    }
}
