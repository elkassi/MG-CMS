package com.lear.MGCMS.controller.scheduling;

import java.util.List;
import java.util.Map;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.payload.scheduling.OptimizationRequest;
import com.lear.MGCMS.payload.scheduling.OptimizationResponse;
import com.lear.MGCMS.payload.scheduling.SequenceLoadResponse;
import com.lear.MGCMS.services.UserService;
import com.lear.MGCMS.services.scheduling.SchedulingOptimizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduling")
public class SchedulingController {

    @Autowired
    private SchedulingOptimizationService schedulingService;

    @Autowired
    private UserService userService;

    /**
     * Load sequences for selected machines
     * GET /api/scheduling/loadSequences?zoneName=Zone1&machines=AA1,AA2,AA3,AA4
     */
    @GetMapping("/loadSequences")
    public ResponseEntity<List<SequenceLoadResponse>> loadSequences(
            @RequestParam String zoneName,
            @RequestParam List<String> machines
    ) {
        List<SequenceLoadResponse> sequences = schedulingService.loadSequences(zoneName, machines);
        return ResponseEntity.ok(sequences);
    }

    /**
     * Run optimization on selected sequences
     * POST /api/scheduling/optimize
     */
    @PostMapping("/optimize")
    public ResponseEntity<OptimizationResponse> optimize(
            @RequestBody OptimizationRequest request,
            Authentication authentication
    ) {
        User user = userService.findByUsername(authentication.getName());
        OptimizationResponse response = schedulingService.optimize(request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Get optimization status (for polling)
     * GET /api/scheduling/status/{planId}
     */
    @GetMapping("/status/{planId}")
    public ResponseEntity<Map<String, Object>> getOptimizationStatus(@PathVariable String planId) {
        Map<String, Object> status = schedulingService.getOptimizationStatus(planId);
        return ResponseEntity.ok(status);
    }

    /**
     * Stop a running optimization
     * POST /api/scheduling/stop/{planId}
     */
    @PostMapping("/stop/{planId}")
    public ResponseEntity<Map<String, Object>> stopOptimization(@PathVariable String planId) {
        boolean stopped = schedulingService.stopOptimization(planId);
        Map<String, Object> result = Map.of(
            "success", stopped,
            "message", stopped ? "Optimization stopped" : "No running optimization found"
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Check for changes in data that require re-optimization
     * GET /api/scheduling/checkChanges/{planId}
     */
    @GetMapping("/checkChanges/{planId}")
    public ResponseEntity<Map<String, Object>> checkForChanges(@PathVariable String planId) {
        Map<String, Object> changes = schedulingService.checkForChanges(planId);
        return ResponseEntity.ok(changes);
    }

    /**
     * Get all active plans across all zones (global view)
     * GET /api/scheduling/globalPlans
     */
    @GetMapping("/globalPlans")
    public ResponseEntity<List<OptimizationResponse>> getGlobalPlans() {
        List<OptimizationResponse> plans = schedulingService.getGlobalPlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * Get a specific plan by ID
     * GET /api/scheduling/plan/{planId}
     */
    @GetMapping("/plan/{planId}")
    public ResponseEntity<OptimizationResponse> getPlan(@PathVariable String planId) {
        OptimizationResponse plan = schedulingService.getPlan(planId);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plan);
    }

    /**
     * Delete a plan
     * DELETE /api/scheduling/plan/{planId}
     */
    @DeleteMapping("/plan/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable String planId) {
        schedulingService.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Re-optimize with updated parameters
     * POST /api/scheduling/reoptimize/{planId}
     */
    @PostMapping("/reoptimize/{planId}")
    public ResponseEntity<OptimizationResponse> reoptimize(
            @PathVariable String planId,
            @RequestBody OptimizationRequest request,
            Authentication authentication
    ) {
        // Delete the old plan first
        schedulingService.deletePlan(planId);
        
        // Run new optimization
        User user = userService.findByUsername(authentication.getName());
        OptimizationResponse response = schedulingService.optimize(request, user);
        return ResponseEntity.ok(response);
    }
}
