package com.lear.MGCMS.controller.scheduling;

import com.lear.MGCMS.domain.scheduling.SchedulingConfig;
import com.lear.MGCMS.repositories.scheduling.SchedulingConfigRepository;
import com.lear.MGCMS.services.scheduling.DispatchAlgorithms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/scheduling/config")
public class SchedulingConfigController {

    @Autowired
    private SchedulingConfigRepository repository;

    /**
     * Get scheduling config for a zone (or global default if zone is null).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<SchedulingConfig> getConfig(
            @RequestParam(required = false) String zone) {
        Optional<SchedulingConfig> config;
        if (zone != null && !zone.isEmpty()) {
            config = repository.findByZoneCode(zone);
        } else {
            config = repository.findByZoneCodeIsNull();
        }
        return ResponseEntity.ok(config.orElseGet(SchedulingConfig::balanced));
    }

    /**
     * Save/update scheduling config.
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<SchedulingConfig> saveConfig(
            @RequestBody SchedulingConfig config,
            Authentication auth) {
        // Validate algorithm
        if (config.getAlgorithm() != null &&
                !DispatchAlgorithms.AVAILABLE_ALGORITHMS.contains(config.getAlgorithm().toUpperCase())) {
            return ResponseEntity.badRequest().build();
        }

        // Find existing or create new
        Optional<SchedulingConfig> existing;
        if (config.getZoneCode() != null && !config.getZoneCode().isEmpty()) {
            existing = repository.findByZoneCode(config.getZoneCode());
        } else {
            existing = repository.findByZoneCodeIsNull();
        }

        if (existing.isPresent()) {
            SchedulingConfig current = existing.get();
            current.setAlgorithm(config.getAlgorithm());
            current.setPreset(config.getPreset());
            current.setWeightBoxCompletion(config.getWeightBoxCompletion());
            current.setWeightMachineUtilization(config.getWeightMachineUtilization());
            current.setWeightChangeover(config.getWeightChangeover());
            current.setWeightDueDate(config.getWeightDueDate());
            current.setAutoApply(config.isAutoApply());
            current.setAutoDispatchIntervalMinutes(config.getAutoDispatchIntervalMinutes());
            current.setUpdatedBy(auth != null ? auth.getName() : null);
            return ResponseEntity.ok(repository.save(current));
        } else {
            config.setUpdatedBy(auth != null ? auth.getName() : null);
            return ResponseEntity.ok(repository.save(config));
        }
    }

    /**
     * Get preset configurations.
     */
    @GetMapping("/presets")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<SchedulingConfig>> getPresets() {
        return ResponseEntity.ok(Arrays.asList(
                SchedulingConfig.express(),
                SchedulingConfig.balanced(),
                SchedulingConfig.efficient()
        ));
    }
}
