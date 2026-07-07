package com.lear.MGCMS.controller.dispatcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.LiveChargeDto;
import com.lear.MGCMS.services.dispatcher.LiveChargeService;

/**
 * REST surface for the {@code /processDispatcher} live-charge view.
 * Returns a status-aware snapshot of every active sequence with the full
 * remaining-time breakdown and per-(zone, machineType) capacity math.
 *
 * <p>Same security gating as {@link DispatcherController}: 404 when
 * {@code mgcms.dispatcher.enabled=false}, otherwise open to
 * {@code ROLE_PROCESS}, {@code ROLE_CHEF_DE_ZONE},
 * {@code ROLE_CHEF_EQUIPE} and {@code ROLE_ADMIN}.</p>
 */
@RestController
@RequestMapping("/api/dispatcher")
public class LiveChargeController {

    @Autowired private LiveChargeService liveChargeService;
    @Autowired private DispatcherProperties properties;

    @GetMapping("/liveCharge")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN')")
    public ResponseEntity<LiveChargeDto> liveCharge() {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(liveChargeService.compute());
    }
}
