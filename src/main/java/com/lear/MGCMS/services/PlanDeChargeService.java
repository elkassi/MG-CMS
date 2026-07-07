package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.CapaciteInstallee;
import com.lear.MGCMS.domain.EtatMachineHistorique;
import com.lear.MGCMS.domain.PlanDeChargeSnapshot;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.ShiftLoadCalculation;
import com.lear.MGCMS.repositories.PlanDeChargeSnapshotRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ShiftLoadCalculationRepository;
import com.lear.MGCMS.repositories.CuttingPlan.CuttingPlanRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestSerieInfoRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestPartNumber;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;
import com.lear.cms.domain.TimingModel;
import com.lear.cms.repositories.TimingModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanDeChargeService {

    @Autowired
    private ProductionTableRepository productionTableRepository;

    @Autowired
    private EtatMachineHistoriqueService etatMachineHistoriqueService;

    @Autowired
    private ShiftLoadCalculationRepository shiftLoadCalculationRepository;

    @Autowired
    private TimingModelRepository timingModelRepository;
    
    @Autowired
    private CuttingRequestSerieInfoRepository cuttingRequestSerieInfoRepository;

    @Autowired
    private CapaciteInstalleeService capaciteInstalleeService;

    @Autowired
    private CuttingPlanRepository cuttingPlanRepository;

    @Autowired
    private CuttingTimeCalculator cuttingTimeCalculator;

    @Autowired
    private NonImportedChargeService nonImportedChargeService;

    @Autowired
    private CuttingRequestRepository cuttingRequestRepository;

    @Autowired
    private PerimetreService perimetreService;

    @Autowired
    private PlanDeChargeSnapshotRepository planDeChargeSnapshotRepository;

    // Shift duration in minutes (~7h40min effective work time)
    private static final int SHIFT_DURATION_MINUTES = 460;
    private static final int TIMING_MODEL_BATCH_SIZE = 2000;
    // Default efficiency ratio when no capaciteInstallee row is found (90%)
    private static final double DEFAULT_EFFICIENCY_RATIO = 0.90;

    /**
     * Map a machine type to the capaciteInstallee groupe
     * (Coupe for Lectra/Lectra IP6/Gerber, Laser for LASER-DXF).
     */
    private String getGroupeForMachineType(String machineType) {
        if (machineType == null) return null;
        switch (machineType) {
            case "Lectra":
            case "Lectra IP6":
            case "Gerber":
                return "Coupe";
            case "LASER-DXF":
                return "Laser";
            default:
                return machineType;
        }
    }

    // resolveEfficiencyRatio() was removed: efficiency now lives per-machine on
    // ProductionTable and is applied to the cutting time (numerator) by
    // CuttingTimeCalculator. The PlanDeCharge capacity denominator is therefore
    // raw (see calculateShiftLoad / the frontend getShiftProductiveCapacityMinutes).

    /**
     * Resolve the configured shift minutes for a (date, shift, machineType).
     * Falls back to {@link #SHIFT_DURATION_MINUTES} when no row is configured.
     */
    private int resolveConfiguredShiftMinutes(LocalDate date, int shiftNumber, String machineType) {
        String groupe = getGroupeForMachineType(machineType);
        if (groupe == null) return SHIFT_DURATION_MINUTES;
        CapaciteInstallee cap = capaciteInstalleeService.getEffective(date, shiftNumber, groupe);
        if (cap == null || cap.getTempsTotalParMachine() == null) return SHIFT_DURATION_MINUTES;
        double minutes = cap.getTempsTotalParMachine();
        if (minutes <= 0) return SHIFT_DURATION_MINUTES;
        return (int) Math.round(minutes);
    }

    /**
     * Resolve the global efficience target (%) for a (date, shift, machineType)
     * from CapaciteInstallee (the "efficience global coupe/laser"). Falls back to
     * 90% when no row is configured.
     */
    private double resolveEfficienceTargetPct(LocalDate date, int shiftNumber, String machineType) {
        String groupe = getGroupeForMachineType(machineType);
        if (groupe == null) return 90.0;
        CapaciteInstallee cap = capaciteInstalleeService.getEffective(date, shiftNumber, groupe);
        if (cap == null || cap.getEfficienceTarget() == null || cap.getEfficienceTarget() <= 0) return 90.0;
        return cap.getEfficienceTarget();
    }

    /**
     * Resolve the installed machine count for a (date, shift, machineType).
     * Falls back to 0 if no capaciteInstallee row exists.
     */
    private int resolveInstalledCapacity(LocalDate date, int shiftNumber, String machineType) {
        String groupe = getGroupeForMachineType(machineType);
        if (groupe == null) return 0;
        CapaciteInstallee cap = capaciteInstalleeService.getEffective(date, shiftNumber, groupe);
        if (cap == null || cap.getCapaciteInstallee() == null) return 0;
        return cap.getCapaciteInstallee();
    }

    /**
     * Get shift start and end times for a given date and shift number.
     */
    public Map<String, LocalDateTime> getShiftTimes(LocalDate date, int shiftNumber) {
        Map<String, LocalDateTime> times = new HashMap<>();
        
        switch (shiftNumber) {
            case 1:
                // Shift 1: 21:55 previous day to 05:45 current day
                times.put("start", LocalDateTime.of(date.minusDays(1), LocalTime.of(21, 55)));
                times.put("end", LocalDateTime.of(date, LocalTime.of(5, 45)));
                break;
            case 2:
                // Shift 2: 05:55 to 13:45
                times.put("start", LocalDateTime.of(date, LocalTime.of(5, 55)));
                times.put("end", LocalDateTime.of(date, LocalTime.of(13, 45)));
                break;
            case 3:
                // Shift 3: 13:55 to 21:45
                times.put("start", LocalDateTime.of(date, LocalTime.of(13, 55)));
                times.put("end", LocalDateTime.of(date, LocalTime.of(21, 45)));
                break;
            default:
                throw new IllegalArgumentException("Invalid shift number: " + shiftNumber);
        }
        
        return times;
    }

    /**
     * Get all production tables (machines) grouped by zone.
     */
    public Map<String, List<ProductionTable>> getMachinesGroupedByZone() {
        List<ProductionTable> machines = productionTableRepository.findAll();
        
        return machines.stream()
                .filter(m -> m.getZone() != null)
                .sorted(Comparator
                        .comparing((ProductionTable m) -> m.getZone().getOrderInd(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing((ProductionTable m) -> m.getZone().getNom(), Comparator.nullsLast(String::compareTo))
                        .thenComparing(ProductionTable::getNom, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.groupingBy(
                        m -> m.getZone().getNom(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * Get machine status for a specific date range.
     * Returns a map: machine -> date -> shift -> status code
     */
    public Map<String, Map<LocalDate, Map<Integer, String>>> getMachineStatusGrid(
            LocalDate startDate, LocalDate endDate) {
        
        // Get date range for querying (include previous day for shift 1)
        LocalDateTime rangeStart = LocalDateTime.of(startDate.minusDays(1), LocalTime.of(21, 55));
        LocalDateTime rangeEnd = LocalDateTime.of(endDate, LocalTime.of(21, 45));
        
        // Get all status records for the date range
        List<EtatMachineHistorique> allStatuses = etatMachineHistoriqueService.findByDateRangeOverlap(rangeStart, rangeEnd);
        
        // Group by machine
        Map<String, List<EtatMachineHistorique>> statusByMachine = allStatuses.stream()
                .collect(Collectors.groupingBy(EtatMachineHistorique::getMachine));
        
        // Build the grid
        Map<String, Map<LocalDate, Map<Integer, String>>> grid = new LinkedHashMap<>();
        
        // Get all machines
        List<ProductionTable> machines = productionTableRepository.findAll();
        
        for (ProductionTable machine : machines) {
            String machineName = machine.getNom();
            Map<LocalDate, Map<Integer, String>> dateMap = new LinkedHashMap<>();
            
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                Map<Integer, String> shiftMap = new LinkedHashMap<>();
                
                for (int shift = 1; shift <= 3; shift++) {
                    Map<String, LocalDateTime> shiftTimes = getShiftTimes(currentDate, shift);
                    LocalDateTime shiftMid = shiftTimes.get("start").plusMinutes(SHIFT_DURATION_MINUTES / 2);
                    
                    // Find status at shift midpoint
                    String statusCode = "M"; // Default: Marche
                    
                    List<EtatMachineHistorique> machineStatuses = statusByMachine.get(machineName);
                    if (machineStatuses != null) {
                        for (EtatMachineHistorique status : machineStatuses) {
                            if (status.getStartDate().compareTo(shiftMid) <= 0 &&
                                    (status.getEndDate() == null || status.getEndDate().compareTo(shiftMid) >= 0)) {
                                statusCode = status.getCodeEtat();
                                break;
                            }
                        }
                    }
                    
                    shiftMap.put(shift, statusCode);
                }
                
                dateMap.put(currentDate, shiftMap);
                currentDate = currentDate.plusDays(1);
            }
            
            grid.put(machineName, dateMap);
        }
        
        return grid;
    }

    /**
     * Get detailed series information for a specific date and shift.
     * Returns series with cutting times from TimingModel (validated > real > tempsDeCoupe).
     */
    public List<Map<String, Object>> getDetailedSeriesForShift(LocalDate dueDate, String dueShift) {
        List<Object[]> seriesData = cuttingRequestSerieInfoRepository.findDetailedSeriesForShift(dueDate, dueShift);

        if (seriesData == null || seriesData.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect all placements to batch query TimingModel
        Set<String> placements = new HashSet<>();
        // Collect all cuttingPlanIds to batch-resolve cmsId in a single query
        Set<Long> planIds = new HashSet<>();
        for (Object[] row : seriesData) {
            String placement = row[5] != null ? row[5].toString() : null;
            if (placement != null) {
                placements.add(placement);
            }
            if (row.length > 18 && row[18] != null) {
                planIds.add(((Number) row[18]).longValue());
            }
        }

        // Get cutting times from TimingModel for all placements
        Map<String, Double[]> timingModelData = getTimingModelDataByPlacements(new ArrayList<>(placements));
        Map<String, String> machineTypeMap = getMachineTypeMap();
        Map<String, Double> machineEfficienceMap = getMachineEfficienceMap();
        Map<Long, Long> planIdToCmsId = resolveCmsIdsByPlanIds(planIds);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : seriesData) {
            Map<String, Object> serieInfo = new LinkedHashMap<>();

            String serie = row[0] != null ? row[0].toString() : null;
            String sequence = row[1] != null ? row[1].toString() : null;
            LocalDate date = row[2] != null ? (LocalDate) row[2] : null;
            String shift = row[3] != null ? row[3].toString() : null;
            String machine = row[4] != null ? row[4].toString() : null;
            String placement = row[5] != null ? row[5].toString() : null;
            String partNumber = row[6] != null ? row[6].toString() : null;
            String description = row[7] != null ? row[7].toString() : null;
            LocalDateTime dateDebutMatelassage = row[8] != null ? (LocalDateTime) row[8] : null;
            LocalDateTime dateFinMatelassage = row[9] != null ? (LocalDateTime) row[9] : null;
            String statusMatelassage = row[10] != null ? row[10].toString() : null;
            String tableMatelassage = row[11] != null ? row[11].toString() : null;
            LocalDateTime dateDebutCoupe = row[12] != null ? (LocalDateTime) row[12] : null;
            LocalDateTime dateFinCoupe = row[13] != null ? (LocalDateTime) row[13] : null;
            String statusCoupe = row[14] != null ? row[14].toString() : null;
            String tableCoupe = row[15] != null ? row[15].toString() : null;
            Double tempsDeCoupe = row[16] != null ? ((Number) row[16]).doubleValue() : null;
            Integer nbrCouche = row[17] != null ? ((Number) row[17]).intValue() : null;
            Long cuttingPlanId = (row.length > 18 && row[18] != null) ? ((Number) row[18]).longValue() : null;
            Long cmsId = cuttingPlanId != null ? planIdToCmsId.get(cuttingPlanId) : null;

            serieInfo.put("serie", serie);
            serieInfo.put("sequence", sequence);
            serieInfo.put("dueDate", date);
            serieInfo.put("dueShift", shift);
            serieInfo.put("machine", machine);
            serieInfo.put("placement", placement);
            serieInfo.put("partNumber", partNumber);
            serieInfo.put("description", description);
            serieInfo.put("dateDebutMatelassage", dateDebutMatelassage);
            serieInfo.put("dateFinMatelassage", dateFinMatelassage);
            serieInfo.put("statusMatelassage", statusMatelassage);
            serieInfo.put("tableMatelassage", tableMatelassage);
            serieInfo.put("dateDebutCoupe", dateDebutCoupe);
            serieInfo.put("dateFinCoupe", dateFinCoupe);
            serieInfo.put("statusCoupe", statusCoupe);
            serieInfo.put("tableCoupe", tableCoupe);
            serieInfo.put("tempsDeCoupe", tempsDeCoupe);
            serieInfo.put("nbrCouche", nbrCouche);
            serieInfo.put("cuttingPlanId", cuttingPlanId);
            serieInfo.put("cmsId", cmsId);
            
            // Get TimingModel cutting times
            Double validatedCuttingTime = null;
            Double realCuttingTime = null;
            if (placement != null && timingModelData.containsKey(placement)) {
                Double[] times = timingModelData.get(placement);
                validatedCuttingTime = times[0];
                realCuttingTime = times[1];
            }
            serieInfo.put("validatedCuttingTime", validatedCuttingTime);
            serieInfo.put("realCuttingTime", realCuttingTime);
            
            // Calculate effective cutting time: validated > real > tempsDeCoupe
            // For tempsDeCoupe (estimated), multiply by nbrCouche ONLY for LASER-DXF machines
            // since LASER-DXF cuts one layer at a time
            Double effectiveCuttingTime = null;
            String cuttingTimeSource = null;
            
            String resolvedType = resolveMachineTypeName(machine, tableCoupe, machineTypeMap);

            if (validatedCuttingTime != null && validatedCuttingTime > 0) {
                cuttingTimeSource = "Validated";
            } else if (realCuttingTime != null && realCuttingTime > 0) {
                cuttingTimeSource = "Real";
            } else if (tempsDeCoupe != null && tempsDeCoupe > 0) {
                cuttingTimeSource = "TempsDeCoupe";
            }

            effectiveCuttingTime = resolveEffectiveCuttingTime(
                    placement,
                    tempsDeCoupe,
                    nbrCouche,
                    resolvedType,
                    timingModelData,
                    resolveMachineEfficience(machine, tableCoupe, machineEfficienceMap)
            );
            serieInfo.put("effectiveCuttingTime", effectiveCuttingTime);
            serieInfo.put("cuttingTimeSource", cuttingTimeSource);
            
            // Determine status with proper retard classification
            // Get shift end time for retard calculation
            int shiftNum;
            try {
                shiftNum = Integer.parseInt(dueShift);
            } catch (NumberFormatException e) {
                shiftNum = 1;
            }
            Map<String, LocalDateTime> shiftTimesMap = getShiftTimes(dueDate, shiftNum);
            LocalDateTime shiftStart = shiftTimesMap.get("start");
            LocalDateTime shiftEnd = shiftTimesMap.get("end");
            
            boolean isPrepared = dateFinMatelassage != null;
            boolean isCutting = dateDebutCoupe != null && dateFinCoupe == null;
            boolean isCut = dateFinCoupe != null;
            boolean isCarryover = false;
            boolean isPartialRetard = false;
            double retardMinutes = 0;
            String retardType = null;
            
            // SR (Surpassant): minutes worked BEFORE the shift started
            // If dateDebutCoupe < shiftStart, the overlap before shiftStart counts as SR
            double srMinutes = 0;
            if (dateDebutCoupe != null && dateDebutCoupe.isBefore(shiftStart)) {
                LocalDateTime srEnd = (dateFinCoupe != null && dateFinCoupe.isBefore(shiftStart)) 
                        ? dateFinCoupe : shiftStart;
                srMinutes = java.time.Duration.between(dateDebutCoupe, srEnd).toMinutes();
                if (srMinutes < 0) srMinutes = 0;
                // Cap at effectiveCuttingTime
                if (effectiveCuttingTime != null && srMinutes > effectiveCuttingTime) {
                    srMinutes = effectiveCuttingTime;
                }
            }
            
            if (dateDebutCoupe == null) {
                // Case (a): Non coupé - full retard
                isCarryover = true;
                retardType = "non_coupe";
                if (effectiveCuttingTime != null) retardMinutes = effectiveCuttingTime;
            } else if (dateDebutCoupe.isAfter(shiftEnd)) {
                // Case (b): Début coupe after shift end - full retard
                isCarryover = true;
                retardType = "debut_apres_shift";
                if (effectiveCuttingTime != null) retardMinutes = effectiveCuttingTime;
            } else if (dateFinCoupe == null && LocalDateTime.now().isAfter(shiftEnd)) {
                // Still cutting but shift is over - retard
                isCarryover = true;
                retardType = "en_cours_apres_shift";
                if (effectiveCuttingTime != null) retardMinutes = effectiveCuttingTime;
            } else if (dateFinCoupe != null && dateFinCoupe.isAfter(shiftEnd)) {
                // Case (c): Partial retard - fin coupe after shift end
                isPartialRetard = true;
                isCarryover = true;
                retardType = "partiel";
                retardMinutes = java.time.Duration.between(shiftEnd, dateFinCoupe).toMinutes();
            }
            // else: fully cut within shift - no retard
            
            serieInfo.put("isPrepared", isPrepared);
            serieInfo.put("isCutting", isCutting);
            serieInfo.put("isCut", isCut);
            serieInfo.put("isCarryover", isCarryover);
            serieInfo.put("isPartialRetard", isPartialRetard);
            serieInfo.put("retardMinutes", retardMinutes);
            serieInfo.put("retardType", retardType);
            serieInfo.put("srMinutes", srMinutes);
            
            result.add(serieInfo);
        }
        
        return result;
    }
    
    /**
     * Batch-resolve CuttingPlan.cmsId for a set of CuttingPlan ids.
     * Uses a light projection query to avoid loading the full entity graph.
     * Returns: planId -> cmsId (entries where cmsId is null are skipped).
     */
    private Map<Long, Long> resolveCmsIdsByPlanIds(Collection<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = cuttingPlanRepository.findIdAndCmsIdByIdIn(new ArrayList<>(planIds));
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null || row[1] == null) continue;
            Long id = ((Number) row[0]).longValue();
            Long cms = ((Number) row[1]).longValue();
            result.put(id, cms);
        }
        return result;
    }

    /**
     * Get validated and real cutting times from TimingModel for placements.
     * Returns map: placement -> [validatedTime, realTime]
     */
    private Map<String, Double[]> getTimingModelDataByPlacements(List<String> placements) {
        if (placements == null || placements.isEmpty()) {
            return Collections.emptyMap();
        }

        LinkedHashSet<String> uniquePlacements = placements.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Double[]> result = new HashMap<>();

        List<String> placementList = new ArrayList<>(uniquePlacements);
        for (int index = 0; index < placementList.size(); index += TIMING_MODEL_BATCH_SIZE) {
            List<String> batch = placementList.subList(index, Math.min(index + TIMING_MODEL_BATCH_SIZE, placementList.size()));
            List<Object[]> rows = timingModelRepository.findValidatedAndRealTimesByPlacements(batch);

            for (Object[] row : rows) {
                String placement = (String) row[0];
                Double validated = row[1] != null ? ((Number) row[1]).doubleValue() : null;
                Double real = row[2] != null ? ((Number) row[2]).doubleValue() : null;
                result.put(placement, new Double[]{validated, real});
            }
        }

        return result;
    }

    private String resolveMachineTypeName(String machineName, String tableCoupe, Map<String, String> machineTypeMap) {
        String machineType = machineName != null ? machineTypeMap.getOrDefault(machineName, "") : "";
        if (machineType.isEmpty() && tableCoupe != null) {
            machineType = machineTypeMap.getOrDefault(tableCoupe, "");
        }
        return machineType;
    }

    /**
     * Resolve a machine's expected efficiency (%) the same way {@link #resolveMachineTypeName}
     * resolves its type: machine field first, then tableCoupe. Falls back to 90% when the
     * machine is unknown or has no value, so the cutting time stays sane.
     */
    private double resolveMachineEfficience(String machineName, String tableCoupe, Map<String, Double> efficienceMap) {
        Double eff = machineName != null ? efficienceMap.get(machineName) : null;
        if (eff == null && tableCoupe != null) {
            eff = efficienceMap.get(tableCoupe);
        }
        return eff != null && eff > 0 ? eff : 90.0;
    }

    /**
     * Resolve the effective cutting time for one serie.
     *
     * <p>Since Phase 1 of the full guide (see {@code plans/FULL_GUIDE_IMPLEMENTATION.md}),
     * the logic lives in {@link CuttingTimeCalculator}. This method adapts the
     * legacy {@code Double[]{validated, real}} map shape to the bean's
     * {@code Map<String, TimingRow>} shape and delegates. The return contract
     * is preserved: {@code null} when no value is available, a positive
     * {@code Double} otherwise.</p>
     *
     * <p>Kept as a private pass-through for one release so the surrounding
     * call sites in this service remain untouched. Delete in a later cleanup
     * pass once all callers have migrated to the bean directly.</p>
     */
    private Double resolveEffectiveCuttingTime(String placement,
                                               Double tempsDeCoupe,
                                               Integer nbrCouche,
                                               String machineType,
                                               Map<String, Double[]> timingModelData,
                                               Double efficiencePct) {
        Map<String, CuttingTimeCalculator.TimingRow> adapted = new HashMap<>();
        if (timingModelData != null) {
            for (Map.Entry<String, Double[]> e : timingModelData.entrySet()) {
                Double[] v = e.getValue();
                Double validated = v != null && v.length > 0 ? v[0] : null;
                Double real      = v != null && v.length > 1 ? v[1] : null;
                adapted.put(e.getKey(), new CuttingTimeCalculator.TimingRow(validated, real));
            }
        }
        // Numerator = raw time scaled by the machine's expected efficience
        // (time × 1/(efficience/100)), per the 2026-06-16 rules (commit 477b21b):
        // a slower machine books proportionally more load. The PlanDeCharge load
        // DENOMINATOR stays RAW capacity (machines × configured minutes, see the
        // frontend getShiftProductiveCapacityMinutes), so efficience is counted
        // exactly once — matching the documented UI formula "Chg % = temps ×
        // 1/efficience% / (machines actives × temps configuré)". Passing a real
        // efficiencePct also makes resolve() skip the legacy Gerber ×2 (a Gerber
        // configured at ~50% efficience yields the same ×2).
        CuttingTimeCalculator.Resolved r = cuttingTimeCalculator.resolve(
                placement, tempsDeCoupe, nbrCouche, machineType, adapted, efficiencePct);
        return r.source == CuttingTimeCalculator.Source.NONE ? null : r.minutes;
    }

    /**
     * Calculate shift charge summary with detailed breakdown by machine type.
     */
    public Map<String, Object> calculateShiftChargeSummary(LocalDate dueDate, String dueShift) {
        List<Map<String, Object>> series = getDetailedSeriesForShift(dueDate, dueShift);
        
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("dueDate", dueDate);
        summary.put("dueShift", dueShift);
        summary.put("totalSeries", series.size());
        
        // Group by machineType (from tableCoupe -> lookup machineType)
        Map<String, List<Map<String, Object>>> seriesByMachine = new LinkedHashMap<>();
        Map<String, Double> chargeByMachineType = new LinkedHashMap<>();
        Map<String, Integer> countByMachineType = new LinkedHashMap<>();
        Map<String, Double> cutTimeByMachineType = new LinkedHashMap<>();
        Map<String, Double> carryoverByMachineType = new LinkedHashMap<>();
        
        double totalPlannedTime = 0;
        double totalCutTime = 0;
        double totalCarryover = 0;
        double totalRetardMinutes = 0;
        int cutCount = 0;
        int carryoverCount = 0;
        int partialRetardCount = 0;
        
        // Get machine type lookup
        Map<String, String> machineToType = getMachineTypeMap();
        
        for (Map<String, Object> serieInfo : series) {
            String machineName = (String) serieInfo.get("machine");
            String tableCoupe = (String) serieInfo.get("tableCoupe");
            // Resolve machine type: try machine field first, fallback to tableCoupe
            String machineType = resolveMachineTypeName(machineName, tableCoupe, machineToType);
            if (machineType == null || machineType.isEmpty()) {
                machineType = "Unknown";
            }
            // Store resolved machine type back in the serie info
            serieInfo.put("machineType", machineType);
            
            Double effectiveCuttingTime = (Double) serieInfo.get("effectiveCuttingTime");
            Boolean isCut = (Boolean) serieInfo.get("isCut");
            Boolean isCarryover = (Boolean) serieInfo.get("isCarryover");
            Boolean isPartialRetard = (Boolean) serieInfo.get("isPartialRetard");
            Double retardMinutes = serieInfo.get("retardMinutes") != null ? ((Number) serieInfo.get("retardMinutes")).doubleValue() : 0.0;
            
            seriesByMachine.computeIfAbsent(machineType, k -> new ArrayList<>()).add(serieInfo);
            
            if (effectiveCuttingTime != null && effectiveCuttingTime > 0) {
                totalPlannedTime += effectiveCuttingTime;
                chargeByMachineType.merge(machineType, effectiveCuttingTime, Double::sum);
                countByMachineType.merge(machineType, 1, Integer::sum);
                
                if (isCarryover != null && isCarryover) {
                    totalCarryover += retardMinutes > 0 ? retardMinutes : effectiveCuttingTime;
                    totalRetardMinutes += retardMinutes;
                    carryoverByMachineType.merge(machineType, retardMinutes > 0 ? retardMinutes : effectiveCuttingTime, Double::sum);
                    carryoverCount++;
                    if (isPartialRetard != null && isPartialRetard) {
                        partialRetardCount++;
                    }
                    // Also count cut portion for partial retard
                    if (isPartialRetard != null && isPartialRetard) {
                        double cutPortion = effectiveCuttingTime - retardMinutes;
                        if (cutPortion > 0) {
                            totalCutTime += cutPortion;
                            cutTimeByMachineType.merge(machineType, cutPortion, Double::sum);
                        }
                    }
                } else {
                    totalCutTime += effectiveCuttingTime;
                    cutTimeByMachineType.merge(machineType, effectiveCuttingTime, Double::sum);
                    cutCount++;
                }
            }
        }
        
        summary.put("totalPlannedTime", totalPlannedTime);
        summary.put("totalCutTime", totalCutTime);
        summary.put("totalCarryover", totalCarryover);
        summary.put("totalRetardMinutes", totalRetardMinutes);
        summary.put("cutCount", cutCount);
        summary.put("carryoverCount", carryoverCount);
        summary.put("partialRetardCount", partialRetardCount);
        summary.put("chargeByMachineType", chargeByMachineType);
        summary.put("countByMachineType", countByMachineType);
        summary.put("cutTimeByMachineType", cutTimeByMachineType);
        summary.put("carryoverByMachineType", carryoverByMachineType);
        summary.put("seriesByMachineType", seriesByMachine);

        // Non-imported charge from Order_Schedule (status = 'F')
        Map<String, Object> nonImported = nonImportedChargeService.computeCharge(dueDate, dueShift);
        summary.put("nonImportedCharge", nonImported);

        return summary;
    }
    
    /**
     * Get non-imported charge from Order_Schedule for a date/shift.
     * Delegates to {@link NonImportedChargeService}.
     */
    public Map<String, Object> getNonImportedCharge(LocalDate date, String shift) {
        return nonImportedChargeService.computeCharge(date, shift);
    }

    /**
     * Part-number cutting-time report for a shift (Plan de Charge "Détails de charge").
     * <p>Imported rows: the shift's series are grouped by sequence; each sequence's
     * total effectiveCuttingTime is split across its part numbers by their cached
     * perimeter share (computed + cached lazily on first use). Non-imported rows come
     * straight from {@link NonImportedChargeService} (perimetre / % left null).
     * Times are in minutes.</p>
     */
    @Transactional
    public List<Map<String, Object>> getPartNumberCuttingTimeReport(LocalDate dueDate, String dueShift) {
        List<Map<String, Object>> rows = new ArrayList<>();

        // ---- imported: group the shift's series by sequence ----
        List<Map<String, Object>> series = getDetailedSeriesForShift(dueDate, dueShift);
        Map<String, List<Map<String, Object>>> bySequence = new LinkedHashMap<>();
        for (Map<String, Object> s : series) {
            String seq = (String) s.get("sequence");
            if (seq == null) continue;
            bySequence.computeIfAbsent(seq, k -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<String, List<Map<String, Object>>> e : bySequence.entrySet()) {
            String sequence = e.getKey();
            double sequenceTime = 0.0;
            Long seriesCmsId = null;
            for (Map<String, Object> s : e.getValue()) {
                Object ect = s.get("effectiveCuttingTime");
                if (ect instanceof Number) sequenceTime += ((Number) ect).doubleValue();
                if (seriesCmsId == null && s.get("cmsId") instanceof Number) {
                    seriesCmsId = ((Number) s.get("cmsId")).longValue();
                }
            }

            CuttingRequest req = cuttingRequestRepository.findBySequence(sequence);
            if (req == null || req.getCuttingRequestPartNumbers() == null
                    || req.getCuttingRequestPartNumbers().isEmpty()) {
                continue;
            }
            perimetreService.ensureRequestPerimetre(req);

            List<CuttingRequestPartNumber> pns = req.getCuttingRequestPartNumbers();
            double totalPerimetre = 0.0;
            for (CuttingRequestPartNumber p : pns) {
                if (p.getPerimetre() != null) totalPerimetre += p.getPerimetre();
            }
            Long cmsId = req.getCmsId() != null ? req.getCmsId() : seriesCmsId;

            for (CuttingRequestPartNumber p : pns) {
                double per = p.getPerimetre() != null ? p.getPerimetre() : 0.0;
                // When no perimeter is cached, split the sequence time equally so the
                // part-number total always equals the sequence's series total (#1).
                double share = totalPerimetre > 0 ? per / totalPerimetre : 1.0 / pns.size();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("projet", req.getProjet());
                row.put("version", req.getVersion());
                row.put("partNumber", p.getPartNumber());
                row.put("quantity", p.getQuantity());
                row.put("cmsId", cmsId);
                row.put("sequence", sequence);
                row.put("perimetre", per);
                row.put("percentageOnPlan", share * 100.0);
                row.put("tempsDeCoupe", sequenceTime * share);
                row.put("imported", true);
                rows.add(row);
            }
        }

        // ---- non-imported: straight from the existing charge ----
        Map<String, Object> nonImported = nonImportedChargeService.computeCharge(dueDate, dueShift);
        Object detailsObj = nonImported.get("details");
        if (detailsObj instanceof List) {
            for (Object o : (List<?>) detailsObj) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> d = (Map<?, ?>) o;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("projet", null);
                row.put("version", null);
                row.put("partNumber", d.get("partNumber"));
                row.put("quantity", d.get("quantity"));
                row.put("cmsId", null);
                row.put("sequence", null);
                row.put("perimetre", null);
                row.put("percentageOnPlan", null);
                row.put("tempsDeCoupe", d.get("estimatedMinutes"));
                row.put("imported", false);
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Get machine name to machine type mapping.
     */
    private Map<String, String> getMachineTypeMap() {
        List<ProductionTable> machines = productionTableRepository.findAll();
        Map<String, String> machineToType = new HashMap<>();
        for (ProductionTable machine : machines) {
            if (machine.getMachineType() != null) {
                machineToType.put(machine.getNom(), machine.getMachineType().getName());
            }
        }
        return machineToType;
    }

    /**
     * Get machine name -&gt; expected efficiency (%) mapping ({@code ProductionTable.efficience}).
     * PlanDeCharge feeds this into the cutting-time calculator (numerator scaling).
     */
    private Map<String, Double> getMachineEfficienceMap() {
        List<ProductionTable> machines = productionTableRepository.findAll();
        Map<String, Double> machineToEff = new HashMap<>();
        for (ProductionTable machine : machines) {
            if (machine.getEfficience() != null) {
                machineToEff.put(machine.getNom(), machine.getEfficience());
            }
        }
        return machineToEff;
    }

    /**
     * Get saved shift load calculations for a date range.
     */
    public List<ShiftLoadCalculation> getShiftLoadCalculations(LocalDate startDate, LocalDate endDate) {
        return shiftLoadCalculationRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Get aggregated cutting time by machine and shift for a date range.
     * Uses per-serie data with TimingModel lookup (validated > real > tempsDeCoupe).
     * Groups by machine field (not tableCoupe).
     * Returns a map: machineName -> date -> shift -> totalCuttingTime
     */
    public Map<String, Map<LocalDate, Map<String, Double>>> getAggregatedCuttingTimeByMachine(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = cuttingRequestSerieInfoRepository.findSeriesForAggregation(startDate, endDate);

        // Build machine type map to determine LASER-DXF/Gerber machines
        Map<String, String> machineTypeMap = getMachineTypeMap();
        Map<String, Double> machineEfficienceMap = getMachineEfficienceMap();

        // Collect all placements to batch query TimingModel
        Set<String> allPlacements = new HashSet<>();
        for (Object[] row : results) {
            String placement = row[1] != null ? row[1].toString() : null;
            if (placement != null) allPlacements.add(placement);
        }
        Map<String, Double[]> timingModelData = getTimingModelDataByPlacements(new ArrayList<>(allPlacements));
        
        Map<String, Map<LocalDate, Map<String, Double>>> aggregated = new LinkedHashMap<>();
        
        for (Object[] row : results) {
            String machine = (String) row[0];
            String placement = row[1] != null ? row[1].toString() : null;
            LocalDate planningDate = (LocalDate) row[2];
            String shift = (String) row[3];
            Double tempsDeCoupe = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            Integer nbrCouche = row[5] != null ? ((Number) row[5]).intValue() : 1;
            
            // Apply validated > real > tempsDeCoupe priority (same as getDetailedSeriesForShift)
            String machineType = resolveMachineTypeName(machine, null, machineTypeMap);
            Double effectiveCuttingTime = resolveEffectiveCuttingTime(
                    placement,
                    tempsDeCoupe,
                    nbrCouche,
                    machineType,
                    timingModelData,
                    resolveMachineEfficience(machine, null, machineEfficienceMap)
            );
            
            if (effectiveCuttingTime == null || effectiveCuttingTime <= 0) continue;
            
            aggregated.computeIfAbsent(machine, k -> new LinkedHashMap<>())
                      .computeIfAbsent(planningDate, k -> new LinkedHashMap<>())
                      .merge(shift, effectiveCuttingTime, Double::sum);
        }
        
        return aggregated;
    }
    
    /**
     * Get aggregated cutting time split by completion status (cut vs not cut) with proper retard calculation.
     * Uses per-serie data with TimingModel lookup (validated > real > tempsDeCoupe).
     * Groups by machine field (not tableCoupe).
     * Returns a map: machineName -> date -> shift -> { total, cut, notCut, sr }
     */
    public Map<String, Map<LocalDate, Map<String, Map<String, Double>>>> getAggregatedCuttingTimeWithStatus(LocalDate startDate, LocalDate endDate) {
        List<Object[]> seriesResults = cuttingRequestSerieInfoRepository.findSeriesForAggregation(startDate, endDate);

        // Build machine type map to determine LASER-DXF/Gerber machines
        Map<String, String> machineTypeMap = getMachineTypeMap();
        Map<String, Double> machineEfficienceMap = getMachineEfficienceMap();

        // Collect all placements to batch query TimingModel
        Set<String> allPlacements = new HashSet<>();
        for (Object[] row : seriesResults) {
            String placement = row[1] != null ? row[1].toString() : null;
            if (placement != null) allPlacements.add(placement);
        }
        Map<String, Double[]> timingModelData = getTimingModelDataByPlacements(new ArrayList<>(allPlacements));
        
        Map<String, Map<LocalDate, Map<String, Map<String, Double>>>> aggregated = new LinkedHashMap<>();
        
        for (Object[] row : seriesResults) {
            String machine = (String) row[0];
            String placement = row[1] != null ? row[1].toString() : null;
            LocalDate planningDate = (LocalDate) row[2];
            String shift = (String) row[3];
            Double tempsDeCoupe = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            Integer nbrCouche = row[5] != null ? ((Number) row[5]).intValue() : 1;
            LocalDateTime dateDebutCoupe = row[6] != null ? (LocalDateTime) row[6] : null;
            LocalDateTime dateFinCoupe = row[7] != null ? (LocalDateTime) row[7] : null;
            
            // Apply validated > real > tempsDeCoupe priority (same as getDetailedSeriesForShift)
            String machineType = resolveMachineTypeName(machine, null, machineTypeMap);
            Double cuttingTime = resolveEffectiveCuttingTime(
                    placement,
                    tempsDeCoupe,
                    nbrCouche,
                    machineType,
                    timingModelData,
                    resolveMachineEfficience(machine, null, machineEfficienceMap)
            );
            
            if (cuttingTime == null || cuttingTime <= 0) continue;
            
            // Get shift times for retard/SR calculation
            int shiftNumber;
            try {
                shiftNumber = Integer.parseInt(shift);
            } catch (NumberFormatException e) {
                shiftNumber = 1;
            }
            Map<String, LocalDateTime> shiftTimes = getShiftTimes(planningDate, shiftNumber);
            LocalDateTime shiftStart = shiftTimes.get("start");
            LocalDateTime shiftEnd = shiftTimes.get("end");
            
            // Initialize the map structure
            Map<String, Double> statusMap = aggregated
                    .computeIfAbsent(machine, k -> new LinkedHashMap<>())
                    .computeIfAbsent(planningDate, k -> new LinkedHashMap<>())
                    .computeIfAbsent(shift, k -> {
                        Map<String, Double> m = new LinkedHashMap<>();
                        m.put("total", 0.0);
                        m.put("cut", 0.0);
                        m.put("notCut", 0.0);
                        m.put("sr", 0.0);
                        return m;
                    });
            
            statusMap.merge("total", cuttingTime, Double::sum);
            
            // SR: minutes worked BEFORE the shift started
            double srTime = 0.0;
            if (dateDebutCoupe != null && dateDebutCoupe.isBefore(shiftStart)) {
                LocalDateTime srEnd = (dateFinCoupe != null && dateFinCoupe.isBefore(shiftStart))
                        ? dateFinCoupe : shiftStart;
                srTime = java.time.Duration.between(dateDebutCoupe, srEnd).toMinutes();
                if (srTime < 0) srTime = 0;
                if (srTime > cuttingTime) srTime = cuttingTime;
            }
            statusMap.merge("sr", srTime, Double::sum);
            
            // Determine retard based on three cases
            double retardTime = 0.0;
            double cutTime = 0.0;
            
            if (dateDebutCoupe == null) {
                retardTime = cuttingTime;
            } else if (dateDebutCoupe.isAfter(shiftEnd)) {
                retardTime = cuttingTime;
            } else if (dateFinCoupe == null) {
                if (LocalDateTime.now().isAfter(shiftEnd)) {
                    retardTime = cuttingTime;
                } else {
                    cutTime = cuttingTime;
                }
            } else if (dateFinCoupe.isAfter(shiftEnd)) {
                long partialRetardMinutes = java.time.Duration.between(shiftEnd, dateFinCoupe).toMinutes();
                retardTime = Math.min(partialRetardMinutes, cuttingTime);
                cutTime = cuttingTime - retardTime;
            } else {
                cutTime = cuttingTime;
            }
            
            statusMap.merge("cut", cutTime, Double::sum);
            statusMap.merge("notCut", retardTime, Double::sum);
        }
        
        return aggregated;
    }
    
    /**
     * Get cutting time from TimingModel for specific placements.
     * Returns a map: placement -> cuttingTime
     */
    public Map<String, Double> getCuttingTimesByPlacements(List<String> placements) {
        if (placements == null || placements.isEmpty()) {
            return Collections.emptyMap();
        }

        LinkedHashSet<String> uniquePlacements = placements.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Double> cuttingTimes = new HashMap<>();

        List<String> placementList = new ArrayList<>(uniquePlacements);
        for (int index = 0; index < placementList.size(); index += TIMING_MODEL_BATCH_SIZE) {
            List<String> batch = placementList.subList(index, Math.min(index + TIMING_MODEL_BATCH_SIZE, placementList.size()));
            List<Object[]> results = timingModelRepository.findCuttingTimesByPlacements(batch);

            for (Object[] row : results) {
                String placement = (String) row[0];
                Double cuttingTime = row[1] != null ? ((Number) row[1]).doubleValue() : null;
                if (placement != null && cuttingTime != null) {
                    cuttingTimes.put(placement, cuttingTime);
                }
            }
        }

        return cuttingTimes;
    }
    
    /**
     * Get distinct placements for a date range - lightweight query for frontend to request TimingModel data.
     */
    public List<String> getDistinctPlacements(LocalDate startDate, LocalDate endDate) {
        return cuttingRequestSerieInfoRepository.findDistinctPlacementsByDateRange(startDate, endDate);
    }

    /**
     * Calculate and save shift load for a specific date and shift.
     * Uses the same live aggregation rules as the status grid and charge details:
     * machine field + TimingModel priority (validated > real > tempsDeCoupe).
     */
    @Transactional
    public List<ShiftLoadCalculation> calculateShiftLoad(LocalDate date, int shiftNumber, String username) {
        Map<String, LocalDateTime> shiftTimes = getShiftTimes(date, shiftNumber);
        LocalDateTime shiftStart = shiftTimes.get("start");
        
        // Convert shift number to string for query
        String shiftStr = String.valueOf(shiftNumber);
        
        Map<String, Map<LocalDate, Map<String, Double>>> aggregatedCuttingTime = getAggregatedCuttingTimeByMachine(date, date);

        // Get all machines grouped by type - single query
        List<ProductionTable> machines = productionTableRepository.findAll();
        Map<String, List<ProductionTable>> machinesByType = machines.stream()
                .filter(m -> m.getMachineType() != null)
                .collect(Collectors.groupingBy(m -> m.getMachineType().getName()));

        // Build machine name to type lookup
        Map<String, String> machineToType = new HashMap<>();
        for (ProductionTable machine : machines) {
            if (machine.getMachineType() != null) {
                machineToType.put(machine.getNom(), machine.getMachineType().getName());
            }
        }

        // Batch-fetch every machine's status code at shiftStart in a single query
        // (was N per-machine queries inside the loop below).
        Map<String, String> statusByMachine = etatMachineHistoriqueService.getAllCurrentStatusCodes(shiftStart);

        List<ShiftLoadCalculation> calculations = new ArrayList<>();
        
        BigDecimal globalPlannedTime = BigDecimal.ZERO;
        BigDecimal globalAvailableTime = BigDecimal.ZERO;
        int globalMachinesCount = 0;
        // Track a weighted global efficiency for the GLOBAL row
        BigDecimal globalWeightedEffNumerator = BigDecimal.ZERO;
        BigDecimal globalWeightedEffDenominator = BigDecimal.ZERO;

        for (Map.Entry<String, List<ProductionTable>> entry : machinesByType.entrySet()) {
            String machineType = entry.getKey();
            List<ProductionTable> typeMachines = entry.getValue();

            // Count available machines (status M only) and sum their rendement.
            // rendement = ProductionTable.efficience / 100 (100% -> 1, 50% -> 0.5).
            int availableMachines = 0;
            double rendementSum = 0.0;
            BigDecimal plannedTime = BigDecimal.ZERO;

            for (ProductionTable machine : typeMachines) {
                String status = statusByMachine.getOrDefault(machine.getNom(), "M");
                if ("M".equals(status)) {
                    availableMachines++;
                    double eff = machine.getEfficience() != null ? machine.getEfficience() : 100.0;
                    rendementSum += eff / 100.0;
                }

                // Add planned time for this machine
                Double machinePlannedTime = Optional.ofNullable(aggregatedCuttingTime.get(machine.getNom()))
                        .map(dateMap -> dateMap.get(date))
                        .map(shiftMap -> shiftMap.get(shiftStr))
                        .orElse(null);
                if (machinePlannedTime != null) {
                    plannedTime = plannedTime.add(BigDecimal.valueOf(machinePlannedTime));
                }
            }

            // Denominator: Σ rendement × configured shift minutes × global efficience
            // (efficience global "coupe/laser" from CapaciteInstallee.efficienceTarget).
            int configuredMinutes = resolveConfiguredShiftMinutes(date, shiftNumber, machineType);
            double efficiencyTargetPct = resolveEfficienceTargetPct(date, shiftNumber, machineType);
            BigDecimal efficiencyPercentage = BigDecimal.valueOf(efficiencyTargetPct)
                    .setScale(2, RoundingMode.HALF_UP);

            // availableTime = rendementSum × configuredMinutes × (efficienceTarget / 100)
            BigDecimal availableTime = BigDecimal.valueOf(rendementSum)
                    .multiply(BigDecimal.valueOf(configuredMinutes))
                    .multiply(BigDecimal.valueOf(efficiencyTargetPct / 100.0))
                    .setScale(2, RoundingMode.HALF_UP);

            // Calculate load percentage
            BigDecimal loadPercentage = BigDecimal.ZERO;
            if (availableTime.compareTo(BigDecimal.ZERO) > 0) {
                loadPercentage = plannedTime.multiply(BigDecimal.valueOf(100))
                        .divide(availableTime, 2, RoundingMode.HALF_UP);
            }

            // Create or update calculation record
            ShiftLoadCalculation calc = shiftLoadCalculationRepository
                    .findByShiftDateAndShiftNumberAndMachineType(date, shiftNumber, machineType)
                    .orElse(new ShiftLoadCalculation());

            calc.setShiftDate(date);
            calc.setShiftNumber(shiftNumber);
            calc.setMachineType(machineType);
            calc.setTotalPlannedTime(plannedTime);
            calc.setAvailableTime(availableTime);
            calc.setLoadPercentage(loadPercentage);
            calc.setEfficiencyPercentage(efficiencyPercentage);
            calc.setMachinesCount(availableMachines);
            calc.setCalculatedAt(LocalDateTime.now());
            calc.setCalculatedBy(username);

            // Calculate carryover time from previous shift
            BigDecimal carryoverTime = calculateCarryoverTime(date, shiftNumber, machineType, plannedTime, availableTime);
            calc.setCarryoverTime(carryoverTime);

            calculations.add(shiftLoadCalculationRepository.save(calc));

            globalPlannedTime = globalPlannedTime.add(plannedTime);
            globalAvailableTime = globalAvailableTime.add(availableTime);
            globalMachinesCount += availableMachines;

            // Weight the efficiency by machine-minutes so the GLOBAL row tells the truth
            // when Coupe and Laser have different efficiency targets.
            BigDecimal weight = BigDecimal.valueOf(rendementSum)
                    .multiply(BigDecimal.valueOf(configuredMinutes));
            globalWeightedEffNumerator = globalWeightedEffNumerator.add(weight.multiply(efficiencyPercentage));
            globalWeightedEffDenominator = globalWeightedEffDenominator.add(weight);
        }

        // Save global calculation
        ShiftLoadCalculation globalCalc = shiftLoadCalculationRepository
                .findByShiftDateAndShiftNumberAndMachineType(date, shiftNumber, "GLOBAL")
                .orElse(new ShiftLoadCalculation());

        globalCalc.setShiftDate(date);
        globalCalc.setShiftNumber(shiftNumber);
        globalCalc.setMachineType("GLOBAL");
        globalCalc.setTotalPlannedTime(globalPlannedTime);
        globalCalc.setAvailableTime(globalAvailableTime);

        BigDecimal globalLoadPercentage = BigDecimal.ZERO;
        if (globalAvailableTime.compareTo(BigDecimal.ZERO) > 0) {
            globalLoadPercentage = globalPlannedTime.multiply(BigDecimal.valueOf(100))
                    .divide(globalAvailableTime, 2, RoundingMode.HALF_UP);
        }
        globalCalc.setLoadPercentage(globalLoadPercentage);
        // Weighted efficiency across machine types (falls back to 90 when no weight yet)
        BigDecimal globalEfficiency;
        if (globalWeightedEffDenominator.compareTo(BigDecimal.ZERO) > 0) {
            globalEfficiency = globalWeightedEffNumerator.divide(globalWeightedEffDenominator, 2, RoundingMode.HALF_UP);
        } else {
            globalEfficiency = BigDecimal.valueOf(DEFAULT_EFFICIENCY_RATIO * 100.0).setScale(2, RoundingMode.HALF_UP);
        }
        globalCalc.setEfficiencyPercentage(globalEfficiency);
        globalCalc.setMachinesCount(globalMachinesCount);
        globalCalc.setCalculatedAt(LocalDateTime.now());
        globalCalc.setCalculatedBy(username);
        
        // Calculate global carryover
        BigDecimal globalCarryover = calculateCarryoverTime(date, shiftNumber, "GLOBAL", globalPlannedTime, globalAvailableTime);
        globalCalc.setCarryoverTime(globalCarryover);
        
        calculations.add(shiftLoadCalculationRepository.save(globalCalc));
        
        return calculations;
    }

    /**
     * Calculate carryover time from the previous shift.
     * Carryover = excess planned time when load > 100% (plannedTime - availableTime if positive)
     */
    private BigDecimal calculateCarryoverTime(LocalDate date, int shiftNumber, String machineType, 
                                               BigDecimal currentPlannedTime, BigDecimal currentAvailableTime) {
        // Determine previous shift
        LocalDate prevDate;
        int prevShift;
        
        if (shiftNumber == 1) {
            // Previous is shift 3 of previous day
            prevDate = date.minusDays(1);
            prevShift = 3;
        } else {
            // Same day, previous shift
            prevDate = date;
            prevShift = shiftNumber - 1;
        }
        
        // Get previous shift calculation
        Optional<ShiftLoadCalculation> prevCalcOpt = shiftLoadCalculationRepository
                .findByShiftDateAndShiftNumberAndMachineType(prevDate, prevShift, machineType);
        
        if (prevCalcOpt.isPresent()) {
            ShiftLoadCalculation prevCalc = prevCalcOpt.get();
            BigDecimal prevPlanned = prevCalc.getTotalPlannedTime();
            BigDecimal prevAvailable = prevCalc.getAvailableTime();
            BigDecimal prevCarryover = prevCalc.getCarryoverTime();
            
            if (prevPlanned != null && prevAvailable != null) {
                // Calculate excess from previous shift (including its carryover)
                BigDecimal totalPrevLoad = prevPlanned;
                if (prevCarryover != null) {
                    totalPrevLoad = totalPrevLoad.add(prevCarryover);
                }
                
                BigDecimal excess = totalPrevLoad.subtract(prevAvailable);
                if (excess.compareTo(BigDecimal.ZERO) > 0) {
                    return excess;
                }
            }
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Get all series for a date range with resolved effective cutting times.
     * Returns lightweight data suitable for frontend indicator calculations.
     * Each entry: { serie, machine, placement, dueDate, dueShift, tempsDeCoupe, nbrCouche,
     *               dateDebutCoupe, dateFinCoupe, effectiveCuttingTime }
     */
    public List<Map<String, Object>> getSeriesForDateRange(LocalDate startDate, LocalDate endDate) {
        // Use findSeriesForDateRange which includes serie, sequence, tableCoupe, statusCoupe
        List<Object[]> results = cuttingRequestSerieInfoRepository.findSeriesForDateRange(startDate, endDate);
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> machineTypeMap = getMachineTypeMap();
        Map<String, Double> machineEfficienceMap = getMachineEfficienceMap();

        // Collect placements for TimingModel resolution + cutting plan ids for cmsId lookup.
        Set<String> allPlacements = new HashSet<>();
        Set<Long> planIds = new HashSet<>();
        for (Object[] row : results) {
            // findSeriesForDateRange columns: serie(0), sequence(1), dueDate(2), dueShift(3),
            // machine(4), placement(5), tableCoupe(6), tempsDeCoupe(7), dateDebutCoupe(8), dateFinCoupe(9), statusCoupe(10), nbrCouche(11), cuttingPlanId(12)
            String placement = row[5] != null ? row[5].toString() : null;
            if (placement != null) allPlacements.add(placement);
            if (row[12] != null) planIds.add(((Number) row[12]).longValue());
        }
        Map<String, Double[]> timingModelData = getTimingModelDataByPlacements(new ArrayList<>(allPlacements));
        Map<Long, Long> planIdToCmsId = resolveCmsIdsByPlanIds(planIds);

        List<Map<String, Object>> seriesList = new ArrayList<>();
        for (Object[] row : results) {
            String serie = row[0] != null ? row[0].toString() : null;
            String sequence = row[1] != null ? row[1].toString() : null;
            LocalDate planningDate = (LocalDate) row[2];
            String shift = (String) row[3];
            String machine = (String) row[4];
            String placement = row[5] != null ? row[5].toString() : null;
            String tableCoupe = row[6] != null ? row[6].toString() : null;
            Double tempsDeCoupe = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;
            LocalDateTime dateDebutCoupe = row[8] != null ? (LocalDateTime) row[8] : null;
            LocalDateTime dateFinCoupe = row[9] != null ? (LocalDateTime) row[9] : null;
            // row[10] = statusCoupe (unused)
            Integer nbrCouche = row[11] != null ? ((Number) row[11]).intValue() : 1;
            Long cuttingPlanId = row[12] != null ? ((Number) row[12]).longValue() : null;
            Long cmsId = cuttingPlanId != null ? planIdToCmsId.get(cuttingPlanId) : null;

            String machineType = resolveMachineTypeName(machine, tableCoupe, machineTypeMap);

            // Resolve cuttingTimeSource with the same priority as getDetailedSeriesForShift:
            // Validated > Real > TempsDeCoupe. Surface it so the modal can show provenance.
            String cuttingTimeSource = null;
            if (placement != null && timingModelData.containsKey(placement)) {
                Double[] times = timingModelData.get(placement);
                Double validated = times[0];
                Double real = times[1];
                if (validated != null && validated > 0) {
                    cuttingTimeSource = "Validated";
                } else if (real != null && real > 0) {
                    cuttingTimeSource = "Real";
                }
            }
            if (cuttingTimeSource == null && tempsDeCoupe != null && tempsDeCoupe > 0) {
                cuttingTimeSource = "TempsDeCoupe";
            }

            Double effectiveCuttingTime = resolveEffectiveCuttingTime(
                    placement, tempsDeCoupe, nbrCouche, machineType, timingModelData,
                    resolveMachineEfficience(machine, tableCoupe, machineEfficienceMap));
            if (effectiveCuttingTime == null || effectiveCuttingTime <= 0) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("serie", serie);
            entry.put("sequence", sequence);
            entry.put("machine", machine);
            entry.put("placement", placement);
            entry.put("tableCoupe", tableCoupe);
            entry.put("dueDate", planningDate.toString()); // ISO string "yyyy-MM-dd"
            entry.put("dueShift", shift);
            entry.put("nbrCouche", nbrCouche);
            entry.put("tempsDeCoupe", tempsDeCoupe);
            entry.put("effectiveCuttingTime", effectiveCuttingTime);
            entry.put("cuttingTimeSource", cuttingTimeSource);
            entry.put("dateDebutCoupe", dateDebutCoupe != null ? dateDebutCoupe.toString() : null);
            entry.put("dateFinCoupe", dateFinCoupe != null ? dateFinCoupe.toString() : null);
            entry.put("cuttingPlanId", cuttingPlanId);
            entry.put("cmsId", cmsId);
            seriesList.add(entry);
        }
        return seriesList;
    }

    /**
     * Get current shift information.
     */
    public Map<String, Object> getCurrentShiftInfo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        int currentShift;
        
        LocalTime time = now.toLocalTime();
        
        if (time.isBefore(LocalTime.of(5, 45))) {
            currentShift = 1;
        } else if (time.isBefore(LocalTime.of(13, 45))) {
            currentShift = 2;
        } else if (time.isBefore(LocalTime.of(21, 45))) {
            currentShift = 3;
        } else {
            currentShift = 1;
            today = today.plusDays(1); // Shift 1 of the next day
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("date", today);
        info.put("shift", currentShift);
        info.put("shiftTimes", getShiftTimes(today, currentShift));

        return info;
    }

    // ----------------------------------------------------------------- snapshots

    /**
     * Get the persisted "Détails de charge" snapshot for a past shift, if any.
     * Only past shifts are ever snapshotted (the frontend computes the current and
     * next shift live), so callers use this to serve an old shift without recompute.
     */
    public Optional<PlanDeChargeSnapshot> getShiftSnapshot(LocalDate date, int shiftNumber) {
        return planDeChargeSnapshotRepository.findByShiftDateAndShiftNumber(date, shiftNumber);
    }

    /**
     * Upsert the "Détails de charge" snapshot for a shift. The payload is the JSON
     * the frontend built for that shift; we store it verbatim (see
     * {@link PlanDeChargeSnapshot}). The "Actualiser" button recomputes live and
     * calls this again to overwrite.
     */
    @Transactional
    public PlanDeChargeSnapshot saveShiftSnapshot(LocalDate date, int shiftNumber, String snapshotJson, String username) {
        PlanDeChargeSnapshot snap = planDeChargeSnapshotRepository
                .findByShiftDateAndShiftNumber(date, shiftNumber)
                .orElseGet(PlanDeChargeSnapshot::new);
        LocalDateTime now = LocalDateTime.now();
        if (snap.getId() == null) {
            snap.setCreatedAt(now);
        }
        snap.setShiftDate(date);
        snap.setShiftNumber(shiftNumber);
        snap.setSnapshotJson(snapshotJson);
        snap.setUpdatedAt(now);
        snap.setCreatedBy(username);
        return planDeChargeSnapshotRepository.save(snap);
    }
}
