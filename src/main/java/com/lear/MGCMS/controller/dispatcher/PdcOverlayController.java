package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.PdcActualsOverlayService;
import com.lear.MGCMS.services.dispatcher.PdcActualsOverlayService.MachineActuals;

/**
 * Phase 6 read-only overlay endpoint.
 *
 * <p>Returns per-machine planned-vs-capacity actuals plus a human-readable
 * efficiency badge the PdC can paint without additional formatting
 * work client-side.</p>
 */
@RestController
@RequestMapping("/api/pdc")
public class PdcOverlayController {

    @Autowired
    private PdcActualsOverlayService overlayService;

    @GetMapping("/overlay")
    public ResponseEntity<List<Map<String, Object>>> overlay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift,
            @RequestParam List<String> machines) {

        List<MachineActuals> actuals = overlayService.overlayFor(date, shift, machines);
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (MachineActuals a : actuals) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("machineNom", a.getMachineNom());
            m.put("queueDepth", a.getQueueDepth());
            m.put("plannedMinutes", a.getPlannedMinutes());
            m.put("shiftEffectiveMinutes", a.getShiftEffectiveMinutes());
            m.put("efficiencyRatio", a.getEfficiencyRatio());
            m.put("earliestStart", a.getEarliestStart());
            m.put("latestEnd", a.getLatestEnd());
            m.put("badge", overlayService.formatEfficiencyBadge(a));
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }
}
