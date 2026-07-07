package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.ContinuousDispatchOptimizerService;
import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.EngineState;
import com.lear.MGCMS.services.dispatcher.SerieStatusDateValidator;
import com.lear.MGCMS.services.dispatcher.WorkbenchCacheService;
import com.lear.MGCMS.services.scheduling.ShiftClock;

@RestController
@RequestMapping("/api/workbench")
public class WorkbenchController {

    @Autowired private DispatcherProperties dispatcherProperties;
    @Autowired private ShiftClock shiftClock;
    @Autowired private WorkbenchCacheService workbenchCacheService;
    @Autowired private SerieStatusDateValidator validator;
    @Autowired private ContinuousDispatchOptimizerService optimizerService;

    @GetMapping("/data")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN','VALID_QN_LOGISTIQUE')")
    public ResponseEntity<?> data(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer shift) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        if (date == null) { date = slot.date; }
        if (shift == null) { shift = slot.shift; }
        if (shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().body("shift must be 1, 2 or 3");
        }
        Map<String, Object> data = workbenchCacheService.getData(date, shift);

        EngineState state = optimizerService.getState();
        if (state == EngineState.IDLE || state == EngineState.STOPPED) {
            workbenchCacheService.preloadEngineIfIdle(date, shift);
        }

        return ResponseEntity.ok(data);
    }

    @PostMapping("/reload")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN')")
    public ResponseEntity<?> reload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer shift) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        if (date == null) { date = slot.date; }
        if (shift == null) { shift = slot.shift; }
        if (shift < 1 || shift > 3) {
            return ResponseEntity.badRequest().body("shift must be 1, 2 or 3");
        }
        Map<String, Object> data = workbenchCacheService.reloadAll(date, shift);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/validate-statuses")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN')")
    public ResponseEntity<?> validateStatuses(
            @RequestParam(required = false) List<String> sequences,
            @RequestParam(required = false, defaultValue = "false") boolean autoCorrect) {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> result = validator.validateStatuses(sequences, autoCorrect);
        return ResponseEntity.ok(result);
    }
}
