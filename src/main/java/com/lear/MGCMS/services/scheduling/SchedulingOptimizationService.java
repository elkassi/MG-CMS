package com.lear.MGCMS.services.scheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.scheduling.OptimizedPlan;
import com.lear.MGCMS.domain.scheduling.OptimizedSeriesAssignment;
import com.lear.MGCMS.payload.scheduling.OptimizationRequest;
import com.lear.MGCMS.payload.scheduling.OptimizationResponse;
import com.lear.MGCMS.payload.scheduling.SequenceLoadResponse;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.scheduling.OptimizedPlanRepository;
import com.lear.MGCMS.repositories.scheduling.OptimizedSeriesAssignmentRepository;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestDataService;
import com.lear.MGCMS.services.CuttingRequest.data.CuttingRequestSerieDataService;
import com.lear.MGCMS.services.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulingOptimizationService {

    @Autowired
    private OptimizedPlanRepository planRepository;

    @Autowired
    private OptimizedSeriesAssignmentRepository assignmentRepository;

    @Autowired
    private CuttingRequestDataService cuttingRequestDataService;

    @Autowired
    private CuttingRequestSerieDataService serieDataService;

    @Autowired
    private ProductionTableRepository productionTableRepository;

    @Autowired
    private QueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    // Track running Python processes
    private static final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private static final Map<String, Integer> processIterations = new ConcurrentHashMap<>();
    private static final Map<String, String> processStatus = new ConcurrentHashMap<>();
    
    // Track stop requests for genetic algorithm
    private static final Map<String, Boolean> stopRequested = new ConcurrentHashMap<>();

    // Spreading time estimation constants (same as OrdonnancementService)
    private static final double COEF_SPREADING_PER_METRE = 0.5;
    private static final double COEF_SETUP_TIME = 2.0;

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${optimization.script.path:scripts/optimizer.py}")
    private String optimizerScriptPath;

    /**
     * Load sequences for the given machines
     */
    public List<SequenceLoadResponse> loadSequences(String zoneName, List<String> machines) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(8);
        List<CuttingRequestData> sequences = queryService.notCompletedSequenced(zoneName, machines, threshold);
        
        List<SequenceLoadResponse> responses = new ArrayList<>();
        
        for (CuttingRequestData seq : sequences) {
            SequenceLoadResponse response = new SequenceLoadResponse();
            response.setSequenceId(seq.getSequence());
            response.setModele(seq.getModele());
            response.setDueDate(seq.getDueDate() != null ? seq.getDueDate().toString() : null);
            response.setDueShift(seq.getDueShift());
            
            // Get series for this sequence
            List<CuttingRequestSerieData> seriesList = serieDataService.findBySequence(seq.getSequence());
            
            // Calculate sequence status
            boolean allComplete = seriesList.stream().allMatch(s -> "Complete".equals(s.getStatusCoupe()));
            boolean anyStarted = seriesList.stream().anyMatch(s -> 
                "In progress".equals(s.getStatusMatelassage()) || 
                "Complete".equals(s.getStatusMatelassage()) ||
                "In progress".equals(s.getStatusCoupe())
            );
            
            if (allComplete) {
                response.setStatus("FINISHED");
            } else if (anyStarted) {
                response.setStatus("IN_PROGRESS");
            } else {
                response.setStatus("NOT_STARTED");
            }
            
            // Get timing estimations
            List<String> placements = seriesList.stream()
                .map(CuttingRequestSerieData::getPlacement)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
            
            Map<String, Double> timingMap = getTimingEstimations(placements);
            
            // Build series info
            List<SequenceLoadResponse.SerieInfo> seriesInfoList = new ArrayList<>();
            LocalDateTime minStart = null;
            LocalDateTime maxEnd = null;
            
            for (CuttingRequestSerieData serie : seriesList) {
                SequenceLoadResponse.SerieInfo info = new SequenceLoadResponse.SerieInfo();
                info.setSerieId(serie.getSerie());
                info.setPlacement(serie.getPlacement());
                info.setPartNumberMaterial(serie.getPartNumberMaterial());
                info.setMachine(serie.getMachine());
                info.setStatusMatelassage(serie.getStatusMatelassage());
                info.setStatusCoupe(serie.getStatusCoupe());
                info.setTableMatelassage(serie.getTableMatelassage());
                info.setTableCoupe(serie.getTableCoupe());
                info.setDateDebutMatelassage(serie.getDateDebutMatelassage());
                info.setDateFinMatelassage(serie.getDateFinMatelassage());
                info.setDateDebutCoupe(serie.getDateDebutCoupe());
                info.setDateFinCoupe(serie.getDateFinCoupe());
                
                // Get cutting time
                Double cuttingTime = timingMap.getOrDefault(serie.getPlacement(), 0.0);
                if (serie.getTempsDeCoupe() != null && serie.getTempsDeCoupe() > 0) {
                    cuttingTime = serie.getTempsDeCoupe();
                }
                info.setCuttingTimeMinutes(cuttingTime);
                
                // Determine if locked (already in progress or completed)
                info.setIsLocked("In progress".equals(serie.getStatusCoupe()) || "Complete".equals(serie.getStatusCoupe()));
                
                // Track min/max dates
                LocalDateTime start = serie.getDateDebutCoupe();
                LocalDateTime end = serie.getDateFinCoupe();
                if (start != null && (minStart == null || start.isBefore(minStart))) {
                    minStart = start;
                }
                if (end != null && (maxEnd == null || end.isAfter(maxEnd))) {
                    maxEnd = end;
                }
                
                seriesInfoList.add(info);
            }
            
            response.setSeries(seriesInfoList);
            response.setMinStartDate(minStart);
            response.setMaxEndDate(maxEnd);
            
            if (minStart != null && maxEnd != null) {
                double hours = Duration.between(minStart, maxEnd).toMinutes() / 60.0;
                response.setDurationHours(hours);
            }
            
            responses.add(response);
        }
        
        return responses;
    }

    /**
     * Get timing estimations for placements
     */
    private Map<String, Double> getTimingEstimations(List<String> placements) {
        Map<String, Double> timingMap = new HashMap<>();
        if (placements.isEmpty()) return timingMap;
        
        try {
            List<com.lear.MGCMS.payload.TimingPlacement> timings = queryService.getTimingPlacement(placements);
            for (com.lear.MGCMS.payload.TimingPlacement timing : timings) {
                String placement = timing.getPlacementTimingModel();
                Double validatedTime = timing.getValidatedCuttingTimeTimingModel() != null ? 
                    timing.getValidatedCuttingTimeTimingModel() : 0.0;
                Double estimatedTime = timing.getCuttingTimeTimingModel() != null ?
                    timing.getCuttingTimeTimingModel() : 0.0;
                timingMap.put(placement, validatedTime > 0 ? validatedTime : estimatedTime);
            }
        } catch (Exception e) {
            // Log error but continue
        }
        
        return timingMap;
    }

    /**
     * Run optimization on selected sequences
     */
    @Transactional
    public OptimizationResponse optimize(OptimizationRequest request, User user) {
        String planId = UUID.randomUUID().toString();
        
        // Create plan record
        OptimizedPlan plan = new OptimizedPlan();
        plan.setPlanId(planId);
        plan.setZoneName(request.getZoneName());
        plan.setMachineNames(String.join(",", request.getMachineNames()));
        plan.setMachineCount(request.getMachineNames().size());
        plan.setMaxBoxes(request.getMaxBoxes() != null ? request.getMaxBoxes() : request.getMachineNames().size() * 16);
        plan.setCreatedBy(user);
        plan.setStatus("RUNNING");
        plan.setProgress(0);
        
        try {
            plan.setSequenceIds(objectMapper.writeValueAsString(request.getSequenceIds()));
            if (request.getParams() != null) {
                plan.setOptimizationParams(objectMapper.writeValueAsString(request.getParams()));
            }
        } catch (JsonProcessingException e) {
            plan.setSequenceIds(String.join(",", request.getSequenceIds()));
        }
        
        plan = planRepository.save(plan);
        
        // Run optimization asynchronously
        runOptimizationAsync(plan, request);
        
        // Return initial response
        OptimizationResponse response = new OptimizationResponse();
        response.setPlanId(planId);
        response.setStatus("RUNNING");
        response.setProgress(0);
        
        return response;
    }

    /**
     * Run optimization asynchronously
     */
    @Async
    public void runOptimizationAsync(OptimizedPlan plan, OptimizationRequest request) {
        try {
            // Get all series for selected sequences
            List<CuttingRequestSerieData> allSeries = new ArrayList<>();
            for (String seqId : request.getSequenceIds()) {
                allSeries.addAll(serieDataService.findBySequence(seqId));
            }
            
            // Get machines
            List<ProductionTable> machines = productionTableRepository.findByNomIn(request.getMachineNames());
            
            // Get timing estimations
            List<String> placements = allSeries.stream()
                .map(CuttingRequestSerieData::getPlacement)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
            Map<String, Double> timingMap = getTimingEstimations(placements);
            
            // Separate locked (in progress/completed) from schedulable series
            List<CuttingRequestSerieData> lockedSeries = allSeries.stream()
                .filter(s -> "In progress".equals(s.getStatusCoupe()) || "Complete".equals(s.getStatusCoupe()))
                .collect(Collectors.toList());
            
            List<CuttingRequestSerieData> schedulableSeries = allSeries.stream()
                .filter(s -> "Waiting".equals(s.getStatusCoupe()))
                .collect(Collectors.toList());
            
            // Also check if user specified locked series
            if (request.getLockedSeries() != null && !request.getLockedSeries().isEmpty()) {
                Set<String> lockedIds = new HashSet<>(request.getLockedSeries());
                for (CuttingRequestSerieData serie : schedulableSeries) {
                    if (lockedIds.contains(serie.getSerie())) {
                        lockedSeries.add(serie);
                    }
                }
                schedulableSeries = schedulableSeries.stream()
                    .filter(s -> !lockedIds.contains(s.getSerie()))
                    .collect(Collectors.toList());
            }
            
            // Run the scheduling algorithm
            List<OptimizedSeriesAssignment> assignments = runSchedulingAlgorithm(
                plan, schedulableSeries, lockedSeries, machines, timingMap, request
            );
            
            // Save assignments
            assignmentRepository.saveAll(assignments);
            
            // Calculate summary metrics
            LocalDateTime minStart = assignments.stream()
                .map(OptimizedSeriesAssignment::getScheduledStart)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            LocalDateTime maxEnd = assignments.stream()
                .map(OptimizedSeriesAssignment::getScheduledEnd)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            double totalCuttingTime = assignments.stream()
                .mapToDouble(a -> a.getCuttingDurationMinutes() != null ? a.getCuttingDurationMinutes() : 0)
                .sum();
            
            // Update plan
            plan.setMinStartDate(minStart);
            plan.setMaxEndDate(maxEnd);
            plan.setMaxDurationMinutes((double) Duration.between(minStart, maxEnd).toMinutes());
            plan.setTotalCuttingTime(totalCuttingTime);
            plan.setStatus("COMPLETED");
            plan.setProgress(100);
            plan.setUpdatedAt(LocalDateTime.now());
            plan.setIsActive(true);
            
            // Deactivate other plans for this zone
            List<OptimizedPlan> activePlans = planRepository.findByZoneNameAndIsActiveTrue(request.getZoneName());
            for (OptimizedPlan activePlan : activePlans) {
                if (!activePlan.getPlanId().equals(plan.getPlanId())) {
                    activePlan.setIsActive(false);
                    planRepository.save(activePlan);
                }
            }
            
            // Build and save machine assignments JSON
            Map<String, List<Map<String, Object>>> machineAssignments = new HashMap<>();
            for (OptimizedSeriesAssignment a : assignments) {
                machineAssignments.computeIfAbsent(a.getMachineName(), k -> new ArrayList<>());
                Map<String, Object> entry = new HashMap<>();
                entry.put("serieId", a.getSerieId());
                entry.put("sequenceId", a.getSequenceId());
                entry.put("start", a.getScheduledStart().toString());
                entry.put("end", a.getScheduledEnd().toString());
                entry.put("duration", a.getCuttingDurationMinutes());
                machineAssignments.get(a.getMachineName()).add(entry);
            }
            try {
                plan.setMachineAssignments(objectMapper.writeValueAsString(machineAssignments));
            } catch (JsonProcessingException e) {
                // Ignore
            }
            
            planRepository.save(plan);
            
            // Plan is now complete - can be polled via getPlan
            
        } catch (Exception e) {
            plan.setStatus("FAILED");
            plan.setErrorMessage(e.getMessage());
            plan.setUpdatedAt(LocalDateTime.now());
            planRepository.save(plan);
        }
    }

    /**
     * Get optimization status for a plan (for polling)
     */
    public Map<String, Object> getOptimizationStatus(String planId) {
        Map<String, Object> status = new HashMap<>();
        
        // Check if there's a running Python process
        Process process = runningProcesses.get(planId);
        if (process != null && process.isAlive()) {
            status.put("pythonRunning", true);
            status.put("iterations", processIterations.getOrDefault(planId, 0));
            status.put("status", processStatus.getOrDefault(planId, "RUNNING"));
        } else {
            status.put("pythonRunning", false);
            // Get from database
            Optional<OptimizedPlan> planOpt = planRepository.findByPlanId(planId);
            if (planOpt.isPresent()) {
                OptimizedPlan plan = planOpt.get();
                status.put("status", plan.getStatus());
                status.put("progress", plan.getProgress());
                status.put("iterations", plan.getIterationCount());
                status.put("bestScore", plan.getOptimizationScore());
            }
        }
        return status;
    }

    /**
     * Stop a running optimization (Python or Java genetic algorithm)
     */
    public boolean stopOptimization(String planId) {
        // Stop Python process if running
        Process process = runningProcesses.get(planId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningProcesses.remove(planId);
            processIterations.remove(planId);
            processStatus.put(planId, "STOPPED");
            
            // Update plan status
            planRepository.findByPlanId(planId).ifPresent(plan -> {
                plan.setStatus("STOPPED");
                plan.setUpdatedAt(LocalDateTime.now());
                planRepository.save(plan);
            });
            return true;
        }
        
        // Signal genetic algorithm to stop
        stopRequested.put(planId, true);
        
        // Check if there's a running plan
        Optional<OptimizedPlan> planOpt = planRepository.findByPlanId(planId);
        if (planOpt.isPresent() && "RUNNING".equals(planOpt.get().getStatus())) {
            // The genetic algorithm will check the stopRequested flag and stop gracefully
            return true;
        }
        
        return false;
    }

    /**
     * Check for changes in data that require re-optimization
     */
    public Map<String, Object> checkForChanges(String planId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasChanges", false);
        
        Optional<OptimizedPlan> planOpt = planRepository.findByPlanId(planId);
        if (planOpt.isEmpty()) {
            return result;
        }
        
        OptimizedPlan plan = planOpt.get();
        List<OptimizedSeriesAssignment> assignments = assignmentRepository.findByOptimizedPlanPlanId(planId);
        
        List<String> changedSeries = new ArrayList<>();
        for (OptimizedSeriesAssignment assignment : assignments) {
            // Skip locked series
            if (Boolean.TRUE.equals(assignment.getIsLocked())) continue;
            
            // Check if series status changed (e.g., another series was cut instead)
            CuttingRequestSerieData currentSerie = serieDataService.findById(assignment.getSerieId());
            if (currentSerie != null) {
                String currentStatus = currentSerie.getStatusCoupe();
                if ("Complete".equals(currentStatus) || "In progress".equals(currentStatus)) {
                    // This series was cut or is being cut
                    if (!Boolean.TRUE.equals(assignment.getIsLocked())) {
                        changedSeries.add(assignment.getSerieId());
                    }
                }
            }
        }
        
        if (!changedSeries.isEmpty()) {
            result.put("hasChanges", true);
            result.put("changedSeries", changedSeries);
            result.put("message", changedSeries.size() + " series have been cut or are in progress");
        }
        
        return result;
    }

    /**
     * Main scheduling algorithm - Genetic Algorithm optimization
     * Goal: Minimize composite(max, median, stdDev) of
     *       (max dateFinCoupe - min dateDebutMatelassage) / sequence_boxes
     * 
     * This uses a true genetic algorithm with:
     * - Population of machine assignments
     * - Tournament selection
     * - Crossover and mutation
     * - Adaptive mutation rate
     * - 10 minute timeout, 1 million iterations max
     */
    private List<OptimizedSeriesAssignment> runSchedulingAlgorithm(
            OptimizedPlan plan,
            List<CuttingRequestSerieData> schedulableSeries,
            List<CuttingRequestSerieData> lockedSeries,
            List<ProductionTable> machines,
            Map<String, Double> timingMap,
            OptimizationRequest request
    ) {
        // Get max iterations from request or use default (100 million)
        int maxIterations = 100000000;
        if (request.getParams() != null && request.getParams().getMaxIterations() != null) {
            maxIterations = request.getParams().getMaxIterations();
        }
        
        // Get timeout in seconds (default 15 minutes = 900 seconds)
        int timeoutSeconds = 900;
        if (request.getParams() != null && request.getParams().getTimeoutSeconds() != null) {
            timeoutSeconds = request.getParams().getTimeoutSeconds();
        }
        
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        
        // Initialize machine slots with locked series
        Map<String, List<TimeSlot>> initialSlots = new HashMap<>();
        List<OptimizedSeriesAssignment> lockedAssignments = new ArrayList<>();
        
        for (ProductionTable machine : machines) {
            initialSlots.put(machine.getNom(), new ArrayList<>());
        }
        
        // Add locked series to their respective machines
        int orderCounter = 0;
        for (CuttingRequestSerieData serie : lockedSeries) {
            String machineName = serie.getTableCoupe() != null ? serie.getTableCoupe() : serie.getTableMatelassage();
            if (machineName == null || !initialSlots.containsKey(machineName)) continue;
            
            LocalDateTime start = serie.getDateDebutCoupe();
            LocalDateTime end = serie.getDateFinCoupe();
            
            // If no end date, calculate from cutting time
            if (start != null && end == null) {
                double cuttingTime = timingMap.getOrDefault(serie.getPlacement(), 0.0);
                if (serie.getTempsDeCoupe() != null && serie.getTempsDeCoupe() > 0) {
                    cuttingTime = serie.getTempsDeCoupe();
                }
                end = start.plusMinutes((long) Math.ceil(cuttingTime));
            }
            
            if (start != null && end != null) {
                initialSlots.get(machineName).add(new TimeSlot(start, end, serie.getSerie()));
                
                double duration = Duration.between(start, end).toMinutes();
                OptimizedSeriesAssignment assignment = createAssignment(
                    plan, serie, machineName, start, end, duration, true, orderCounter++
                );
                lockedAssignments.add(assignment);
            }
        }
        
        // If no series to schedule, return locked only
        if (schedulableSeries.isEmpty()) {
            plan.setIterationCount(0);
            return lockedAssignments;
        }
        
        // Build sequence to series mapping for score calculation
        Map<String, List<CuttingRequestSerieData>> sequenceToSeries = schedulableSeries.stream()
            .collect(Collectors.groupingBy(s -> s.getSequence() != null ? s.getSequence() : "UNKNOWN"));
        
        // Also include locked series in sequence mapping for accurate box count
        for (CuttingRequestSerieData serie : lockedSeries) {
            String seqId = serie.getSequence() != null ? serie.getSequence() : "UNKNOWN";
            sequenceToSeries.computeIfAbsent(seqId, k -> new ArrayList<>()).add(serie);
        }
        
        // Calculate box count per sequence (sum of nbrCouche, fixed during optimization)
        Map<String, Integer> sequenceBoxCount = new HashMap<>();
        for (Map.Entry<String, List<CuttingRequestSerieData>> entry : sequenceToSeries.entrySet()) {
            int totalBoxes = entry.getValue().stream()
                    .mapToInt(s -> s.getNbrCouche() != null ? s.getNbrCouche() : 1)
                    .sum();
            sequenceBoxCount.put(entry.getKey(), totalBoxes);
        }
        
        // Best solution tracking
        List<OptimizedSeriesAssignment> bestAssignments = null;
        double bestScore = Double.MAX_VALUE;
        
        Random random = new Random(System.currentTimeMillis());
        LocalDateTime now = LocalDateTime.now();
        
        int totalSeries = schedulableSeries.size();
        List<String> machineNames = machines.stream().map(ProductionTable::getNom).collect(Collectors.toList());
        int machineCount = machineNames.size();
        
        if (machineCount == 0) {
            return lockedAssignments;
        }
        
        // ============ GENETIC ALGORITHM ============
        // Each individual = array of machine indices (one per serie)
        
        int populationSize = Math.min(200, Math.max(50, totalSeries * 3));
        List<int[]> population = new ArrayList<>();
        double[] fitness = new double[populationSize];
        
        // Initialize population with diverse strategies
        for (int i = 0; i < populationSize; i++) {
            int[] individual = new int[totalSeries];
            
            if (i == 0) {
                // Round-robin assignment
                for (int j = 0; j < totalSeries; j++) {
                    individual[j] = j % machineCount;
                }
            } else if (i == 1) {
                // All on first machine (baseline)
                Arrays.fill(individual, 0);
            } else if (i < populationSize / 5) {
                // Group by sequence on same machine
                Map<String, Integer> seqToMachine = new HashMap<>();
                int machineIdx = 0;
                for (int j = 0; j < totalSeries; j++) {
                    String seqId = schedulableSeries.get(j).getSequence();
                    if (!seqToMachine.containsKey(seqId)) {
                        seqToMachine.put(seqId, machineIdx % machineCount);
                        machineIdx++;
                    }
                    individual[j] = seqToMachine.get(seqId);
                }
            } else if (i < populationSize / 3) {
                // Group by material
                Map<String, Integer> matToMachine = new HashMap<>();
                int machineIdx = 0;
                for (int j = 0; j < totalSeries; j++) {
                    String mat = schedulableSeries.get(j).getPartNumberMaterial();
                    if (mat == null) mat = "UNKNOWN";
                    if (!matToMachine.containsKey(mat)) {
                        matToMachine.put(mat, machineIdx % machineCount);
                        machineIdx++;
                    }
                    individual[j] = matToMachine.get(mat);
                }
            } else {
                // Random assignment
                for (int j = 0; j < totalSeries; j++) {
                    individual[j] = random.nextInt(machineCount);
                }
            }
            
            population.add(individual);
        }
        
        // Evaluate initial population
        for (int i = 0; i < populationSize; i++) {
            fitness[i] = evaluateSolution(population.get(i), schedulableSeries, machineNames, 
                initialSlots, timingMap, sequenceBoxCount, now, lockedSeries);
            
            if (fitness[i] < bestScore) {
                bestScore = fitness[i];
                bestAssignments = buildAssignments(plan, population.get(i), schedulableSeries, 
                    machineNames, initialSlots, timingMap, now, lockedAssignments);
            }
        }
        
        int iteration = 0;
        int iterationsWithoutImprovement = 0;
        int lastReportedProgress = -1;
        long lastProgressUpdate = System.currentTimeMillis();
        boolean wasStopped = false;
        
        // Main optimization loop
        while (iteration < maxIterations) {
            // Check for stop request
            if (Boolean.TRUE.equals(stopRequested.get(plan.getPlanId()))) {
                stopRequested.remove(plan.getPlanId());
                wasStopped = true;
                break;
            }
            
            // Check timeout
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > timeoutMillis) {
                break;
            }
            
            // Tournament selection (select 2 parents)
            int parent1Idx = tournamentSelect(fitness, random, 5);
            int parent2Idx = tournamentSelect(fitness, random, 5);
            
            // Crossover: Uniform crossover
            int[] child = new int[totalSeries];
            for (int j = 0; j < totalSeries; j++) {
                child[j] = random.nextBoolean() ? population.get(parent1Idx)[j] : population.get(parent2Idx)[j];
            }
            
            // Mutation: Adaptive mutation rate (increases when stuck)
            double mutationRate = 0.05 + (0.25 * Math.min(iterationsWithoutImprovement, 10000) / 10000.0);
            mutationRate = Math.min(mutationRate, 0.4);
            
            for (int j = 0; j < totalSeries; j++) {
                if (random.nextDouble() < mutationRate) {
                    int mutationType = random.nextInt(5);
                    
                    switch (mutationType) {
                        case 0: // Random machine
                            child[j] = random.nextInt(machineCount);
                            break;
                        case 1: // Swap with another serie
                            int swapIdx = random.nextInt(totalSeries);
                            int temp = child[j];
                            child[j] = child[swapIdx];
                            child[swapIdx] = temp;
                            break;
                        case 2: // Move to least loaded machine
                            child[j] = findLeastLoadedMachine(child, machineCount);
                            break;
                        case 3: // Move to same machine as same sequence
                            String seqId = schedulableSeries.get(j).getSequence();
                            if (seqId != null) {
                                for (int k = 0; k < totalSeries; k++) {
                                    if (k != j && seqId.equals(schedulableSeries.get(k).getSequence())) {
                                        child[j] = child[k];
                                        break;
                                    }
                                }
                            }
                            break;
                        case 4: // Move to next machine (circular)
                            child[j] = (child[j] + 1) % machineCount;
                            break;
                    }
                }
            }
            
            // Evaluate child
            double childFitness = evaluateSolution(child, schedulableSeries, machineNames, 
                initialSlots, timingMap, sequenceBoxCount, now, lockedSeries);
            
            // Replace worst individual if child is better
            int worstIdx = 0;
            for (int i = 1; i < populationSize; i++) {
                if (fitness[i] > fitness[worstIdx]) {
                    worstIdx = i;
                }
            }
            
            if (childFitness < fitness[worstIdx]) {
                population.set(worstIdx, child);
                fitness[worstIdx] = childFitness;
                
                // Check if this is new best
                if (childFitness < bestScore) {
                    bestScore = childFitness;
                    bestAssignments = buildAssignments(plan, child, schedulableSeries, 
                        machineNames, initialSlots, timingMap, now, lockedAssignments);
                    iterationsWithoutImprovement = 0;
                } else {
                    iterationsWithoutImprovement++;
                }
            } else {
                iterationsWithoutImprovement++;
            }
            
            iteration++;
            
            // Update progress every second or every 10000 iterations
            long now2 = System.currentTimeMillis();
            if (now2 - lastProgressUpdate > 1000 || iteration % 10000 == 0) {
                lastProgressUpdate = now2;
                
                // Progress based on timeout (primary) or iterations (secondary)
                int progressByTime = (int) ((elapsed * 100) / timeoutMillis);
                int progressByIter = (int) ((iteration * 100.0) / maxIterations);
                int progress = Math.max(progressByTime, progressByIter);
                progress = Math.min(progress, 99);
                
                if (progress > lastReportedProgress) {
                    lastReportedProgress = progress;
                    plan.setProgress(progress);
                    plan.setIterationCount(iteration);
                    plan.setOptimizationScore(bestScore);
                    processIterations.put(plan.getPlanId(), iteration);
                    planRepository.save(plan);
                }
            }
            
            // Population restart if stuck for too long
            if (iterationsWithoutImprovement > 50000) {
                // Restart half the population with random individuals
                for (int i = populationSize / 2; i < populationSize; i++) {
                    int[] newIndividual = new int[totalSeries];
                    for (int j = 0; j < totalSeries; j++) {
                        newIndividual[j] = random.nextInt(machineCount);
                    }
                    population.set(i, newIndividual);
                    fitness[i] = evaluateSolution(newIndividual, schedulableSeries, machineNames, 
                        initialSlots, timingMap, sequenceBoxCount, now, lockedSeries);
                    
                    if (fitness[i] < bestScore) {
                        bestScore = fitness[i];
                        bestAssignments = buildAssignments(plan, newIndividual, schedulableSeries, 
                            machineNames, initialSlots, timingMap, now, lockedAssignments);
                    }
                }
                iterationsWithoutImprovement = 0;
            }
        }
        
        // Final update
        plan.setIterationCount(iteration);
        plan.setOptimizationScore(bestScore);
        
        // If stopped, set status to STOPPED so frontend knows
        if (wasStopped) {
            plan.setStatus("STOPPED");
            plan.setUpdatedAt(LocalDateTime.now());
            planRepository.save(plan);
        }
        
        return bestAssignments != null ? bestAssignments : lockedAssignments;
    }
    
    /**
     * Evaluate a solution: minimize composite(max, median, stdDev) of
     * (max dateFinCoupe - min dateDebutMatelassage) / sequence_boxes.
     * Lower score is better.
     */
    private double evaluateSolution(
            int[] machineAssignment,
            List<CuttingRequestSerieData> series,
            List<String> machineNames,
            Map<String, List<TimeSlot>> initialSlots,
            Map<String, Double> timingMap,
            Map<String, Integer> sequenceBoxCount,
            LocalDateTime now,
            List<CuttingRequestSerieData> lockedSeries
    ) {
        // Initialize machine end times from locked slots
        Map<String, LocalDateTime> machineEndTimes = new HashMap<>();
        for (String machine : machineNames) {
            List<TimeSlot> slots = initialSlots.get(machine);
            if (slots != null && !slots.isEmpty()) {
                LocalDateTime maxEnd = slots.stream()
                    .map(s -> s.end)
                    .max(LocalDateTime::compareTo)
                    .orElse(now);
                machineEndTimes.put(machine, maxEnd.isAfter(now) ? maxEnd : now);
            } else {
                machineEndTimes.put(machine, now);
            }
        }
        
        // Track sequence matelassage start and coupe end
        Map<String, LocalDateTime> sequenceMinMatelassageStart = new HashMap<>();
        Map<String, LocalDateTime> sequenceMaxCoupeEnd = new HashMap<>();
        
        // Add locked series times
        for (CuttingRequestSerieData serie : lockedSeries) {
            String seqId = serie.getSequence();
            if (seqId == null) continue;
            
            LocalDateTime matStart = serie.getDateDebutMatelassage();
            if (matStart == null) matStart = serie.getDateDebutCoupe();
            LocalDateTime coupeEnd = serie.getDateFinCoupe();
            
            if (matStart != null) {
                if (!sequenceMinMatelassageStart.containsKey(seqId) || matStart.isBefore(sequenceMinMatelassageStart.get(seqId))) {
                    sequenceMinMatelassageStart.put(seqId, matStart);
                }
            }
            if (coupeEnd != null) {
                if (!sequenceMaxCoupeEnd.containsKey(seqId) || coupeEnd.isAfter(sequenceMaxCoupeEnd.get(seqId))) {
                    sequenceMaxCoupeEnd.put(seqId, coupeEnd);
                }
            }
        }
        
        // Simulate scheduling with this machine assignment (matelassage + coupe sequentially)
        for (int i = 0; i < series.size(); i++) {
            CuttingRequestSerieData serie = series.get(i);
            String machineName = machineNames.get(machineAssignment[i]);
            String seqId = serie.getSequence();
            if (seqId == null) seqId = "UNKNOWN";
            
            double cuttingTime = timingMap.getOrDefault(serie.getPlacement(), 0.0);
            if (serie.getTempsDeCoupe() != null && serie.getTempsDeCoupe() > 0) {
                cuttingTime = serie.getTempsDeCoupe();
            }
            if (cuttingTime <= 0) cuttingTime = 30; // Default 30 min if unknown
            
            double spreadTime = estimateSpreadingTime(serie);
            
            // Schedule matelassage then coupe sequentially on same machine
            LocalDateTime matelassageStart = machineEndTimes.get(machineName);
            LocalDateTime matelassageEnd = matelassageStart.plusMinutes((long) Math.ceil(spreadTime));
            LocalDateTime coupeStart = matelassageEnd;
            LocalDateTime coupeEnd = coupeStart.plusMinutes((long) Math.ceil(cuttingTime));
            
            machineEndTimes.put(machineName, coupeEnd);
            
            if (!sequenceMinMatelassageStart.containsKey(seqId) || matelassageStart.isBefore(sequenceMinMatelassageStart.get(seqId))) {
                sequenceMinMatelassageStart.put(seqId, matelassageStart);
            }
            if (!sequenceMaxCoupeEnd.containsKey(seqId) || coupeEnd.isAfter(sequenceMaxCoupeEnd.get(seqId))) {
                sequenceMaxCoupeEnd.put(seqId, coupeEnd);
            }
        }
        
        // Calculate indicators for all sequences
        List<Double> indicators = new ArrayList<>();
        for (String seqId : sequenceBoxCount.keySet()) {
            LocalDateTime minStart = sequenceMinMatelassageStart.get(seqId);
            LocalDateTime maxEnd = sequenceMaxCoupeEnd.get(seqId);
            int boxes = sequenceBoxCount.get(seqId);
            
            if (minStart != null && maxEnd != null && boxes > 0) {
                double durationMinutes = Duration.between(minStart, maxEnd).toMinutes();
                double indicator = durationMinutes / boxes;
                indicators.add(indicator);
            }
        }
        
        if (indicators.isEmpty()) return Double.MAX_VALUE;
        
        // Compute max, median, and standard deviation
        double maxInd = indicators.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        
        Collections.sort(indicators);
        double medianInd;
        int n = indicators.size();
        if (n % 2 == 1) {
            medianInd = indicators.get(n / 2);
        } else {
            medianInd = (indicators.get(n / 2 - 1) + indicators.get(n / 2)) / 2.0;
        }
        
        double avgInd = indicators.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = indicators.stream().mapToDouble(d -> Math.pow(d - avgInd, 2)).average().orElse(0);
        double stdDevInd = Math.sqrt(variance);
        
        // Composite score: prioritize reducing max, then median, then variance
        return maxInd * 0.5 + medianInd * 0.3 + stdDevInd * 0.2;
    }
    
    /**
     * Tournament selection for genetic algorithm
     */
    private int tournamentSelect(double[] fitness, Random random, int tournamentSize) {
        int bestIdx = random.nextInt(fitness.length);
        
        for (int i = 1; i < tournamentSize; i++) {
            int idx = random.nextInt(fitness.length);
            if (fitness[idx] < fitness[bestIdx]) { // Lower is better
                bestIdx = idx;
            }
        }
        
        return bestIdx;
    }
    
    /**
     * Find the least loaded machine in assignment
     */
    private int findLeastLoadedMachine(int[] assignment, int machineCount) {
        int[] counts = new int[machineCount];
        for (int m : assignment) {
            if (m >= 0 && m < machineCount) {
                counts[m]++;
            }
        }
        
        int minIdx = 0;
        for (int i = 1; i < machineCount; i++) {
            if (counts[i] < counts[minIdx]) {
                minIdx = i;
            }
        }
        
        return minIdx;
    }
    
    /**
     * Build final assignments from solution
     */
    private List<OptimizedSeriesAssignment> buildAssignments(
            OptimizedPlan plan,
            int[] machineAssignment,
            List<CuttingRequestSerieData> series,
            List<String> machineNames,
            Map<String, List<TimeSlot>> initialSlots,
            Map<String, Double> timingMap,
            LocalDateTime now,
            List<OptimizedSeriesAssignment> lockedAssignments
    ) {
        List<OptimizedSeriesAssignment> result = new ArrayList<>(lockedAssignments);
        
        // Initialize machine end times
        Map<String, LocalDateTime> machineEndTimes = new HashMap<>();
        for (String machine : machineNames) {
            List<TimeSlot> slots = initialSlots.get(machine);
            if (slots != null && !slots.isEmpty()) {
                LocalDateTime maxEnd = slots.stream()
                    .map(s -> s.end)
                    .max(LocalDateTime::compareTo)
                    .orElse(now);
                machineEndTimes.put(machine, maxEnd.isAfter(now) ? maxEnd : now);
            } else {
                machineEndTimes.put(machine, now);
            }
        }
        
        int orderCounter = lockedAssignments.size();
        
        for (int i = 0; i < series.size(); i++) {
            CuttingRequestSerieData serie = series.get(i);
            String machineName = machineNames.get(machineAssignment[i]);
            
            double cuttingTime = timingMap.getOrDefault(serie.getPlacement(), 0.0);
            if (serie.getTempsDeCoupe() != null && serie.getTempsDeCoupe() > 0) {
                cuttingTime = serie.getTempsDeCoupe();
            }
            if (cuttingTime <= 0) cuttingTime = 30; // Default 30 min if unknown
            
            double spreadTime = estimateSpreadingTime(serie);
            LocalDateTime matelassageStart = machineEndTimes.get(machineName);
            LocalDateTime matelassageEnd = matelassageStart.plusMinutes((long) Math.ceil(spreadTime));
            LocalDateTime coupeStart = matelassageEnd;
            LocalDateTime coupeEnd = coupeStart.plusMinutes((long) Math.ceil(cuttingTime));
            
            machineEndTimes.put(machineName, coupeEnd);
            
            // Check if machine changed from original
            String originalMachine = serie.getMachine();
            String movementNote = null;
            if (originalMachine != null && !originalMachine.equals(machineName)) {
                movementNote = "Déplacé de " + originalMachine + " vers " + machineName;
            }
            
            OptimizedSeriesAssignment assignment = createAssignment(
                plan, serie, machineName, coupeStart, coupeEnd, cuttingTime, false, orderCounter++
            );
            assignment.setMovementNote(movementNote);
            
            result.add(assignment);
        }
        
        return result;
    }

    /**
     * Find candidate machines based on serie's machine type requirement
     */
    private List<ProductionTable> findCandidateMachines(CuttingRequestSerieData serie, List<ProductionTable> machines) {
        String placement = serie.getPlacement();
        String machine = serie.getMachine();
        
        // Special handling for -0BF placements
        if (placement != null && placement.toUpperCase().contains("-0BF")) {
            if ("100132940".equals(serie.getPartNumberMaterial()) || "A".equals(serie.getConfig())) {
                return machines.stream()
                    .filter(m -> m.getMachineType() != null && "Lectra IP6".equals(m.getMachineType().getName()))
                    .collect(Collectors.toList());
            } else {
                return machines.stream()
                    .filter(m -> m.getMachineType() != null && "Lectra".equals(m.getMachineType().getName()))
                    .collect(Collectors.toList());
            }
        }
        
        // Default: match by machine type
        String typeName = machine != null ? machine : "Lectra";
        return machines.stream()
            .filter(m -> m.getMachineType() != null && typeName.equals(m.getMachineType().getName()))
            .collect(Collectors.toList());
    }

    /**
     * Estimate spreading time for a serie (same formula as OrdonnancementService).
     */
    private double estimateSpreadingTime(CuttingRequestSerieData serie) {
        double longueur = serie.getLongueur() != null ? serie.getLongueur() : 0;
        int nbrCouche = serie.getNbrCouche() != null ? serie.getNbrCouche() : 1;
        return (longueur * nbrCouche * COEF_SPREADING_PER_METRE) + COEF_SETUP_TIME;
    }

    /**
     * Create an assignment record
     */
    private OptimizedSeriesAssignment createAssignment(
            OptimizedPlan plan,
            CuttingRequestSerieData serie,
            String machineName,
            LocalDateTime start,
            LocalDateTime end,
            double cuttingTime,
            boolean isLocked,
            int order
    ) {
        OptimizedSeriesAssignment assignment = new OptimizedSeriesAssignment();
        assignment.setOptimizedPlan(plan);
        assignment.setSerieId(serie.getSerie());
        assignment.setSequenceId(serie.getSequence());
        assignment.setMachineName(machineName);
        assignment.setScheduledStart(start);
        assignment.setScheduledEnd(end);
        assignment.setCuttingDurationMinutes(cuttingTime);
        assignment.setIsLocked(isLocked);
        assignment.setOrderOnMachine(order);
        assignment.setPartNumberMaterial(serie.getPartNumberMaterial());
        assignment.setPlacement(serie.getPlacement());
        return assignment;
    }

    /**
     * Build response from plan and assignments
     */
    private OptimizationResponse buildResponse(OptimizedPlan plan, List<OptimizedSeriesAssignment> assignments) {
        OptimizationResponse response = new OptimizationResponse();
        response.setPlanId(plan.getPlanId());
        response.setStatus(plan.getStatus());
        response.setProgress(plan.getProgress());
        response.setIterationCount(plan.getIterationCount());
        response.setOptimizationScore(plan.getOptimizationScore());
        response.setMinStartDate(plan.getMinStartDate());
        response.setMaxEndDate(plan.getMaxEndDate());
        response.setMaxDurationMinutes(plan.getMaxDurationMinutes());
        response.setMaxDurationHours(plan.getMaxDurationMinutes() != null ? plan.getMaxDurationMinutes() / 60.0 : null);
        response.setTotalCuttingTime(plan.getTotalCuttingTime());
        
        // Build assignments list
        List<OptimizationResponse.SeriesAssignment> assignmentList = new ArrayList<>();
        Map<String, List<OptimizationResponse.SeriesAssignment>> byMachine = new HashMap<>();
        
        for (OptimizedSeriesAssignment a : assignments) {
            OptimizationResponse.SeriesAssignment sa = new OptimizationResponse.SeriesAssignment();
            sa.setSerieId(a.getSerieId());
            sa.setSequenceId(a.getSequenceId());
            sa.setMachineName(a.getMachineName());
            sa.setScheduledStart(a.getScheduledStart());
            sa.setScheduledEnd(a.getScheduledEnd());
            sa.setCuttingDurationMinutes(a.getCuttingDurationMinutes());
            sa.setIsLocked(a.getIsLocked());
            sa.setMovementNote(a.getMovementNote());
            sa.setPartNumberMaterial(a.getPartNumberMaterial());
            sa.setPlacement(a.getPlacement());
            sa.setOrderOnMachine(a.getOrderOnMachine());
            
            assignmentList.add(sa);
            byMachine.computeIfAbsent(a.getMachineName(), k -> new ArrayList<>()).add(sa);
        }
        
        response.setAssignments(assignmentList);
        response.setAssignmentsByMachine(byMachine);
        
        // Build sequence summaries
        Map<String, List<OptimizedSeriesAssignment>> bySequence = assignments.stream()
            .collect(Collectors.groupingBy(OptimizedSeriesAssignment::getSequenceId));
        
        List<OptimizationResponse.SequenceSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<OptimizedSeriesAssignment>> entry : bySequence.entrySet()) {
            OptimizationResponse.SequenceSummary summary = new OptimizationResponse.SequenceSummary();
            summary.setSequenceId(entry.getKey());
            summary.setSeriesCount(entry.getValue().size());
            
            LocalDateTime minStart = entry.getValue().stream()
                .map(OptimizedSeriesAssignment::getScheduledStart)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
            LocalDateTime maxEnd = entry.getValue().stream()
                .map(OptimizedSeriesAssignment::getScheduledEnd)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
            
            summary.setMinStartDate(minStart);
            summary.setMaxEndDate(maxEnd);
            
            if (minStart != null && maxEnd != null) {
                double durationMinutes = Duration.between(minStart, maxEnd).toMinutes();
                summary.setDurationHours(durationMinutes / 60.0);
                summary.setDurationMinutes(durationMinutes);
            }
            
            double totalCutting = entry.getValue().stream()
                .mapToDouble(a -> a.getCuttingDurationMinutes() != null ? a.getCuttingDurationMinutes() : 0)
                .sum();
            summary.setTotalCuttingMinutes(totalCutting);
            
            // Calculate boxCount and durationPerBox (using series count as proxy for display)
            int boxCount = entry.getValue().size();
            summary.setBoxCount(boxCount);
            if (boxCount > 0 && totalCutting > 0) {
                summary.setDurationPerBox(totalCutting / boxCount);
            }
            
            summaries.add(summary);
        }
        
        response.setSequenceSummaries(summaries);
        
        return response;
    }

    /**
     * Get all global plans across all zones
     */
    public List<OptimizationResponse> getGlobalPlans() {
        List<OptimizedPlan> activePlans = planRepository.findAll().stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .collect(Collectors.toList());
        
        return activePlans.stream()
            .map(plan -> {
                List<OptimizedSeriesAssignment> assignments = assignmentRepository.findByOptimizedPlanId(plan.getId());
                return buildResponse(plan, assignments);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get plan by ID
     */
    public OptimizationResponse getPlan(String planId) {
        Optional<OptimizedPlan> planOpt = planRepository.findByPlanId(planId);
        if (planOpt.isEmpty()) {
            return null;
        }
        
        OptimizedPlan plan = planOpt.get();
        List<OptimizedSeriesAssignment> assignments = assignmentRepository.findByOptimizedPlanPlanId(planId);
        return buildResponse(plan, assignments);
    }

    /**
     * Delete a plan
     */
    @Transactional
    public void deletePlan(String planId) {
        Optional<OptimizedPlan> planOpt = planRepository.findByPlanId(planId);
        if (planOpt.isPresent()) {
            assignmentRepository.deleteByOptimizedPlanId(planOpt.get().getId());
            planRepository.delete(planOpt.get());
        }
    }

    /**
     * Helper class for time slots
     */
    private static class TimeSlot {
        LocalDateTime start;
        LocalDateTime end;
        String serieId;

        TimeSlot(LocalDateTime start, LocalDateTime end, String serieId) {
            this.start = start;
            this.end = end;
            this.serieId = serieId;
        }
    }
}
