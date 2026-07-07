package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.MachineQueue;
import com.lear.MGCMS.services.OrdonnancementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ordonnancement")
public class OrdonnancementController {

    @Autowired
    private OrdonnancementService service;

    /**
     * Get full timeline data for the Gantt view.
     * Smart loading: only series from relevant sequences (24h activity + due shifts).
     * User can add extra sequences via additionalSequences param (comma-separated).
     */
    @GetMapping("/timeline")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getTimeline(
            @RequestParam(defaultValue = "12") int hoursBack,
            @RequestParam(defaultValue = "12") int hoursForward,
            @RequestParam(required = false) String additionalSequences) {
        List<String> extraSeqs = parseSequenceList(additionalSequences);
        return ResponseEntity.ok(service.getTimelineData(hoursBack, hoursForward, extraSeqs));
    }

    /**
     * Incremental refresh: only reloads if changes detected in last N minutes.
     * Returns { noChanges: true } if nothing changed, otherwise full timeline data.
     */
    @GetMapping("/timeline/refresh")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshTimeline(
            @RequestParam(defaultValue = "5") int sinceMinutes,
            @RequestParam(defaultValue = "12") int hoursBack,
            @RequestParam(defaultValue = "12") int hoursForward,
            @RequestParam(required = false) String additionalSequences) {
        List<String> extraSeqs = parseSequenceList(additionalSequences);
        return ResponseEntity.ok(service.getTimelineRefresh(sinceMinutes, hoursBack, hoursForward, extraSeqs));
    }

    /**
     * Load series for a specific sequence (manual user addition).
     */
    @GetMapping("/timeline/sequence/{sequence}")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> loadSequence(@PathVariable String sequence) {
        return ResponseEntity.ok(service.loadSeriesForSequence(sequence));
    }

    /**
     * Get current state of all machines (occupancy, queue, status).
     */
    @GetMapping("/machineState")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getMachineState() {
        return ResponseEntity.ok(service.getMachineStates());
    }

    /**
     * Get automatic dispatching recommendation.
     * Returns collision-free schedule with realistic estimated dates.
     * @param algorithm Optional: SCG, SPT, LPT, EDF, CR, WSPT, ATC, MATERIAL_GROUP
     */
    @GetMapping("/recommendation")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getRecommendation(
            @RequestParam(required = false) String algorithm) {
        return ResponseEntity.ok(service.autoDispatch(algorithm));
    }

    /**
     * Get the list of available dispatch algorithms.
     */
    @GetMapping("/algorithms")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<Map<String, String>>> getAlgorithms() {
        List<Map<String, String>> algorithms = Arrays.asList(
            algorithmInfo("SCG", "Sequence Compaction Greedy", "Prioritizes finishing started sequences, then by due date. Best for box completion speed."),
            algorithmInfo("SPT", "Shortest Processing Time", "Shortest series first. Maximizes throughput (series completed per hour)."),
            algorithmInfo("LPT", "Longest Processing Time", "Longest series first. Better load balancing across machines."),
            algorithmInfo("EDF", "Earliest Due Date First", "Most urgent sequences first. Minimizes late deliveries."),
            algorithmInfo("CR", "Critical Ratio", "Urgency relative to remaining work. CR < 1 = will be late."),
            algorithmInfo("WSPT", "Weighted Shortest Processing Time", "SPT weighted by sequence priority. For prioritized scheduling."),
            algorithmInfo("ATC", "Apparent Tardiness Cost", "Combines urgency + efficiency. Best for mixed workloads."),
            algorithmInfo("MATERIAL_GROUP", "Material Grouping", "Groups same-material series. Minimizes material changeovers.")
        );
        return ResponseEntity.ok(algorithms);
    }

    private Map<String, String> algorithmInfo(String key, String name, String description) {
        Map<String, String> info = new java.util.LinkedHashMap<>();
        info.put("key", key);
        info.put("name", name);
        info.put("description", description);
        return info;
    }

    /**
     * Get full detail of a sequence: header info + all series with dates.
     */
    @GetMapping("/sequence/detail/{sequence}")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getSequenceDetail(@PathVariable String sequence) {
        return ResponseEntity.ok(service.getSequenceDetail(sequence));
    }

    /**
     * Manually assign a serie to a machine.
     */
    @PostMapping("/assignSerie")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> assignSerie(
            @RequestParam String serieId,
            @RequestParam String machineNom) {
        return ResponseEntity.ok(service.assignSerieToMachine(serieId, machineNom));
    }

    /**
     * Move a sequence to a different zone.
     */
    @PostMapping("/assignSequenceZone")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> assignSequenceZone(
            @RequestParam String sequenceId,
            @RequestParam String zoneName) {
        return ResponseEntity.ok(service.assignSequenceToZone(sequenceId, zoneName));
    }

    /**
     * Change a machine's zone assignment.
     */
    @PostMapping("/changeMachineZone")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> changeMachineZone(
            @RequestParam String machineNom,
            @RequestParam String zoneName) {
        return ResponseEntity.ok(service.changeMachineZone(machineNom, zoneName));
    }

    /**
     * Get all available zone names (for dropdown).
     */
    @GetMapping("/zones")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<String>> getZones() {
        return ResponseEntity.ok(service.getAllZoneNames());
    }

    /**
     * Save next 3 series per machine to MachineQueue entity.
     */
    @PostMapping("/saveQueue")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> saveQueue(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(service.saveQueues(username));
    }

    /**
     * Get saved queue for a specific machine.
     */
    @GetMapping("/queue/{machineNom}")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<MachineQueue>> getQueue(@PathVariable String machineNom) {
        return ResponseEntity.ok(service.getQueueForMachine(machineNom));
    }

    /**
     * Get saved queues for all machines.
     */
    @GetMapping("/queue/all")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, List<MachineQueue>>> getAllQueues() {
        return ResponseEntity.ok(service.getAllQueues());
    }

    /**
     * Change the status of a serie (statusMatelassage or statusCoupe).
     * Use this to mark a serie as Incomplete (not enough material) or restore it.
     */
    @PostMapping("/changeSerieStatus")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> changeSerieStatus(
            @RequestParam String serieId,
            @RequestParam String field,
            @RequestParam String newStatus) {
        return ResponseEntity.ok(service.changeSerieStatus(serieId, field, newStatus));
    }

    /**
     * Get machine schedule for production form.
     * Returns next 3 Waiting series + last finish time of non-Waiting series.
     */
    @GetMapping("/machineSchedule/{machineNom}")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getMachineSchedule(@PathVariable String machineNom) {
        return ResponseEntity.ok(service.getMachineSchedule(machineNom));
    }

    /**
     * Get current dispatch engine state.
     */
    @GetMapping("/dispatchEngineState")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDispatchEngineState() {
        return ResponseEntity.ok(service.getDispatchEngineState());
    }

    /**
     * Get sequences with missing materials.
     */
    @GetMapping("/materialAlerts")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getMaterialAlerts() {
        return ResponseEntity.ok(service.getMaterialAlerts());
    }

    /**
     * Parse comma-separated sequence list from query param.
     */
    private List<String> parseSequenceList(String param) {
        if (param == null || param.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(param.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
