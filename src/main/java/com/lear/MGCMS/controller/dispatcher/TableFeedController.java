package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.TableFeedRankingService;
import com.lear.MGCMS.services.scheduling.ShiftClock;

/**
 * REST surface for the {@code /tableFeed} matelassage-feed advisor.
 *
 * <p>For the given (date, shift) it returns, per active zone, every UP table's
 * time-to-idle and the top-N series to mount next so the downstream CNC cutter
 * never starves. Read-only — like {@code /liveCharge}, it never mutates state.</p>
 *
 * <p>Same gating as {@link LiveChargeController}: 404 when
 * {@code mgcms.dispatcher.enabled=false}, otherwise open to
 * {@code ROLE_PROCESS}, {@code ROLE_CHEF_DE_ZONE},
 * {@code ROLE_CHEF_EQUIPE} and {@code ROLE_ADMIN}.</p>
 */
@RestController
@RequestMapping("/api/dispatcher")
public class TableFeedController {

    @Autowired private TableFeedRankingService tableFeedRankingService;
    @Autowired private DispatcherProperties dispatcherProperties;
    @Autowired private ShiftClock shiftClock;

    @GetMapping("/tableFeed")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN')")
    public ResponseEntity<?> tableFeed(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer shift,
            @RequestParam(required = false) Integer topN) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        if (date == null) { date = slot.date; }
        if (shift == null) { shift = slot.shift; }
        if (shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().body("shift must be 1, 2 or 3");
        }
        return ResponseEntity.ok(tableFeedRankingService.compute(date, shift, topN == null ? 0 : topN));
    }
}
