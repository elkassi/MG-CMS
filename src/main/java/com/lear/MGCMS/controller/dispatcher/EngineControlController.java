package com.lear.MGCMS.controller.dispatcher;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.EngineProperties;
import com.lear.MGCMS.services.dispatcher.EngineTickService;

/**
 * Runtime control surface for the Phase 7 ordonnancement engine.
 *
 * <p>Lets ops start / stop the auto-tick scheduler at runtime, peek at the
 * current flag state, and trigger a one-off dispatch for a given
 * (date, shift) without waiting for the cron. Mutates
 * {@link EngineProperties} directly — Spring's
 * {@code @ConfigurationProperties} beans are plain singletons, so flipping
 * a setter is enough; the next {@code @Scheduled} firing reads the new
 * value. Restart wipes the override.</p>
 *
 * <p>Returns 404 when the dispatcher master flag is off, mirroring the
 * pattern used by {@link DispatcherController}.</p>
 */
@RestController
@RequestMapping("/api/engine")
public class EngineControlController {

    @Autowired private DispatcherProperties dispatcherProperties;
    @Autowired private EngineProperties engineProperties;
    @Autowired private EngineTickService engineTick;

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dispatcherEnabled", dispatcherProperties.isEnabled());
        body.put("zoneAware", engineProperties.isZoneAware());
        body.put("autoTickEnabled", engineProperties.getAutoTick().isEnabled());
        body.put("autoTickCron", engineProperties.getAutoTick().getCron());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_EQUIPE','ADMIN')")
    public ResponseEntity<?> start() {
        if (!dispatcherProperties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "DISPATCHER_DISABLED",
                           "message", "Le dispatcher est désactivé (mgcms.dispatcher.enabled=false). Activez-le d'abord."));
        }
        if (!engineProperties.isZoneAware()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "ENGINE_NOT_ZONE_AWARE",
                           "message", "L'engine n'est pas en mode zone-aware (mgcms.engine.zone-aware=false)."));
        }
        engineProperties.getAutoTick().setEnabled(true);
        return ResponseEntity.ok(Map.of(
                "autoTickEnabled", true,
                "message", "Auto-tick démarré. La prochaine itération suit le cron " + engineProperties.getAutoTick().getCron()));
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_EQUIPE','ADMIN')")
    public ResponseEntity<?> stop() {
        engineProperties.getAutoTick().setEnabled(false);
        return ResponseEntity.ok(Map.of(
                "autoTickEnabled", false,
                "message", "Auto-tick arrêté. Le cron ne dispatchera plus jusqu'à un nouveau Start."));
    }

    /**
     * Trigger a single dispatch + version-bump cycle for the requested
     * (date, shift). Useful when ops wants to push a fresh result right
     * after fixing a confirmation, without waiting for the next cron tick.
     */
    @PostMapping("/runOnce")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_EQUIPE','ADMIN')")
    public ResponseEntity<?> runOnce(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int shift) {
        if (!dispatcherProperties.isEnabled() || !engineProperties.isZoneAware()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "ENGINE_NOT_READY",
                           "message", "Activez mgcms.dispatcher.enabled et mgcms.engine.zone-aware avant Run-Once."));
        }
        int published = engineTick.autoDispatch(date, shift);
        return ResponseEntity.ok(Map.of(
                "date", date.toString(),
                "shift", shift,
                "publishedCount", published,
                "message", "Run-Once terminé : " + published + " séquence(s) republiée(s)."));
    }
}
