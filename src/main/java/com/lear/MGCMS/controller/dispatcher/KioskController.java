package com.lear.MGCMS.controller.dispatcher;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.KioskService;
import com.lear.MGCMS.services.dispatcher.NextSerieDto;

/**
 * Phase 8 operator-kiosk endpoints.
 *
 * <p>No authentication (kiosk tablets run on shopfloor LAN and have no
 * login); Spring Security should exempt {@code /api/kiosk/**} at the
 * config level. Both endpoints are cheap reads — the kiosk polls
 * {@code /version} every 2 s and only calls {@code /nextSerie} when the
 * version changes.</p>
 */
@RestController
@RequestMapping("/api/kiosk")
public class KioskController {

    @Autowired
    private KioskService kioskService;

    /** Head-of-queue job; 204 when the queue is empty. */
    @GetMapping("/nextSerie")
    public ResponseEntity<NextSerieDto> nextSerie(@RequestParam String machine) {
        return kioskService.nextSerie(machine)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** Monotonic version counter — kiosks poll this to detect changes. */
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version(@RequestParam String machine) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("machineNom", machine);
        out.put("version", kioskService.currentVersion(machine));
        return ResponseEntity.ok(out);
    }
}
