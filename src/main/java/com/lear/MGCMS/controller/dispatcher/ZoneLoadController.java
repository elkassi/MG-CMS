package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.ZoneLoadDto;
import com.lear.MGCMS.services.dispatcher.ZoneLoadProperties;
import com.lear.MGCMS.services.dispatcher.ZoneLoadService;

/**
 * Read-only API behind the Process Dispatcher heatmap and equilibre badge
 * (Phase A — MASTER_SCHEDULING_VISION_v3.md §4.1, §4.2).
 *
 * <p>Both endpoints return 404 when {@code mgcms.dispatcher.enabled=false}
 * or {@code mgcms.zoneload.heatmap-enabled=false} — same kill-switch
 * pattern the rest of the dispatcher uses, so a feature freeze is one
 * config flip.</p>
 */
@RestController
@RequestMapping("/api/zoneLoad")
public class ZoneLoadController {

    @Autowired private DispatcherProperties dispatcherProperties;
    @Autowired private ZoneLoadProperties zoneLoadProperties;
    @Autowired private ZoneLoadService zoneLoadService;

    @GetMapping("/matrix")
    public ResponseEntity<ZoneLoadDto> matrix(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift) {
        if (!dispatcherProperties.isEnabled() || !zoneLoadProperties.isHeatmapEnabled()) {
            return ResponseEntity.notFound().build();
        }
        if (shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(zoneLoadService.computeMatrix(date, shift));
    }

    @GetMapping("/equilibre")
    public ResponseEntity<ZoneLoadDto.EquilibreSummaryDto> equilibre(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift) {
        if (!dispatcherProperties.isEnabled() || !zoneLoadProperties.isHeatmapEnabled()) {
            return ResponseEntity.notFound().build();
        }
        if (shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(zoneLoadService.computeEquilibre(date, shift));
    }
}
