package com.lear.MGCMS.controller.dispatcher;

import com.lear.MGCMS.services.dispatcher.MaterialAvailabilityChecker;
import com.lear.MGCMS.services.dispatcher.MaterialAvailabilityChecker.MaterialStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST endpoint for material availability preview.
 *
 * <p>Returns per-fabric status within a zone: AVAILABLE_IN_ZONE, NEEDS_TRANSFER, or MISSING.</p>
 */
@RestController
@RequestMapping("/api/material")
public class MaterialPreviewController {

    @Autowired
    private MaterialAvailabilityChecker materialAvailabilityChecker;

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('PROCESS','CHEF_DE_ZONE','CHEF_EQUIPE','ADMIN')")
    public ResponseEntity<?> preview(@RequestBody MaterialPreviewRequest request) {
        if (request.refTissus == null || request.refTissus.isEmpty() || request.zoneNom == null || request.zoneNom.isBlank()) {
            return ResponseEntity.badRequest().body("refTissus and zoneNom are required");
        }

        Map<String, MaterialStatus> result = materialAvailabilityChecker.check(request.refTissus, request.zoneNom);

        Map<String, String> out = result.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().name()
                ));

        return ResponseEntity.ok(out);
    }

    public static class MaterialPreviewRequest {
        public Set<String> refTissus;
        public String zoneNom;
    }
}
