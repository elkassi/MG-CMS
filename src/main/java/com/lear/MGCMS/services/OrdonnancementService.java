package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.MachineQueue;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry;
import com.lear.MGCMS.repositories.MachineQueueRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository;
import com.lear.MGCMS.repositories.dispatcher.EngineScheduleEntryRepository;
import com.lear.MGCMS.repositories.scheduling.SchedulingConfigRepository;
import com.lear.MGCMS.services.dispatcher.SeriesOrderingStrategy;
import com.lear.MGCMS.services.dispatcher.DispatcherProperties;
import com.lear.MGCMS.services.dispatcher.ContinuousDispatchOptimizerService;
import com.lear.MGCMS.services.dispatcher.EngineState;
import com.lear.MGCMS.services.dispatcher.MaterialAvailabilityChecker;
import com.lear.MGCMS.services.dispatcher.LockResolver;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;
import com.lear.MGCMS.services.scheduling.DispatchAlgorithms;
import com.lear.MGCMS.services.scheduling.ShiftClock;
import com.lear.cms.repositories.TimingModelRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Optimized Ordonnancement Service — uses lightweight JPQL projections (19 columns per serie,
 * no @ManyToOne joins) and single-load caching to avoid N+1 query patterns.
 *
 * Performance compared to previous version:
 * - Series queries: 19 columns instead of 50+ columns, no CodeDefaut/CodeScrap/CodeDefautAdditionnel joins
 * - findAllInProgress/findAllNotYet: loaded ONCE per request (was called 5+ times)
 * - Machine statuses: 1 batch query (was N per-machine queries)
 * - TimingModel: 1 batch query (was N per-serie queries)
 * - Machines: 5 columns (was full entity with eager ManyToOne machineType, zone)
 */
@Service
public class OrdonnancementService {

    private static final Logger log = LoggerFactory.getLogger(OrdonnancementService.class);

    @Autowired
    private ProductionTableRepository productionTableRepository;

    @Autowired
    private CuttingRequestSerieDataRepository serieDataRepository;

    @Autowired
    private CuttingRequestDataRepository cuttingRequestDataRepository;

    @Autowired
    private CuttingRequestRepository cuttingRequestRepository;

    @Autowired
    private EngineScheduleEntryRepository engineScheduleEntryRepository;

    @Autowired
    private com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository boxInfoRepository;

    @Autowired
    private EtatMachineHistoriqueService etatMachineHistoriqueService;

    @Autowired
    private TimingModelRepository timingModelRepository;

    @Autowired
    private MachineQueueRepository machineQueueRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private SchedulingConfigRepository schedulingConfigRepository;

    @Autowired
    private CuttingTimeCalculator cuttingTimeCalculator;

    @Autowired
    private SeriesOrderingStrategy seriesOrderingStrategy;

    @Autowired
    private DispatcherProperties dispatcherProperties;

    @Autowired
    private ContinuousDispatchOptimizerService continuousDispatchOptimizerService;

    @Autowired
    private MaterialAvailabilityChecker materialAvailabilityChecker;

    @Autowired
    private ShiftClock shiftClock;

    // Configuration constants
    private static final double COEF_SPREADING_PER_METRE = 0.5;
    private static final double COEF_SETUP_TIME = 2.0;
    private static final double TABLE_LENGTH_DEFAULT = 14.0;
    private static final int SHIFT_DURATION_MINUTES = 460;

    // Machine type compatibility aliases
    private static final String MACHINE_TYPE_LECTRA = "Lectra";
    private static final String MACHINE_TYPE_LECTRA_IP6 = "Lectra IP6";

    // Dispatch-aware machine selection weights
    private static final double LOAD_WEIGHT = 1.0;
    private static final double SLOT_WEIGHT = 0.5;

    // Zone category cache (loaded lazily, refreshed on context reload)
    private volatile Map<String, Zone.Category> zoneCategories;

    // Timeline data cache (3 min TTL — covers all scheduled shift refreshes)
    private final Map<String, Object> timelineCache = new ConcurrentHashMap<>();
    private final Map<String, Long> timelineCacheTimestamps = new ConcurrentHashMap<>();
    private static final long TIMELINE_CACHE_TTL_MS = 180_000;

    /**
     * Invalidate all cached timeline data. Must be called after any operation
     * that changes sequence status, zone assignment, or dispatch state.
     */
    public void invalidateTimelineCache() {
        timelineCache.clear();
        timelineCacheTimestamps.clear();
        log.info("Timeline cache invalidated");
    }

    // ======================== MACHINE TYPE COMPATIBILITY ========================

    /**
     * Returns true if the actual machine type is compatible with the required type.
     * Requires exact match — "Lectra" and "Lectra IP6" are NOT interchangeable.
     */
    private static boolean isCompatibleMachineType(String required, String actual) {
        if (required == null || actual == null) return false;
        return required.equalsIgnoreCase(actual);
    }

    /**
     * Returns all machine types compatible with the given type.
     */
    private static Set<String> getCompatibleMachineTypes(String machineType) {
        Set<String> types = new LinkedHashSet<>();
        if (machineType == null) return types;
        String trimmed = machineType.trim();
        if (trimmed.isEmpty()) return types;
        types.add(trimmed);
        return types;
    }

    // ======================== LIGHTWEIGHT DTO ========================

    /**
     * Lightweight DTO for ordonnancement — only 19 fields.
     * Avoids loading full CuttingRequestSerieData (50+ fields, 3 eager @ManyToOne joins).
     */
    public static class SerieDTO {
        public String serie, sequence, machine, partNumberMaterial, description;
        public Double longueur;
        public Integer nbrCouche;
        public String placement;
        public Double tempsDeCoupe;
        public String tableCoupe, tableMatelassage;
        public String statusCoupe, statusMatelassage;
        public LocalDateTime dateDebutCoupe, dateFinCoupe, dateDebutMatelassage, dateFinMatelassage;
        public String zoneCoupe, zoneMatelassage;
        public LocalDate planningDate;
        public LocalDate dueDate;
        public Integer dueShift;
        public Double completionRatio;

        public static SerieDTO from(Object[] r) {
            SerieDTO d = new SerieDTO();
            d.serie = (String) r[0];
            d.sequence = (String) r[1];
            d.machine = (String) r[2];
            d.partNumberMaterial = (String) r[3];
            d.description = (String) r[4];
            d.longueur = r[5] != null ? ((Number) r[5]).doubleValue() : null;
            d.nbrCouche = r[6] != null ? ((Number) r[6]).intValue() : null;
            d.placement = (String) r[7];
            d.tempsDeCoupe = r[8] != null ? ((Number) r[8]).doubleValue() : null;
            d.tableCoupe = (String) r[9];
            d.tableMatelassage = (String) r[10];
            d.statusCoupe = (String) r[11];
            d.statusMatelassage = (String) r[12];
            d.dateDebutCoupe = (LocalDateTime) r[13];
            d.dateFinCoupe = (LocalDateTime) r[14];
            d.dateDebutMatelassage = (LocalDateTime) r[15];
            d.dateFinMatelassage = (LocalDateTime) r[16];
            d.zoneCoupe = (String) r[17];
            d.zoneMatelassage = (String) r[18];
            d.planningDate = (LocalDate) r[19];
            
            double pieces = r.length > 20 && r[20] != null ? ((Number) r[20]).doubleValue() : 0.0;
            double totalPieces = r.length > 21 && r[21] != null ? ((Number) r[21]).doubleValue() : 0.0;
            d.completionRatio = totalPieces > 0 ? pieces / totalPieces : 0.0;
            
            return d;
        }
    }

    // ======================== DATA LOADING HELPERS ========================

    /**
     * Load all relevant series for ordonnancement.
     * Strategy: active sequences due in the current plant bucket or older
     * (sequenceStatus = 'ACTIVE' or null) + sequences with recent activity
     * + manual sequences. Upcoming due buckets stay out of the current
     * workbench/next-series flow.
     */
    private List<SerieDTO> loadRelevantSeries(List<String> additionalSequences) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since12h = now.minusHours(12);

        Set<String> relevantSequences = new HashSet<>();

        // 1. Active sequences due in the current shift or older.
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        for (Object[] r : cuttingRequestRepository.findActiveDueOnOrBeforeLight(
                slot.date, String.valueOf(slot.shift))) {
            String seq = (String) r[0];
            if (seq != null) relevantSequences.add(seq);
        }

        // 2. Sequences with recent activity (12h bounded window) + at least one status started
        relevantSequences.addAll(serieDataRepository.findRelevantSequences(since12h, now));

        // 3. Manually added sequences by the user
        if (additionalSequences != null) {
            relevantSequences.addAll(additionalSequences);
        }

        if (relevantSequences.isEmpty()) return Collections.emptyList();

        // Load all series for relevant sequences (batched for SQL Server 2100 limit)
        List<String> seqList = new ArrayList<>(relevantSequences);
        List<SerieDTO> result = new ArrayList<>();
        for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
            for (Object[] row : serieDataRepository.findSeriesBySequencesLight(batch)) {
                result.add(SerieDTO.from(row));
            }
        }
        return result;
    }

    private Set<String> collectSequenceIds(List<SerieDTO> series) {
        Set<String> out = new HashSet<>();
        if (series == null) return out;
        for (SerieDTO s : series) {
            if (s.sequence != null) out.add(s.sequence);
        }
        return out;
    }

    private Map<String, Map<String, Object>> loadSequenceInfo(Set<String> sequenceIds) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        if (sequenceIds == null || sequenceIds.isEmpty()) return out;
        List<String> seqList = new ArrayList<>(sequenceIds);
        for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
            for (Object[] row : cuttingRequestDataRepository.findSequenceInfoLight(batch)) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("sequence", row[0]);
                info.put("zone", row[1]);
                info.put("dueDate", row[2] != null ? row[2].toString() : null);
                info.put("dueShift", row[3]);
                out.put((String) row[0], info);
            }
        }
        return out;
    }

    private void applySequenceInfo(List<SerieDTO> series, Map<String, Map<String, Object>> sequenceInfoMap) {
        if (series == null || sequenceInfoMap == null) return;
        for (SerieDTO s : series) {
            Map<String, Object> info = sequenceInfoMap.get(s.sequence);
            if (info == null) continue;
            if (info.get("dueDate") != null) {
                s.dueDate = LocalDate.parse((String) info.get("dueDate"));
            }
            if (info.get("dueShift") != null) {
                try {
                    s.dueShift = Integer.parseInt(info.get("dueShift").toString());
                } catch (NumberFormatException ignored) {
                    s.dueShift = null;
                }
            }
        }
    }

    /**
     * Split loaded series into inProgress / notYet / completed categories.
     */
    private Map<String, List<SerieDTO>> splitSeries(List<SerieDTO> allSeries,
                                                     LocalDateTime windowStart,
                                                     LocalDateTime windowEnd) {
        List<SerieDTO> inProgress = new ArrayList<>();
        List<SerieDTO> notYet = new ArrayList<>();
        List<SerieDTO> completed = new ArrayList<>();

        for (SerieDTO s : allSeries) {
            if (s.dateDebutCoupe != null && s.dateFinCoupe != null) {
                // Completed — only include if within time window
                if (!s.dateDebutCoupe.isAfter(windowEnd) && !s.dateFinCoupe.isBefore(windowStart)) {
                    completed.add(s);
                }
            } else if (s.dateDebutCoupe != null && s.dateFinCoupe == null) {
                inProgress.add(s);
            } else {
                notYet.add(s);
            }
        }

        Map<String, List<SerieDTO>> result = new HashMap<>();
        result.put("inProgress", inProgress);
        result.put("notYet", notYet);
        result.put("completed", completed);
        return result;
    }

    // ======================== SHIFT DETECTION ========================

    /**
     * Determine current shift: 1 (06:00-14:00), 2 (14:00-22:00), 3 (22:00-06:00).
     * Returns [date, dueShift] where date is the planning date and dueShift is numeric ("1","2","3").
     */
    private String[] getCurrentShiftInfo(LocalDateTime now) {
        int hour = now.getHour();
        LocalDate today = now.toLocalDate();

        int minutes = now.toLocalTime().getHour() * 60 + now.toLocalTime().getMinute();
        if (minutes >= 1310) {
            return new String[]{ today.plusDays(1).toString(), "1" };
        } else if (minutes < 350) {
            return new String[]{ today.toString(), "1" };
        } else if (minutes < 830) {
            return new String[]{ today.toString(), "2" };
        } else {
            return new String[]{ today.toString(), "3" };
        }
    }

    /**
     * Get previous shift info: 1→3(yesterday), 2→1(same day), 3→2(same day).
     */
    private String[] getPreviousShiftInfo(String dateStr, String shift) {
        LocalDate d = LocalDate.parse(dateStr);
        switch (shift) {
            case "1": return new String[]{ d.minusDays(1).toString(), "3" };
            case "2": return new String[]{ d.toString(), "1" };
            case "3": return new String[]{ d.toString(), "2" };
            default:  return new String[]{ d.toString(), "1" };
        }
    }

    /**
     * Get the shift start/end times for a given shift.
     * Returns [startHour, endHour] in 24h format.
     */
    private LocalDateTime getShiftEndTime(String dateStr, String shift) {
        LocalDate d = LocalDate.parse(dateStr);
        switch (shift) {
            case "1": return d.atTime(5, 50);   // Shift 1 ends at 05:50
            case "2": return d.atTime(13, 50);  // Shift 2 ends at 13:50
            case "3": return d.atTime(21, 50);  // Shift 3 ends at 21:50
            default:  return d.atTime(13, 50);
        }
    }

    // SQL Server max IN-clause parameters
    private static final int SQL_BATCH_SIZE = 2000;

    /**
     * Batch load placement -> cutting time map.
     * Batches into chunks of 2000 to respect SQL Server's 2100 parameter limit.
     */
    @SafeVarargs
    private final Map<String, Double> buildCuttingTimeMap(List<SerieDTO>... seriesLists) {
        Set<String> placements = new HashSet<>();
        for (List<SerieDTO> list : seriesLists) {
            for (SerieDTO s : list) {
                if (s.placement != null) placements.add(s.placement);
            }
        }
        Map<String, Double> map = new HashMap<>();
        if (!placements.isEmpty()) {
            List<String> placementList = new ArrayList<>(placements);
            for (int i = 0; i < placementList.size(); i += SQL_BATCH_SIZE) {
                List<String> batch = placementList.subList(i, Math.min(i + SQL_BATCH_SIZE, placementList.size()));
                for (Object[] row : timingModelRepository.findCuttingTimesByPlacements(batch)) {
                    if (row[0] != null && row[1] != null) {
                        map.put((String) row[0], ((Number) row[1]).doubleValue());
                    }
                }
            }
        }
        return map;
    }

    private Set<String> loadLaserDxfMachines() {
        return new HashSet<>(productionTableRepository.findLaserDxfMachineNames());
    }

    /**
     * Machines whose {@code machineType.name = 'Gerber'}. Used by
     * {@link #getEstimatedCuttingTime} to trigger the universal Gerber &times; 2
     * factor that {@link CuttingTimeCalculator} applies.
     */
    private Set<String> loadGerberMachines() {
        return new HashSet<>(productionTableRepository.findGerberMachineNames());
    }

    // ======================== DISPATCH-AWARE MACHINE HELPERS ========================

    /**
     * Lazy-load zone categories into a local cache.
     * Called once per request (lightweight — zones table is tiny).
     */
    private void ensureZoneCategories() {
        if (zoneCategories != null) return;
        Map<String, Zone.Category> map = new HashMap<>();
        for (Zone z : zoneRepository.findAll()) {
            if (z.getNom() != null) {
                map.put(z.getNom(), z.getCategory());
            }
        }
        zoneCategories = map;
    }

    private boolean isSharedZone(String zoneNom) {
        ensureZoneCategories();
        Zone.Category cat = zoneCategories.get(zoneNom);
        return cat == Zone.Category.SHARED;
    }

    /**
     * Batch-load dispatchedZone for a set of sequences.
     * Returns map: sequence → dispatchedZone.
     */
    private Map<String, String> loadDispatchZoneAssignments(Set<String> sequences) {
        if (sequences == null || sequences.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        List<String> seqList = new ArrayList<>(sequences);
        for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
            for (Object[] row : cuttingRequestRepository.findDispatchedZonesBySequences(batch)) {
                String seq = (String) row[0];
                String zone = (String) row[1];
                if (seq != null) {
                    result.put(seq, zone);
                }
            }
        }
        // Overlay engine's live bestAssignment when engine is running so
        // Planifiée coupe / Planifier matelassage reflect current proposals
        EngineState engineState = continuousDispatchOptimizerService.getState();
        Map<String, String> engineBest = continuousDispatchOptimizerService.getCurrentBestAssignment();
        if (!engineBest.isEmpty()) {
            for (String seq : sequences) {
                String engineZone = engineBest.get(seq);
                if (engineZone != null) {
                    result.put(seq, engineZone);
                }
            }
        }
        return result;
    }

    /**
     * Build zone → machineType → [machineName] pools from findAllMachinesLight results.
     * machinesRaw columns: 0=id, 1=nom, 2=zoneNom, 3=machineTypeName, 4=tableLength
     */
    private Map<String, Map<String, List<String>>> buildZoneMachinePools(List<Object[]> machinesRaw) {
        Map<String, Map<String, List<String>>> pools = new LinkedHashMap<>();
        for (Object[] row : machinesRaw) {
            String nom = (String) row[1];
            String zone = (String) row[2];
            String type = (String) row[3];
            if (nom == null || zone == null || type == null) continue;
            pools.computeIfAbsent(zone, k -> new LinkedHashMap<>())
                 .computeIfAbsent(type, k -> new ArrayList<>())
                 .add(nom);
        }
        return pools;
    }

    /**
     * Build machineName → machineTypeName reverse lookup.
     */
    private Map<String, String> buildMachineTypeByName(List<Object[]> machinesRaw) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Object[] row : machinesRaw) {
            String nom = (String) row[1];
            String type = (String) row[3];
            if (nom != null && type != null) {
                map.put(nom, type);
            }
        }
        return map;
    }

    /**
     * Build total assigned cutting time (minutes) per machine from already-assigned series.
     * Used for load-balancing: machines with lower total load are preferred.
     */
    private Map<String, Double> buildMachineLoads(
            List<SerieDTO> completed,
            List<SerieDTO> inProgress,
            Map<String, Double> cuttingTimeMap,
            Set<String> laserDxf,
            Set<String> gerber) {
        Map<String, Double> loads = new HashMap<>();
        for (SerieDTO s : completed) {
            if (s.tableCoupe != null && s.dateDebutCoupe != null && s.dateFinCoupe != null) {
                double mins = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                loads.merge(s.tableCoupe, mins, Double::sum);
            }
        }
        for (SerieDTO s : inProgress) {
            if (s.tableCoupe != null && s.dateDebutCoupe != null) {
                double mins = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                loads.merge(s.tableCoupe, mins, Double::sum);
            }
        }
        return loads;
    }

    /**
     * Infer machine type from existing assignments (tableCoupe/tableMatelassage)
     * or fall back to "Lectra". Used when serie.machine is null.
     */
    private String inferMachineType(SerieDTO serie, Set<String> laserDxf, Set<String> gerber) {
        String machineName = serie.tableCoupe;
        if (machineName == null) machineName = serie.tableMatelassage;
        if (machineName != null) {
            if (laserDxf.contains(machineName)) return "LASER-DXF";
            if (gerber.contains(machineName)) return "Gerber";
        }
        return "Lectra";
    }

    /**
     * Core dispatch-aware machine resolver.
     *
     * Priority pools:
     *   A. STRICT dispatched zone + matching machine type
     *   B. SHARED zones + matching machine type (if A empty)
     *   C. Any zone + matching machine type (if B empty)
     *   D. All active machines (ultimate backward-compat fallback)
     *
     * Scoring: primary = total load (lower = better balance),
     *          secondary = earliest available slot (lower = faster).
     */
    /**
     * Build filtered candidate machine sets for dispatch-aware selection.
     * Returns active machines of the required type, respecting zone dispatch.
     *
     * Priority: STRICT dispatched zone → SHARED zones → any zone → all active machines.
     */
    private Set<String> buildCandidateMachinePool(
            SerieDTO serie,
            String requiredMachineType,
            String dispatchedZone,
            Map<String, Map<String, List<String>>> zoneMachinePools,
            Set<String> activeMachines,
            Set<String> laserDxf,
            Set<String> gerber) {

        if (requiredMachineType == null) {
            requiredMachineType = inferMachineType(serie, laserDxf, gerber);
        }

        Set<String> compatibleTypes = getCompatibleMachineTypes(requiredMachineType);

        // Pool A: STRICT dispatched zone + matching type (exact first, then compatible)
        if (dispatchedZone != null) {
            Map<String, List<String>> typeMap = zoneMachinePools.get(dispatchedZone);
            if (typeMap != null) {
                Set<String> filtered = new LinkedHashSet<>();
                for (String type : compatibleTypes) {
                    if (typeMap.containsKey(type)) {
                        filtered.addAll(typeMap.get(type));
                    }
                }
                filtered.retainAll(activeMachines);
                if (!filtered.isEmpty()) return filtered;
            }
        }

        // Pool B: SHARED zones + matching type
        for (Map.Entry<String, Map<String, List<String>>> entry : zoneMachinePools.entrySet()) {
            if (isSharedZone(entry.getKey())) {
                Map<String, List<String>> typeMap = entry.getValue();
                if (typeMap != null) {
                    Set<String> filtered = new LinkedHashSet<>();
                    for (String type : compatibleTypes) {
                        if (typeMap.containsKey(type)) {
                            filtered.addAll(typeMap.get(type));
                        }
                    }
                    filtered.retainAll(activeMachines);
                    if (!filtered.isEmpty()) return filtered;
                }
            }
        }

        // Pool C: any zone with matching type
        for (Map<String, List<String>> typeMap : zoneMachinePools.values()) {
            Set<String> filtered = new LinkedHashSet<>();
            for (String type : compatibleTypes) {
                if (typeMap.containsKey(type)) {
                    filtered.addAll(typeMap.get(type));
                }
            }
            filtered.retainAll(activeMachines);
            if (!filtered.isEmpty()) return filtered;
        }

        // Pool D: all active machines (backward compatibility)
        return new LinkedHashSet<>(activeMachines);
    }

    /**
     * Core dispatch-aware machine resolver.
     *
     * Priority pools:
     *   A. STRICT dispatched zone + matching machine type
     *   B. SHARED zones + matching machine type (if A empty)
     *   C. Any zone + matching machine type (if B empty)
     *   D. All active machines (ultimate backward-compat fallback)
     *
     * Scoring: primary = total load (lower = better balance),
     *          secondary = earliest available slot (lower = faster).
     */
    private String resolveMachineForSerie(
            SerieDTO serie,
            String requiredMachineType,
            String dispatchedZone,
            Map<String, Map<String, List<String>>> zoneMachinePools,
            Map<String, String> machineTypeByName,
            Map<String, Double> machineLoads,
            Map<String, List<long[]>> cuttingOccupied,
            Set<String> activeMachines,
            long nowMs,
            long cuttingMs,
            Set<String> laserDxf,
            Set<String> gerber) {

        Set<String> candidates = buildCandidateMachinePool(
                serie, requiredMachineType, dispatchedZone,
                zoneMachinePools, activeMachines, laserDxf, gerber);

        String bestMachine = null;
        double bestScore = Double.MAX_VALUE;

        for (String machineNom : candidates) {
            if (!activeMachines.contains(machineNom)) continue;

            long earliestSlot = findEarliestSlot(cuttingOccupied.get(machineNom), nowMs, cuttingMs);
            double load = machineLoads.getOrDefault(machineNom, 0.0);

            double score = load * LOAD_WEIGHT
                         + ((earliestSlot - nowMs) / 60000.0) * SLOT_WEIGHT;

            if (score < bestScore) {
                bestScore = score;
                bestMachine = machineNom;
            }
        }

        return bestMachine;
    }

    // ======================== TIMELINE DATA ========================

    /**
     * Get full timeline data for all machines.
     * Uses smart loading: only series from relevant sequences (24h activity + due shifts + manual).
     */
    public Map<String, Object> getTimelineData(int hoursBack, int hoursForward, List<String> additionalSequences) {
        String cacheKey = hoursBack + "|" + hoursForward + "|" + (additionalSequences != null ? additionalSequences.hashCode() : "null");
        EngineState engineState = continuousDispatchOptimizerService != null
                ? continuousDispatchOptimizerService.getState() : null;
        boolean engineRunning = engineState == EngineState.WARMING || engineState == EngineState.IMPROVING;
        Long ts = timelineCacheTimestamps.get(cacheKey);
        if (!engineRunning && ts != null && System.currentTimeMillis() - ts < TIMELINE_CACHE_TTL_MS) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) timelineCache.get(cacheKey);
            if (cached != null) return cached;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(hoursBack);
        LocalDateTime windowEnd = now.plusHours(hoursForward);

        // ---- SMART LOAD: only relevant sequences ----
        List<SerieDTO> allSeries = loadRelevantSeries(additionalSequences);
        Set<String> allSequenceIds = collectSequenceIds(allSeries);
        Map<String, Map<String, Object>> sequenceInfoMap = loadSequenceInfo(allSequenceIds);
        applySequenceInfo(allSeries, sequenceInfoMap);

        Map<String, List<SerieDTO>> split = splitSeries(allSeries, windowStart, windowEnd);
        List<SerieDTO> inProgress = split.get("inProgress");
        List<SerieDTO> notYet = split.get("notYet");
        List<SerieDTO> completed = split.get("completed");

        // Sort notYet by dueDate, dueShift, then dateDebutMatelassage (earliest first, nulls last)
        notYet.sort(Comparator
                .comparing((SerieDTO s) -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparing(s -> s.dateDebutMatelassage != null ? s.dateDebutMatelassage : LocalDateTime.MAX));

        Map<String, Double> cuttingTimeMap = buildCuttingTimeMap(inProgress, notYet, completed);
        Set<String> laserDxf = loadLaserDxfMachines();
        Set<String> gerber = loadGerberMachines();

        // Machines: 5 columns per machine (id, nom, zone, machineType, tableLength)
        List<Object[]> machinesRaw = productionTableRepository.findAllMachinesLight();

        // Machine statuses: 1 batch query (was N per-machine queries)
        Map<String, String> machineStatuses = etatMachineHistoriqueService.getAllCurrentStatusCodes(now);

        // ---- BUILD RESPONSE from cached data ----
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, List<SerieDTO>> seriesByMachine = groupSeriesByMachine(inProgress, notYet);
        Map<String, List<Map<String, Object>>> machinesByZone =
                buildMachinesWithState(machinesRaw, machineStatuses, seriesByMachine, cuttingTimeMap, laserDxf, gerber);
        result.put("machinesByZone", machinesByZone);

        Set<String> allMachineNames = new LinkedHashSet<>();
        for (Object[] row : machinesRaw) {
            allMachineNames.add((String) row[1]);
        }

        List<Map<String, Object>> timelineBlocks =
                buildTimelineBlocks(completed, inProgress, notYet, cuttingTimeMap, laserDxf, gerber, allMachineNames, machineStatuses);
        applyEngineScheduleOverlay(timelineBlocks, allSequenceIds);
        result.put("timelineBlocks", timelineBlocks);

        // Compute per-machine lastFinCoupe (real or estimated) from timeline blocks
        Map<String, String> lastFinCoupePerMachine = new LinkedHashMap<>();
        for (Map<String, Object> block : timelineBlocks) {
            String machine = (String) block.get("tableCoupe");
            if (machine == null) machine = (String) block.get("assignedMachine");
            if (machine == null) continue;

            // Priority: estimatedFinCoupe (covers all statuses) > dateFinCoupe (real)
            String finCoupe = (String) block.get("estimatedFinCoupe");
            if (finCoupe == null) finCoupe = (String) block.get("dateFinCoupe");
            if (finCoupe == null) continue;

            String existing = lastFinCoupePerMachine.get(machine);
            if (existing == null || finCoupe.compareTo(existing) > 0) {
                lastFinCoupePerMachine.put(machine, finCoupe);
            }
        }
        // Inject lastFinCoupe into machinesByZone
        for (List<Map<String, Object>> machines : machinesByZone.values()) {
            for (Map<String, Object> m : machines) {
                String nom = (String) m.get("nom");
                m.put("lastFinCoupe", lastFinCoupePerMachine.get(nom));
            }
        }

        List<Map<String, Object>> unassignedSeries =
                buildUnassignedSeries(notYet, cuttingTimeMap, laserDxf, gerber);
        result.put("unassignedSeries", unassignedSeries);

        // Load real box counts per active sequence
        Set<String> activeSeqs = new HashSet<>();
        for (Map<String, Object> block : timelineBlocks) {
            if (!"COMPLETED".equals(block.get("status"))) {
                String seq = (String) block.get("sequence");
                if (seq != null) activeSeqs.add(seq);
            }
        }
        for (Map<String, Object> s : unassignedSeries) {
            String seq = (String) s.get("sequence");
            if (seq != null) activeSeqs.add(seq);
        }
        Map<String, Integer> boxCounts = loadBoxCounts(activeSeqs);
        result.put("metrics", calculateMetrics(timelineBlocks, unassignedSeries, machinesByZone, boxCounts));

        // Count active machines for shift overflow calculation
        long activeMachineCount = machinesByZone.values().stream()
                .flatMap(List::stream)
                .filter(m -> "M".equals(m.get("status")))
                .count();
        result.put("shiftOverflow", calculateShiftOverflow(inProgress, notYet, cuttingTimeMap, laserDxf, gerber, now, Math.max(1, activeMachineCount)));
        result.put("currentTime", now.toString());
        result.put("windowStart", windowStart.toString());
        result.put("windowEnd", windowEnd.toString());

        result.put("sequenceInfo", sequenceInfoMap);

        // Dispatch engine data — only when dispatcher is enabled
        if (dispatcherProperties != null && dispatcherProperties.isEnabled()) {
            result.put("dispatchEngineState", getDispatchEngineState());
            result.put("zoneAssignments", buildZoneAssignments(allSequenceIds, allSeries));
            result.put("materialAlerts", buildMaterialAlerts(allSeries));
        }

        if (!engineRunning) {
            timelineCache.put(cacheKey, result);
            timelineCacheTimestamps.put(cacheKey, System.currentTimeMillis());
        }
        return result;
    }

    /**
     * Incremental refresh: checks if anything changed in last N minutes.
     * If changes detected, returns full rebuilt timeline. If not, returns noChanges flag.
     * This avoids reloading full data on every 5-minute tick.
     */
    public Map<String, Object> getTimelineRefresh(int sinceMinutes, int hoursBack, int hoursForward,
                                                   List<String> additionalSequences) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(sinceMinutes);
        long changeCount = serieDataRepository.countRecentChanges(since);

        if (changeCount == 0) {
            // No changes — return minimal response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("noChanges", true);
            result.put("currentTime", LocalDateTime.now().toString());
            return result;
        }

        // Changes detected — do full relevant load
        return getTimelineData(hoursBack, hoursForward, additionalSequences);
    }

    /**
     * Load all series for a specific sequence (manual user addition).
     * Returns the series data so the frontend can merge it.
     */
    public Map<String, Object> loadSeriesForSequence(String sequenceId) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<SerieDTO> series = serieDataRepository.findSeriesBySequencesLight(Collections.singletonList(sequenceId))
                .stream().map(SerieDTO::from).collect(Collectors.toList());

        if (series.isEmpty()) {
            result.put("found", false);
            result.put("sequence", sequenceId);
            return result;
        }

        Set<String> laserDxf = loadLaserDxfMachines();
        Set<String> gerber = loadGerberMachines();
        Map<String, Double> cuttingTimeMap = buildCuttingTimeMap(series);

        List<Map<String, Object>> seriesList = new ArrayList<>();
        for (SerieDTO s : series) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("serie", s.serie);
            m.put("sequence", s.sequence);
            m.put("partNumberMaterial", s.partNumberMaterial);
            m.put("description", s.description);
            m.put("longueur", s.longueur);
            m.put("nbrCouche", s.nbrCouche);
            m.put("statusCoupe", s.statusCoupe);
            m.put("statusMatelassage", s.statusMatelassage);
            m.put("tableCoupe", s.tableCoupe);
            m.put("tableMatelassage", s.tableMatelassage);
            m.put("estimatedCuttingTime", getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber));
            seriesList.add(m);
        }

        result.put("found", true);
        result.put("sequence", sequenceId);
        result.put("seriesCount", series.size());
        result.put("series", seriesList);
        return result;
    }

    /**
     * Load full detail of a sequence: sequence info (dueDate, dueShift, zone) + all series
     * with all dates (real and estimated). Useful for the sequence detail modal.
     */
    public Map<String, Object> getSequenceDetail(String sequenceId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Load sequence header info
        CuttingRequestData seqData = cuttingRequestDataRepository.findBySequence(sequenceId);
        if (seqData == null) {
            result.put("found", false);
            result.put("sequence", sequenceId);
            return result;
        }

        result.put("found", true);
        result.put("sequence", sequenceId);
        result.put("dueDate", seqData.getDueDate() != null ? seqData.getDueDate().toString() : null);
        result.put("dueShift", seqData.getDueShift());
        result.put("zone", seqData.getZone() != null ? seqData.getZone().getNom() : null);
        result.put("projet", seqData.getProjet());
        result.put("modele", seqData.getModele());
        result.put("definition", seqData.getDefinition());
        result.put("planningDate", seqData.getPlanningDate() != null ? seqData.getPlanningDate().toString() : null);
        result.put("createdAt", seqData.getCreatedAt() != null ? seqData.getCreatedAt().toString() : null);

        // Load all series for this sequence
        List<SerieDTO> series = serieDataRepository.findSeriesBySequencesLight(Collections.singletonList(sequenceId))
                .stream().map(SerieDTO::from).collect(Collectors.toList());

        // Load full entities to get extra fields (matelassageEndroit, laize, config, drill)
        Map<String, CuttingRequestSerieData> fullDataBySerie = new HashMap<>();
        for (CuttingRequestSerieData full : serieDataRepository.findBySequence(sequenceId)) {
            if (full.getSerie() != null) {
                fullDataBySerie.put(full.getSerie(), full);
            }
        }

        Set<String> laserDxf = loadLaserDxfMachines();
        Set<String> gerber = loadGerberMachines();
        Map<String, Double> cuttingTimeMap = buildCuttingTimeMap(series);
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> seriesList = new ArrayList<>();
        int completedCount = 0, cuttingCount = 0, spreadingCount = 0, waitingCount = 0, incompleteCount = 0;

        for (SerieDTO s : series) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("serie", s.serie);
            m.put("partNumberMaterial", s.partNumberMaterial);
            m.put("description", s.description);
            m.put("longueur", s.longueur);
            m.put("nbrCouche", s.nbrCouche);
            m.put("placement", s.placement);
            m.put("machine", s.machine);
            m.put("statusCoupe", s.statusCoupe);
            m.put("statusMatelassage", s.statusMatelassage);
            m.put("tableCoupe", s.tableCoupe);
            m.put("tableMatelassage", s.tableMatelassage);
            m.put("zoneCoupe", s.zoneCoupe);
            m.put("zoneMatelassage", s.zoneMatelassage);
            // All real dates
            m.put("dateDebutMatelassage", s.dateDebutMatelassage != null ? s.dateDebutMatelassage.toString() : null);
            m.put("dateFinMatelassage", s.dateFinMatelassage != null ? s.dateFinMatelassage.toString() : null);
            m.put("dateDebutCoupe", s.dateDebutCoupe != null ? s.dateDebutCoupe.toString() : null);
            m.put("dateFinCoupe", s.dateFinCoupe != null ? s.dateFinCoupe.toString() : null);
            // Cutting time
            double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
            double spreadTime = estimateSpreadingTime(s);
            m.put("estimatedCuttingTime", cuttingTime);
            m.put("estimatedSpreadingTime", spreadTime);

            // Material status
            if (materialAvailabilityChecker != null && seqData.getZone() != null && s.partNumberMaterial != null) {
                Map<String, MaterialAvailabilityChecker.MaterialStatus> matStatus =
                        materialAvailabilityChecker.check(Collections.singleton(s.partNumberMaterial.trim()), seqData.getZone().getNom());
                m.put("materialStatus", matStatus.getOrDefault(s.partNumberMaterial.trim(), MaterialAvailabilityChecker.MaterialStatus.NOT_IN_ZONE).name());
            } else {
                m.put("materialStatus", "UNKNOWN");
            }

            // Estimate null dates
            if (s.dateDebutMatelassage == null) {
                m.put("estimatedDebutMatelassage", now.toString());
                m.put("estimatedFinMatelassage", now.plusMinutes((long) spreadTime).toString());
            } else if (s.dateFinMatelassage == null) {
                LocalDateTime estEnd = s.dateDebutMatelassage.plusMinutes((long) spreadTime);
                if (estEnd.isBefore(now)) estEnd = now.plusMinutes(2);
                m.put("estimatedFinMatelassage", estEnd.toString());
            }
            if (s.dateDebutCoupe == null) {
                LocalDateTime spreadEnd = s.dateFinMatelassage;
                if (spreadEnd == null) {
                    spreadEnd = s.dateDebutMatelassage != null
                            ? s.dateDebutMatelassage.plusMinutes((long) spreadTime)
                            : now.plusMinutes((long) spreadTime);
                }
                if (spreadEnd.isBefore(now)) spreadEnd = now;
                m.put("estimatedDebutCoupe", spreadEnd.toString());
                m.put("estimatedFinCoupe", spreadEnd.plusMinutes((long) cuttingTime).toString());
            } else if (s.dateFinCoupe == null) {
                LocalDateTime estEnd = s.dateDebutCoupe.plusMinutes((long) cuttingTime);
                if (estEnd.isBefore(now)) estEnd = now.plusMinutes(2);
                m.put("estimatedFinCoupe", estEnd.toString());
            }

            // Extra fields from full entity (matelassageEndroit, laize, config, drill)
            CuttingRequestSerieData full = fullDataBySerie.get(s.serie);
            if (full != null) {
                m.put("matelassageEndroit", full.getMatelassageEndroit());
                m.put("laize", full.getLaize());
                m.put("config", full.getConfig());
                String drill = full.getDrill();
                if (drill != null && drill.contains(",")) {
                    String[] parts = drill.split(",");
                    m.put("drill1", parts.length > 0 ? parts[0] : null);
                    m.put("drill2", parts.length > 1 ? parts[1] : null);
                } else {
                    m.put("drill1", drill);
                    m.put("drill2", null);
                }
            } else {
                m.put("matelassageEndroit", null);
                m.put("laize", null);
                m.put("config", null);
                m.put("drill1", null);
                m.put("drill2", null);
            }

            // Status counting
            if ("Complete".equals(s.statusCoupe)) completedCount++;
            else if ("In progress".equals(s.statusCoupe)) cuttingCount++;
            else if ("In progress".equals(s.statusMatelassage)) spreadingCount++;
            else if ("Incomplete".equals(s.statusMatelassage)) incompleteCount++;
            else waitingCount++;

            seriesList.add(m);
        }

        // Calculate totals for sequence completion time
        double totalRemainingCuttingTime = 0;
        double totalRemainingSpreadingTime = 0;
        String latestFinCoupe = null;

        for (Map<String, Object> m : seriesList) {
            String statusCoupe = (String) m.get("statusCoupe");
            if (!"Complete".equals(statusCoupe)) {
                totalRemainingCuttingTime += ((Number) m.get("estimatedCuttingTime")).doubleValue();
                totalRemainingSpreadingTime += ((Number) m.get("estimatedSpreadingTime")).doubleValue();
            }
            // Find latest finCoupe (real or estimated)
            String finCoupe = m.get("dateFinCoupe") != null ? (String) m.get("dateFinCoupe")
                             : (String) m.get("estimatedFinCoupe");
            if (finCoupe != null && (latestFinCoupe == null || finCoupe.compareTo(latestFinCoupe) > 0)) {
                latestFinCoupe = finCoupe;
            }
        }

        result.put("series", seriesList);
        result.put("seriesCount", series.size());
        result.put("completedCount", completedCount);
        result.put("cuttingCount", cuttingCount);
        result.put("spreadingCount", spreadingCount);
        result.put("waitingCount", waitingCount);
        result.put("incompleteCount", incompleteCount);
        result.put("totalRemainingCuttingTime", totalRemainingCuttingTime);
        result.put("totalRemainingSpreadingTime", totalRemainingSpreadingTime);
        result.put("estimatedCompletion", latestFinCoupe);

        // Table length available for each machine in the sequence zone
        String zoneNom = seqData.getZone() != null ? seqData.getZone().getNom() : null;
        if (zoneNom != null) {
            List<Object[]> machinesRaw = productionTableRepository.findAllMachinesLight();
            Map<String, String> machineStatuses = etatMachineHistoriqueService.getAllCurrentStatusCodes(now);
            List<Map<String, Object>> machinesInZone = new ArrayList<>();
            for (Object[] row : machinesRaw) {
                String machineZone = (String) row[2];
                if (!zoneNom.equals(machineZone)) continue;
                String nom = (String) row[1];
                double tableLength = row[4] != null ? ((Number) row[4]).doubleValue() : TABLE_LENGTH_DEFAULT;
                double occupied = calculateOccupiedLengthStandalone(nom);
                Map<String, Object> minfo = new LinkedHashMap<>();
                minfo.put("nom", nom);
                minfo.put("tableLength", tableLength);
                minfo.put("occupiedLength", occupied);
                minfo.put("availableLength", Math.max(0, tableLength - occupied));
                minfo.put("status", machineStatuses.getOrDefault(nom, "M"));
                machinesInZone.add(minfo);
            }
            result.put("machinesInZone", machinesInZone);
        }

        return result;
    }

    // ======================== CHANGE SERIE STATUS ========================

    /**
     * Change the statusMatelassage or statusCoupe of a serie.
     * Allowed statusMatelassage values: Waiting, In progress, Complete, Incomplete
     * Allowed statusCoupe values: Waiting, In progress, Complete
     */
    public Map<String, Object> changeSerieStatus(String serieId, String field, String newStatus) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Validate field
        if (!"statusMatelassage".equals(field) && !"statusCoupe".equals(field)) {
            result.put("success", false);
            result.put("error", "Champ invalide: " + field);
            return result;
        }

        // Validate allowed status values
        List<String> allowedMat = Arrays.asList("Waiting", "In progress", "Complete", "Incomplete");
        List<String> allowedCoupe = Arrays.asList("Waiting", "In progress", "Complete");
        List<String> allowed = "statusMatelassage".equals(field) ? allowedMat : allowedCoupe;
        if (!allowed.contains(newStatus)) {
            result.put("success", false);
            result.put("error", "Statut invalide: " + newStatus + ". Valeurs autorisées: " + allowed);
            return result;
        }

        CuttingRequestSerieData serie = serieDataRepository.findBySerie(serieId);
        if (serie == null) {
            result.put("success", false);
            result.put("error", "Série non trouvée: " + serieId);
            return result;
        }

        String oldStatus;
        if ("statusMatelassage".equals(field)) {
            oldStatus = serie.getStatusMatelassage();
            serie.setStatusMatelassage(newStatus);
        } else {
            oldStatus = serie.getStatusCoupe();
            serie.setStatusCoupe(newStatus);
        }
        serieDataRepository.save(serie);

        // If we just marked a serie's statusCoupe as Complete, check if the whole sequence is now done
        if ("statusCoupe".equals(field) && "Complete".equals(newStatus)) {
            Integer nonFinished = serieDataRepository.countNonFinishedBySequence(serie.getSequence());
            if (nonFinished != null && nonFinished == 0) {
                CuttingRequestData seq = cuttingRequestDataRepository.findBySequence(serie.getSequence());
                if (seq != null && !"COMPLETED".equals(seq.getSequenceStatus())) {
                    seq.setSequenceStatus("COMPLETED");
                    cuttingRequestDataRepository.save(seq);
                }
            }
        }

        result.put("success", true);
        result.put("serie", serieId);
        result.put("field", field);
        result.put("oldStatus", oldStatus);
        result.put("newStatus", newStatus);
        return result;
    }

    // ======================== MACHINES WITH STATE ========================

    /**
     * Build machines grouped by zone with state from pre-loaded data.
     * No additional queries — everything computed in-memory.
     */
    private Map<String, List<Map<String, Object>>> buildMachinesWithState(
            List<Object[]> machinesRaw,
            Map<String, String> machineStatuses,
            Map<String, List<SerieDTO>> seriesByMachine,
            Map<String, Double> cuttingTimeMap,
            Set<String> laserDxf,
            Set<String> gerber) {

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        for (Object[] row : machinesRaw) {
            Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
            String nom = (String) row[1];
            String zone = (String) row[2];
            String machineType = (String) row[3];
            double tableLength = row[4] != null ? ((Number) row[4]).doubleValue() : TABLE_LENGTH_DEFAULT;

            result.computeIfAbsent(zone, k -> new ArrayList<>());

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("nom", nom);
            info.put("id", id);
            info.put("zone", zone);
            info.put("machineType", machineType);
            info.put("tableLength", tableLength);

            // Status from batch query (default "M" if not found)
            info.put("status", machineStatuses.getOrDefault(nom, "M"));

            // Occupied length computed from pre-loaded series (no extra queries)
            double occupied = calculateOccupiedLength(nom, seriesByMachine, cuttingTimeMap, laserDxf, gerber);
            info.put("occupiedLength", occupied);
            info.put("availableLength", Math.max(0, tableLength - occupied));

            result.get(zone).add(info);
        }

        // machinesRaw is already ordered by zone, nom from the query
        return result;
    }

    /**
     * Standalone version for one-off calls (e.g., from getMachineStates).
     */
    private Map<String, List<Map<String, Object>>> buildMachinesWithState() {
        LocalDateTime now = LocalDateTime.now();
        List<SerieDTO> allSeries = loadRelevantSeries(Collections.emptyList());
        Map<String, List<SerieDTO>> split = splitSeries(allSeries, now.minusHours(12), now.plusHours(12));
        List<SerieDTO> inProgress = split.get("inProgress");
        List<SerieDTO> notYet = split.get("notYet");
        Map<String, Double> cuttingTimeMap = buildCuttingTimeMap(inProgress, notYet);
        Set<String> laserDxf = loadLaserDxfMachines();
        Set<String> gerber = loadGerberMachines();
        List<Object[]> machinesRaw = productionTableRepository.findAllMachinesLight();
        Map<String, String> machineStatuses = etatMachineHistoriqueService.getAllCurrentStatusCodes(now);
        Map<String, List<SerieDTO>> seriesByMachine = groupSeriesByMachine(inProgress, notYet);
        return buildMachinesWithState(machinesRaw, machineStatuses, seriesByMachine, cuttingTimeMap, laserDxf, gerber);
    }

    // ======================== OCCUPANCY CALCULATION ========================

    /**
     * Calculate occupied length from pre-loaded data. No database queries.
     */
    private double calculateOccupiedLength(String machineNom,
                                           Map<String, List<SerieDTO>> seriesByMachine,
                                           Map<String, Double> cuttingTimeMap,
                                           Set<String> laserDxf,
                                           Set<String> gerber) {
        List<SerieDTO> onMachine = seriesByMachine.getOrDefault(machineNom, Collections.emptyList());
        double occupied = 0;
        LocalDateTime now = LocalDateTime.now();

        for (SerieDTO s : onMachine) {
            double longueur = s.longueur != null ? s.longueur : 0;

            if ("In progress".equals(s.statusCoupe) && s.dateDebutCoupe != null) {
                double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                if (cuttingTime > 0) {
                    long elapsedSeconds = Duration.between(s.dateDebutCoupe, now).getSeconds();
                    double progress = Math.min(1.0, (elapsedSeconds / 60.0) / cuttingTime);
                    occupied += longueur * (1 - progress);
                }
            } else if ("Waiting".equals(s.statusCoupe)) {
                occupied += longueur;
            }
        }
        return occupied;
    }

    /**
     * Standalone version for one-off calls (e.g., from assignSerieToMachine).
     * Uses direct machine query for accurate occupancy (not filtered by 24h).
     */
    private double calculateOccupiedLengthStandalone(String machineNom) {
        List<SerieDTO> onMachine = serieDataRepository.findSeriesOnMachineLight(machineNom).stream()
                .map(SerieDTO::from).collect(Collectors.toList());
        Set<String> laserDxf = loadLaserDxfMachines();
        Set<String> gerber = loadGerberMachines();
        Map<String, Double> cuttingTimeMap = buildCuttingTimeMap(onMachine);

        double occupied = 0;
        LocalDateTime now = LocalDateTime.now();
        for (SerieDTO s : onMachine) {
            double longueur = s.longueur != null ? s.longueur : 0;
            if ("In progress".equals(s.statusCoupe) && s.dateDebutCoupe != null) {
                double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                if (cuttingTime > 0) {
                    long elapsedSeconds = Duration.between(s.dateDebutCoupe, now).getSeconds();
                    double progress = Math.min(1.0, (elapsedSeconds / 60.0) / cuttingTime);
                    occupied += longueur * (1 - progress);
                }
            } else if ("Waiting".equals(s.statusCoupe)) {
                occupied += longueur;
            }
        }
        return occupied;
    }

    /**
     * Filter pre-loaded series to those physically present on a machine table.
     */
    private List<SerieDTO> getSeriesOnMachine(String machineNom,
                                              List<SerieDTO> inProgress,
                                              List<SerieDTO> notYet) {
        List<SerieDTO> result = new ArrayList<>();
        for (SerieDTO s : inProgress) {
            if (machineNom.equals(s.tableCoupe)) result.add(s);
        }
        for (SerieDTO s : notYet) {
            if (machineNom.equals(s.tableCoupe) && "Complete".equals(s.statusMatelassage)) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Pre-group series by machine name to avoid O(M×S) scans in buildMachinesWithState.
     */
    private Map<String, List<SerieDTO>> groupSeriesByMachine(List<SerieDTO> inProgress,
                                                              List<SerieDTO> notYet) {
        Map<String, List<SerieDTO>> map = new HashMap<>();
        for (SerieDTO s : inProgress) {
            if (s.tableCoupe != null) {
                map.computeIfAbsent(s.tableCoupe, k -> new ArrayList<>()).add(s);
            }
        }
        for (SerieDTO s : notYet) {
            if (s.tableCoupe != null && "Complete".equals(s.statusMatelassage)) {
                map.computeIfAbsent(s.tableCoupe, k -> new ArrayList<>()).add(s);
            }
        }
        return map;
    }

    // ======================== CUTTING TIME ========================

    /**
     * Get estimated cutting time by delegating to {@link CuttingTimeCalculator}
     * (the single source of truth shared with {@code PlanDeChargeService}).
     *
     * <p>{@code cuttingTimeMap} carries already-resolved TimingModel values
     * (validated OR real, via the repository's {@code COALESCE}). We feed it
     * into the calculator as the {@code validated} slot so the bean treats it
     * as an authoritative timing-model reading — which means the LASER-DXF
     * layer multiplier correctly does NOT re-apply to it (the timing-model
     * value already bakes layers in). The Gerber &times; 2 factor is applied
     * universally by the bean, which fixes a long-standing drift where this
     * service was missing it.</p>
     */
    private double getEstimatedCuttingTime(SerieDTO serie,
                                           Map<String, Double> cuttingTimeMap,
                                           Set<String> laserDxf,
                                           Set<String> gerber) {
        Map<String, CuttingTimeCalculator.TimingRow> tim;
        if (serie.placement != null && cuttingTimeMap != null) {
            Double v = cuttingTimeMap.get(serie.placement);
            if (v != null) {
                tim = Collections.singletonMap(
                        serie.placement,
                        new CuttingTimeCalculator.TimingRow(v, null));
            } else {
                tim = Collections.emptyMap();
            }
        } else {
            tim = Collections.emptyMap();
        }

        // Derive the machineType string the bean needs from the two lookup sets.
        // Anything not matching LASER-DXF or Gerber is passed as "Lectra"
        // (a safe no-op label — neither post-adjustment triggers).
        String machineType = "Lectra";
        if (serie.tableCoupe != null) {
            if (laserDxf != null && laserDxf.contains(serie.tableCoupe)) {
                machineType = "LASER-DXF";
            } else if (gerber != null && gerber.contains(serie.tableCoupe)) {
                machineType = "Gerber";
            }
        }

        double resolved = cuttingTimeCalculator.resolveMinutes(
            serie.placement, serie.tempsDeCoupe, serie.nbrCouche, machineType, tim);

        // Historical production copies can miss timing rows; keep scheduling usable by
        // preventing zero-length cutting windows in recommendations and saved queues.
        return resolved > 0 ? resolved : 1.0;
    }

    private void applyEngineScheduleOverlay(List<Map<String, Object>> timelineBlocks,
                                            Set<String> sequenceIds) {
        if (timelineBlocks == null || timelineBlocks.isEmpty()
                || sequenceIds == null || sequenceIds.isEmpty()
                || engineScheduleEntryRepository == null) {
            return;
        }

        Map<String, Map<EngineScheduleEntry.Phase, EngineScheduleEntry>> bySerie = new HashMap<>();
        List<String> seqList = new ArrayList<>(sequenceIds);
        for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
            for (EngineScheduleEntry entry : engineScheduleEntryRepository.findBySequences(batch)) {
                if (entry.getId() == null || entry.getId().getSerieId() == null
                        || entry.getId().getPhase() == null) {
                    continue;
                }
                bySerie.computeIfAbsent(entry.getId().getSerieId(), k -> new EnumMap<>(EngineScheduleEntry.Phase.class))
                        .put(entry.getId().getPhase(), entry);
            }
        }
        if (bySerie.isEmpty()) return;

        for (Map<String, Object> block : timelineBlocks) {
            String serie = block.get("serie") != null ? String.valueOf(block.get("serie")) : null;
            if (serie == null) continue;
            Map<EngineScheduleEntry.Phase, EngineScheduleEntry> entries = bySerie.get(serie);
            if (entries == null) continue;

            boolean completed = "COMPLETED".equals(block.get("status"))
                    || "Complete".equalsIgnoreCase(String.valueOf(block.get("statusCoupe")));
            boolean hasRealMat = hasValue(block.get("dateDebutMatelassage"));
            boolean hasRealCoupe = hasValue(block.get("dateDebutCoupe"));

            EngineScheduleEntry mat = entries.get(EngineScheduleEntry.Phase.MATELASSAGE);
            if (!hasRealMat && mat != null) {
                if (mat.getMachineNom() != null) {
                    block.put("tableMatelassage", mat.getMachineNom());
                }
                if (mat.getZoneNom() != null) {
                    block.put("zoneMatelassage", mat.getZoneNom());
                }
                putDate(block, "estimatedDebutMatelassage", mat.getPlannedStart());
                putDate(block, "estimatedFinMatelassage", mat.getPlannedEnd());
                putDate(block, "estimatedSpreadStart", mat.getPlannedStart());
                putDate(block, "estimatedSpreadEnd", mat.getPlannedEnd());
            }

            EngineScheduleEntry coupe = entries.get(EngineScheduleEntry.Phase.COUPE);
            if (!completed && !hasRealCoupe && coupe != null) {
                if (coupe.getMachineNom() != null) {
                    block.put("tableCoupe", coupe.getMachineNom());
                    block.put("assignedMachine", coupe.getMachineNom());
                }
                if (coupe.getZoneNom() != null) {
                    block.put("zoneCoupe", coupe.getZoneNom());
                }
                putDate(block, "estimatedDebutCoupe", coupe.getPlannedStart());
                putDate(block, "estimatedFinCoupe", coupe.getPlannedEnd());
                putDate(block, "estimatedCoupeStart", coupe.getPlannedStart());
                putDate(block, "estimatedCoupeEnd", coupe.getPlannedEnd());
                block.put("engineScheduled", true);
            }
        }
    }

    private boolean hasValue(Object value) {
        return value != null && !String.valueOf(value).trim().isEmpty()
                && !"null".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private void putDate(Map<String, Object> target, String key, LocalDateTime value) {
        if (value != null) {
            target.put(key, value.toString());
        }
    }

    // ======================== TIMELINE BLOCKS ========================

    /**
     * Build timeline blocks from pre-loaded data. No additional queries.
     * Handles all 4 serie states with proper date estimation:
     * 1. dateFinCoupe != null && dateDebutCoupe != null → COMPLETED (show as-is)
     * 2. dateFinCoupe == null && dateDebutCoupe != null → CUTTING (estimate end)
     * 3. dateDebutCoupe == null:
     *    a. statusMatelassage = Complete → READY_TO_CUT (assign to tableCoupe or tableMatelassage)
     *    b. statusMatelassage = In progress → SPREADING (estimate end, then estimate coupe start/end)
     *    c. statusMatelassage = Waiting → WAITING (estimate all dates from current time)
     *
     * COLLISION-FREE ESTIMATION: All estimated cutting dates are scheduled
     * sequentially per machine (no overlap). Within each machine, series are
     * ordered by dateDebutMatelassage (earliest first). This mirrors the
     * autoDispatch() logic but for the regular timeline/table view.
     */
    private List<Map<String, Object>> buildTimelineBlocks(
            List<SerieDTO> completed,
            List<SerieDTO> inProgress,
            List<SerieDTO> notYet,
            Map<String, Double> cuttingTimeMap,
            Set<String> laserDxf,
            Set<String> gerber,
            Set<String> allMachineNames,
            Map<String, String> machineStatuses) {

        List<Map<String, Object>> blocks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        long nowMs = toMs(now);

        // Machine type lookup (needed for compatibility validation across all statuses)
        List<Object[]> machinesRaw = productionTableRepository.findAllMachinesLight();
        Map<String, String> machineTypeByName = buildMachineTypeByName(machinesRaw);

        // Per-machine occupied intervals for collision-free estimation
        Map<String, List<long[]>> cuttingOccupied = new LinkedHashMap<>();
        Map<String, List<long[]>> spreadingOccupied = new LinkedHashMap<>();

        // 1. COMPLETED: show as-is, register cutting intervals
        for (SerieDTO s : completed) {
            if (s.dateDebutCoupe != null && s.dateFinCoupe != null) {
                blocks.add(buildBlock(s, "COMPLETED", s.dateDebutCoupe, s.dateFinCoupe, cuttingTimeMap, laserDxf, gerber));
                if (s.tableCoupe != null) {
                    cuttingOccupied.computeIfAbsent(s.tableCoupe, k -> new ArrayList<>())
                            .add(new long[]{toMs(s.dateDebutCoupe), toMs(s.dateFinCoupe)});
                }
            }
        }

        // 2. CUTTING: estimate end, register interval, expose estimatedFinCoupe
        for (SerieDTO s : inProgress) {
            double estimatedTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
            LocalDateTime estimatedEnd = s.dateDebutCoupe.plusMinutes((long) estimatedTime);
            if (estimatedEnd.isBefore(now)) {
                estimatedEnd = now.plusMinutes(5); // still in progress but overdue
            }

            Map<String, Object> block = buildBlock(s, "CUTTING", s.dateDebutCoupe, estimatedEnd, cuttingTimeMap, laserDxf, gerber);
            block.put("estimatedFinCoupe", estimatedEnd.toString());
            block.put("estimatedCoupeEnd", estimatedEnd.toString());
            blocks.add(block);

            // Register this cutting interval so subsequent series don't overlap
            if (s.tableCoupe != null) {
                cuttingOccupied.computeIfAbsent(s.tableCoupe, k -> new ArrayList<>())
                        .add(new long[]{toMs(s.dateDebutCoupe), toMs(estimatedEnd)});
            }
        }

        // Register existing spreading intervals (from COMPLETED and CUTTING with matelassage dates)
        for (SerieDTO s : completed) {
            if (s.dateDebutMatelassage != null && s.tableMatelassage != null) {
                long startMs = toMs(s.dateDebutMatelassage);
                long endMs = s.dateFinMatelassage != null ? toMs(s.dateFinMatelassage)
                        : startMs + (long) (estimateSpreadingTime(s) * 60000);
                spreadingOccupied.computeIfAbsent(s.tableMatelassage, k -> new ArrayList<>())
                        .add(new long[]{startMs, endMs});
            }
        }
        for (SerieDTO s : inProgress) {
            if (s.dateDebutMatelassage != null && s.tableMatelassage != null) {
                long startMs = toMs(s.dateDebutMatelassage);
                long endMs = s.dateFinMatelassage != null ? toMs(s.dateFinMatelassage)
                        : startMs + (long) (estimateSpreadingTime(s) * 60000);
                if (endMs < nowMs) endMs = nowMs + 120000;
                spreadingOccupied.computeIfAbsent(s.tableMatelassage, k -> new ArrayList<>())
                        .add(new long[]{startMs, endMs});
            }
        }

        // Sort notYet by dueDate, dueShift, then dateDebutMatelassage (earliest first, nulls last)
        List<SerieDTO> sortedNotYet = new ArrayList<>(notYet);
        sortedNotYet.sort(Comparator
                .comparing((SerieDTO s) -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparing(s -> s.dateDebutMatelassage != null ? s.dateDebutMatelassage : LocalDateTime.MAX));

        // 3a. READY_TO_CUT: statusMatelassage = Complete, dateDebutCoupe = null
        //     Machine resolution rule:
        //       - If tableCoupe is non-null/non-empty → use tableCoupe (physical move already done)
        //       - Else if tableMatelassage is non-null → use tableMatelassage (serie stays on same table)
        //     Sequential per machine — ordered by dateDebutMatelassage (oldest first)
        for (SerieDTO s : sortedNotYet) {
            if ("Complete".equals(s.statusMatelassage) && s.dateDebutCoupe == null) {
                // Prefer explicit tableCoupe; fall back to tableMatelassage (serie stays on same table)
                boolean hasExplicitCoupe = s.tableCoupe != null && !s.tableCoupe.isEmpty();
                String machine = hasExplicitCoupe ? s.tableCoupe : s.tableMatelassage;
                // Validate tableMatelassage compatibility with serie's required machine type
                if (!hasExplicitCoupe && machine != null) {
                    String matType = machineTypeByName.get(machine);
                    if (!isCompatibleMachineType(s.machine, matType)) {
                        machine = null; // incompatible — do not schedule on this table
                    }
                }
                if (machine == null) continue;

                double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                long cuttingMs = (long) (cuttingTime * 60000);

                // Earliest coupe start: after matelassage ends, and not before now
                long earliestStart = nowMs;
                if (s.dateFinMatelassage != null && toMs(s.dateFinMatelassage) > earliestStart) {
                    earliestStart = toMs(s.dateFinMatelassage);
                }

                // Find earliest non-colliding cutting slot on this machine
                long coupeStartMs = findEarliestSlot(
                        cuttingOccupied.getOrDefault(machine, new ArrayList<>()), earliestStart, cuttingMs);
                long coupeEndMs = coupeStartMs + cuttingMs;

                // Register occupied interval so the next serie starts after this one
                cuttingOccupied.computeIfAbsent(machine, k -> new ArrayList<>())
                        .add(new long[]{coupeStartMs, coupeEndMs});

                LocalDateTime estCoupeStart = fromMsToLDT(coupeStartMs);
                LocalDateTime estCoupeEnd = fromMsToLDT(coupeEndMs);

                Map<String, Object> block = buildBlock(s, "READY_TO_CUT", estCoupeStart, estCoupeEnd, cuttingTimeMap, laserDxf, gerber);
                // Always resolve tableCoupe to the actual machine (may override null from DTO)
                block.put("tableCoupe", machine);
                block.put("assignedMachine", machine);
                block.put("estimatedCoupeStart", estCoupeStart.toString());
                block.put("estimatedCoupeEnd", estCoupeEnd.toString());
                block.put("estimatedDebutCoupe", estCoupeStart.toString());
                block.put("estimatedFinCoupe", estCoupeEnd.toString());
                blocks.add(block);
            }
        }

        // 3b. SPREADING: statusMatelassage = In progress (still spreading)
        //     Estimate matelassage end, then find collision-free coupe slot after spreading finishes
        for (SerieDTO s : sortedNotYet) {
            if ("In progress".equals(s.statusMatelassage) && s.dateDebutCoupe == null) {
                double spreadTime = estimateSpreadingTime(s);
                LocalDateTime spreadStart = s.dateDebutMatelassage != null ? s.dateDebutMatelassage : now;
                LocalDateTime estimatedSpreadEnd = spreadStart.plusMinutes((long) spreadTime);
                if (estimatedSpreadEnd.isBefore(now)) {
                    estimatedSpreadEnd = now.plusMinutes(5); // overdue
                }
                long spreadEndMs = toMs(estimatedSpreadEnd);

                // Estimate coupe start after spreading finishes — collision-free on the machine
                String machine = s.tableCoupe != null ? s.tableCoupe : s.tableMatelassage;
                // Validate tableMatelassage compatibility with serie's required machine type
                if (s.tableCoupe == null && machine != null) {
                    String matType = machineTypeByName.get(machine);
                    if (!isCompatibleMachineType(s.machine, matType)) {
                        machine = null; // incompatible — do not schedule cutting on this table
                    }
                }
                double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                long cuttingMs = (long) (cuttingTime * 60000);

                long coupeStartMs;
                if (machine != null) {
                    coupeStartMs = findEarliestSlot(
                            cuttingOccupied.getOrDefault(machine, new ArrayList<>()),
                            Math.max(nowMs, spreadEndMs), cuttingMs);
                    cuttingOccupied.computeIfAbsent(machine, k -> new ArrayList<>())
                            .add(new long[]{coupeStartMs, coupeStartMs + cuttingMs});
                } else {
                    coupeStartMs = Math.max(nowMs, spreadEndMs);
                }
                long coupeEndMs = coupeStartMs + cuttingMs;

                LocalDateTime estCoupeStart = fromMsToLDT(coupeStartMs);
                LocalDateTime estCoupeEnd = fromMsToLDT(coupeEndMs);

                Map<String, Object> block = buildBlock(s, "SPREADING", spreadStart, estimatedSpreadEnd, cuttingTimeMap, laserDxf, gerber);
                block.put("estimatedCoupeStart", estCoupeStart.toString());
                block.put("estimatedCoupeEnd", estCoupeEnd.toString());
                block.put("estimatedDebutCoupe", estCoupeStart.toString());
                block.put("estimatedFinCoupe", estCoupeEnd.toString());
                if (machine != null) block.put("assignedMachine", machine);
                blocks.add(block);
            }
        }

        // 3c. WAITING: statusMatelassage = Waiting, all dates null
        //     TWO-PASS approach per user specification:
        //     PASS 1: Determine tableCoupe + dateDebutCoupe/dateFinCoupe (collision-free, SCG order)
        //             Only assign to machines with status "M" (active).
        //     PASS 2: Estimate matelassage dates by chaining on the tableMatelassage (=inferred
        //             tableCoupe for WAITING), ordered by estimated dateDebutCoupe.
        //             dateDebutMatelassage = last dateFinMatelassage on that tableMatelassage (>= now)
        //             dateFinMatelassage  = dateDebutMatelassage + spreadingTime

        // Build active machine set (only "M" status)
        Set<String> activeMachines = new LinkedHashSet<>();
        if (allMachineNames != null && machineStatuses != null) {
            for (String mn : allMachineNames) {
                if ("M".equals(machineStatuses.getOrDefault(mn, "M"))) {
                    activeMachines.add(mn);
                }
            }
        }

        // ---- DISPATCH-AWARE MACHINE SELECTION DATA ----
        Set<String> allSequences = new HashSet<>();
        for (SerieDTO s : completed) allSequences.add(s.sequence);
        for (SerieDTO s : inProgress) allSequences.add(s.sequence);
        for (SerieDTO s : notYet) allSequences.add(s.sequence);

        Map<String, String> dispatchZones = loadDispatchZoneAssignments(allSequences);
        Map<String, Map<String, List<String>>> zoneMachinePools = buildZoneMachinePools(machinesRaw);
        Map<String, Double> machineLoads = buildMachineLoads(completed, inProgress, cuttingTimeMap, laserDxf, gerber);

        // Build sequence → cutting machine affinity from all processed series
        Map<String, String> seqMachineAffinity = new LinkedHashMap<>();
        for (SerieDTO s : completed) {
            if (s.sequence != null && s.tableCoupe != null)
                seqMachineAffinity.putIfAbsent(s.sequence, s.tableCoupe);
        }
        for (SerieDTO s : inProgress) {
            if (s.sequence != null && s.tableCoupe != null)
                seqMachineAffinity.putIfAbsent(s.sequence, s.tableCoupe);
        }
        for (SerieDTO s : sortedNotYet) {
            if (s.sequence != null && s.dateDebutCoupe == null) {
                String m = s.tableCoupe != null ? s.tableCoupe : s.tableMatelassage;
                // Only accept tableMatelassage if its machine type is compatible with serie's required type
                if (m != null && s.tableCoupe == null && s.tableMatelassage != null) {
                    String matType = machineTypeByName.get(s.tableMatelassage);
                    if (!isCompatibleMachineType(s.machine, matType)) {
                        m = null; // incompatible type — discard fallback
                    }
                }
                if (m != null) seqMachineAffinity.putIfAbsent(s.sequence, m);
            }
        }

        // Collect WAITING series
        List<SerieDTO> waitingList = new ArrayList<>();
        for (SerieDTO s : sortedNotYet) {
            if ("Waiting".equals(s.statusMatelassage) && s.dateDebutCoupe == null
                    && s.dateDebutMatelassage == null) {
                waitingList.add(s);
            }
        }

        if (!waitingList.isEmpty()) {
            // Count total and done series per sequence (for SCG ordering)
            Map<String, Integer> totalSeriesPerSeq = new LinkedHashMap<>();
            Map<String, Integer> doneSeriesPerSeq = new LinkedHashMap<>();
            for (SerieDTO s : completed) {
                if (s.sequence != null) {
                    totalSeriesPerSeq.merge(s.sequence, 1, Integer::sum);
                    doneSeriesPerSeq.merge(s.sequence, 1, Integer::sum);
                }
            }
            for (SerieDTO s : inProgress) {
                if (s.sequence != null) {
                    totalSeriesPerSeq.merge(s.sequence, 1, Integer::sum);
                    doneSeriesPerSeq.merge(s.sequence, 1, Integer::sum);
                }
            }
            for (SerieDTO s : notYet) {
                if (s.sequence != null) {
                    totalSeriesPerSeq.merge(s.sequence, 1, Integer::sum);
                    if (!"Waiting".equals(s.statusMatelassage)) {
                        doneSeriesPerSeq.merge(s.sequence, 1, Integer::sum);
                    }
                }
            }

            Map<String, Integer> waitingCountPerSeq = new LinkedHashMap<>();
            for (SerieDTO s : waitingList) {
                if (s.sequence != null) waitingCountPerSeq.merge(s.sequence, 1, Integer::sum);
            }
            Map<String, Double> doneFractionPerSeq = new LinkedHashMap<>();
            for (String seq : totalSeriesPerSeq.keySet()) {
                int total = totalSeriesPerSeq.getOrDefault(seq, 1);
                int done = doneSeriesPerSeq.getOrDefault(seq, 0);
                doneFractionPerSeq.put(seq, (double) done / total);
            }

            // SCG ordering for coupe scheduling
            final Map<String, Double> ctMap = cuttingTimeMap;
            final Set<String> ldxf = laserDxf;
            final Set<String> grb = gerber;
            waitingList.sort(Comparator
                    .comparing((SerieDTO s) -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                    .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                    .thenComparingDouble((SerieDTO s) -> -doneFractionPerSeq.getOrDefault(s.sequence, 0.0))
                    .thenComparingInt(s -> waitingCountPerSeq.getOrDefault(s.sequence, 0))
                    .thenComparing(s -> s.sequence != null ? s.sequence : "")
                    .thenComparingDouble(s -> getEstimatedCuttingTime(s, ctMap, ldxf, grb))
            );

            // PASS 1: Estimate coupe dates and assign tableCoupe
            // Store results for pass 2
            List<long[]> waitingCoupeIntervals = new ArrayList<>(); // [coupeStartMs, coupeEndMs] per waitingList index
            String[] waitingMachines = new String[waitingList.size()]; // inferred machine per index

            for (int i = 0; i < waitingList.size(); i++) {
                SerieDTO s = waitingList.get(i);
                double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                long cuttingMs = (long) (cuttingTime * 60000);

                // Infer machine: tableCoupe > tableMatelassage > sequence affinity > dispatch-aware resolver
                String machine = s.tableCoupe;
                if (machine == null && s.tableMatelassage != null) {
                    // Validate tableMatelassage compatibility before using it
                    String matType = machineTypeByName.get(s.tableMatelassage);
                    if (isCompatibleMachineType(s.machine, matType)) {
                        machine = s.tableMatelassage;
                    }
                }
                if (machine == null && s.sequence != null) {
                    // Validate sequence affinity compatibility before using it
                    String affinityMachine = seqMachineAffinity.get(s.sequence);
                    if (affinityMachine != null) {
                        String affinityType = machineTypeByName.get(affinityMachine);
                        if (isCompatibleMachineType(s.machine, affinityType)) {
                            machine = affinityMachine;
                        }
                    }
                }
                if (machine == null && !activeMachines.isEmpty()) {
                    machine = resolveMachineForSerie(
                            s, s.machine, dispatchZones.get(s.sequence),
                            zoneMachinePools, machineTypeByName, machineLoads,
                            cuttingOccupied, activeMachines, nowMs, cuttingMs, laserDxf, gerber);
                }

                // Track affinity for sibling WAITING series of the same sequence
                // Only pin affinity if the resolved machine is compatible with this serie's type
                if (machine != null && s.sequence != null) {
                    String resolvedType = machineTypeByName.get(machine);
                    if (isCompatibleMachineType(s.machine, resolvedType)) {
                        seqMachineAffinity.putIfAbsent(s.sequence, machine);
                    }
                }
                waitingMachines[i] = machine;

                if (machine != null) {
                    long coupeStartMs = findEarliestSlot(
                            cuttingOccupied.getOrDefault(machine, new ArrayList<>()), nowMs, cuttingMs);
                    long coupeEndMs = coupeStartMs + cuttingMs;
                    cuttingOccupied.computeIfAbsent(machine, k -> new ArrayList<>())
                            .add(new long[]{coupeStartMs, coupeEndMs});
                    waitingCoupeIntervals.add(new long[]{coupeStartMs, coupeEndMs});
                    // Update load for subsequent series (incremental load balancing)
                    machineLoads.merge(machine, cuttingTime, Double::sum);
                } else {
                    // Fallback: use now
                    long cuttingEndMs = nowMs + cuttingMs;
                    waitingCoupeIntervals.add(new long[]{nowMs, cuttingEndMs});
                }
            }

            // PASS 2: Estimate matelassage dates
            // Order WAITING by estimated dateDebutCoupe for chaining matelassage
            // Build index-sorted list by coupeStart
            Integer[] waitIndexes = new Integer[waitingList.size()];
            for (int i = 0; i < waitIndexes.length; i++) waitIndexes[i] = i;
            Arrays.sort(waitIndexes, Comparator.comparingLong(i -> waitingCoupeIntervals.get(i)[0]));

            for (int idx : waitIndexes) {
                SerieDTO s = waitingList.get(idx);
                long coupeStartMs = waitingCoupeIntervals.get(idx)[0];
                long coupeEndMs = waitingCoupeIntervals.get(idx)[1];
                String machine = waitingMachines[idx];

                double spreadTime = estimateSpreadingTime(s);
                long spreadMs = (long) (spreadTime * 60000);

                LocalDateTime estimatedCoupeStart = fromMsToLDT(coupeStartMs);
                LocalDateTime estimatedCoupeEnd = fromMsToLDT(coupeEndMs);
                LocalDateTime estimatedSpreadStart, estimatedSpreadEnd;

                // Determine spreading table = machine (same table)
                String spreadTable = machine;
                if (machine != null) {
                    boolean isLaserDxf = laserDxf.contains(machine);
                    if (isLaserDxf) {
                        // LASER-DXF: parallel — matelassage starts same time as coupe
                        estimatedSpreadStart = estimatedCoupeStart;
                        estimatedSpreadEnd = fromMsToLDT(coupeStartMs + spreadMs);
                    } else {
                        // Sequential: find spreading slot BEFORE coupeStart
                        // dateDebutMatelassage = last dateFinMatelassage on this table (>= now)
                        long spreadStartMs = findEarliestSlot(
                                spreadingOccupied.getOrDefault(spreadTable, new ArrayList<>()), nowMs, spreadMs);
                        long spreadEndMs = spreadStartMs + spreadMs;
                        spreadingOccupied.computeIfAbsent(spreadTable, k -> new ArrayList<>())
                                .add(new long[]{spreadStartMs, spreadEndMs});

                        estimatedSpreadStart = fromMsToLDT(spreadStartMs);
                        estimatedSpreadEnd = fromMsToLDT(spreadEndMs);
                    }
                } else {
                    estimatedSpreadStart = now;
                    estimatedSpreadEnd = now.plusMinutes((long) spreadTime);
                }

                Map<String, Object> block = buildBlock(s, "WAITING", estimatedSpreadStart, estimatedSpreadEnd, cuttingTimeMap, laserDxf, gerber);
                block.put("estimatedSpreadStart", estimatedSpreadStart.toString());
                block.put("estimatedSpreadEnd", estimatedSpreadEnd.toString());
                block.put("estimatedCoupeStart", estimatedCoupeStart.toString());
                block.put("estimatedCoupeEnd", estimatedCoupeEnd.toString());
                block.put("estimatedDebutMatelassage", estimatedSpreadStart.toString());
                block.put("estimatedFinMatelassage", estimatedSpreadEnd.toString());
                block.put("estimatedDebutCoupe", estimatedCoupeStart.toString());
                block.put("estimatedFinCoupe", estimatedCoupeEnd.toString());
                if (machine != null) {
                    block.put("assignedMachine", machine);
                    block.put("tableCoupe", machine);
                    block.put("tableMatelassage", machine);
                }
                blocks.add(block);
            }
        }

        return blocks;
    }

    /**
     * Build a single timeline block map from a SerieDTO.
     */
    private Map<String, Object> buildBlock(SerieDTO s, String status,
                                           LocalDateTime start, LocalDateTime end,
                                           Map<String, Double> cuttingTimeMap,
                                           Set<String> laserDxf,
                                           Set<String> gerber) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("serie", s.serie);
        block.put("sequence", s.sequence);
        block.put("partNumberMaterial", s.partNumberMaterial);
        block.put("description", s.description);
        block.put("longueur", s.longueur);
        block.put("nbrCouche", s.nbrCouche);
        block.put("tempsDeCoupe", s.tempsDeCoupe);
        block.put("estimatedCuttingTime", getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber));
        block.put("estimatedSpreadingTime", estimateSpreadingTime(s));
        block.put("status", status);
        block.put("tableCoupe", s.tableCoupe);
        block.put("tableMatelassage", s.tableMatelassage);
        block.put("zoneCoupe", s.zoneCoupe);
        block.put("zoneMatelassage", s.zoneMatelassage);
        block.put("placement", s.placement);
        block.put("statusCoupe", s.statusCoupe);
        block.put("statusMatelassage", s.statusMatelassage);
        block.put("dateDebutCoupe", s.dateDebutCoupe != null ? s.dateDebutCoupe.toString() : null);
        block.put("dateFinCoupe", s.dateFinCoupe != null ? s.dateFinCoupe.toString() : null);
        block.put("dateDebutMatelassage", s.dateDebutMatelassage != null ? s.dateDebutMatelassage.toString() : null);
        block.put("dateFinMatelassage", s.dateFinMatelassage != null ? s.dateFinMatelassage.toString() : null);
        block.put("blockStart", start != null ? start.toString() : null);
        block.put("blockEnd", end != null ? end.toString() : null);
        return block;
    }

    // ======================== UNASSIGNED SERIES ========================

    /**
     * Build unassigned series list from pre-loaded notYet data.
     * Uses 1 lightweight batch query for sequence zone/dueDate/dueShift.
     */
    private List<Map<String, Object>> buildUnassignedSeries(
            List<SerieDTO> notYet,
            Map<String, Double> cuttingTimeMap,
            Set<String> laserDxf,
            Set<String> gerber) {

        List<Map<String, Object>> result = new ArrayList<>();

        // Collect sequences for batch lookup
        Set<String> sequenceIds = new LinkedHashSet<>();
        for (SerieDTO s : notYet) {
            if ((s.tableCoupe == null || s.tableCoupe.isEmpty()) && s.sequence != null) {
                sequenceIds.add(s.sequence);
            }
        }

        // Batch load: [sequence, zoneName, dueDate, dueShift] — batched to respect 2100 param limit
        Map<String, Object[]> seqInfoMap = new HashMap<>();
        if (!sequenceIds.isEmpty()) {
            List<String> seqList = new ArrayList<>(sequenceIds);
            for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
                List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
                for (Object[] row : cuttingRequestDataRepository.findSequenceInfoLight(batch)) {
                    seqInfoMap.put((String) row[0], row);
                }
            }
        }

        for (SerieDTO s : notYet) {
            if (s.tableCoupe == null || s.tableCoupe.isEmpty()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("serie", s.serie);
                m.put("sequence", s.sequence);
                m.put("partNumberMaterial", s.partNumberMaterial);
                m.put("description", s.description);
                m.put("longueur", s.longueur);
                m.put("nbrCouche", s.nbrCouche);
                m.put("tempsDeCoupe", s.tempsDeCoupe);
                m.put("estimatedCuttingTime", getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber));
                m.put("estimatedSpreadingTime", estimateSpreadingTime(s));
                m.put("statusMatelassage", s.statusMatelassage);
                m.put("statusCoupe", s.statusCoupe);
                m.put("tableMatelassage", s.tableMatelassage);
                m.put("zoneMatelassage", s.zoneMatelassage);
                m.put("placement", s.placement);
                m.put("dateDebutMatelassage", s.dateDebutMatelassage != null ? s.dateDebutMatelassage.toString() : null);
                m.put("dateFinMatelassage", s.dateFinMatelassage != null ? s.dateFinMatelassage.toString() : null);

                // Sequence zone, dueDate, dueShift from light query
                Object[] seqInfo = s.sequence != null ? seqInfoMap.get(s.sequence) : null;
                if (seqInfo != null && seqInfo[1] != null) {
                    m.put("sequenceZone", (String) seqInfo[1]);
                } else {
                    m.put("sequenceZone", s.zoneMatelassage);
                }
                if (seqInfo != null) {
                    m.put("dueDate", seqInfo[2] != null ? seqInfo[2].toString() : null);
                    m.put("dueShift", seqInfo[3]);
                }

                result.add(m);
            }
        }

        // Sort by dispatch priority (Complete > In progress > Waiting), then by dueDate, dueShift, dateDebutMatelassage
        result.sort(Comparator
                .comparingInt((Map<String, Object> m) -> getSerieDispatchPriority((String) m.getOrDefault("statusMatelassage", "Waiting")))
                .thenComparing(m -> {
                    Object d = m.get("dueDate");
                    return d != null ? d.toString() : "9999-12-31";
                })
                .thenComparingInt(m -> {
                    Object s = m.get("dueShift");
                    return s != null ? Integer.parseInt(s.toString()) : Integer.MAX_VALUE;
                })
                .thenComparing(m -> {
                    Object d = m.get("dateDebutMatelassage");
                    return d != null ? d.toString() : "9999-12-31T23:59:59";
                })
                .thenComparing(m -> (String) m.getOrDefault("sequence", ""))
                .thenComparing(m -> (String) m.getOrDefault("serie", "")));

        return result;
    }

    // ======================== AUTO-DISPATCH (Collision-Free Scheduling) ========================

    /**
     * Generate automatic dispatching with realistic, collision-free date estimation.
     *
     * Algorithm:
     * 1. For each unassigned (waiting) serie, estimate:
     *    dateDebutMatelassage -> dateFinMatelassage -> dateDebutCoupe -> dateFinCoupe
     * 2. No spreading interval on the same table should overlap with another spreading
     * 3. No cutting interval on the same machine should overlap with another cutting
     * 4. All estimated dates >= now
     * 5. Goal: optimal sequence duration (series of same sequence cut back-to-back)
     *
     * Returns the dispatched series with their estimated times (not saved to DB).
     */
    public Map<String, Object> autoDispatch() {
        return autoDispatch(null);
    }

    /**
     * Auto-dispatch with a specified algorithm.
     * @param algorithm one of: SCG, SPT, LPT, EDF, CR, WSPT, ATC, MATERIAL_GROUP (null = load from config or default SCG)
     */
    public Map<String, Object> autoDispatch(String algorithm) {
        // Resolve algorithm from saved config if not specified
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = schedulingConfigRepository.findByZoneCodeIsNull()
                    .map(c -> c.getAlgorithm())
                    .orElse("SCG");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Smart load: relevant sequences only
        List<SerieDTO> allSeries = loadRelevantSeries(Collections.emptyList());
        Map<String, List<SerieDTO>> split = splitSeries(allSeries, now.minusHours(12), now.plusHours(12));
        List<SerieDTO> inProgress = split.get("inProgress");
        List<SerieDTO> notYet = split.get("notYet");
        Map<String, Double> cuttingTimeMap = buildCuttingTimeMap(inProgress, notYet);
        Set<String> laserDxf = loadLaserDxfMachines();
        Set<String> gerber = loadGerberMachines();
        List<Object[]> machinesRaw = productionTableRepository.findAllMachinesLight();
        Map<String, String> machineStatuses = etatMachineHistoriqueService.getAllCurrentStatusCodes(now);

        // Build machine availability maps
        // Key = machine name, Value = list of occupied intervals [start, end]
        Map<String, List<long[]>> cuttingOccupied = new LinkedHashMap<>();
        Map<String, List<long[]>> spreadingOccupied = new LinkedHashMap<>();
        Map<String, Double> machineTableLengths = new LinkedHashMap<>();
        Map<String, String> machineZones = new LinkedHashMap<>();

        for (Object[] row : machinesRaw) {
            String nom = (String) row[1];
            String zone = (String) row[2];
            double tableLen = row[4] != null ? ((Number) row[4]).doubleValue() : TABLE_LENGTH_DEFAULT;
            machineTableLengths.put(nom, tableLen);
            machineZones.put(nom, zone);
            cuttingOccupied.put(nom, new ArrayList<>());
            spreadingOccupied.put(nom, new ArrayList<>());
        }

        long nowMs = now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        // Register already occupied intervals from in-progress and completed series
        for (SerieDTO s : allSeries) {
            // Cutting occupation
            if (s.dateDebutCoupe != null && s.tableCoupe != null) {
                long startMs = toMs(s.dateDebutCoupe);
                long endMs;
                if (s.dateFinCoupe != null) {
                    endMs = toMs(s.dateFinCoupe);
                } else {
                    double ct = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                    endMs = startMs + (long)(ct * 60000);
                    if (endMs < nowMs) endMs = nowMs + 300000; // still in progress
                }
                List<long[]> slots = cuttingOccupied.get(s.tableCoupe);
                if (slots != null) slots.add(new long[]{startMs, endMs});
            }
            // Spreading occupation
            if (s.dateDebutMatelassage != null && s.tableMatelassage != null) {
                long startMs = toMs(s.dateDebutMatelassage);
                long endMs;
                if (s.dateFinMatelassage != null) {
                    endMs = toMs(s.dateFinMatelassage);
                } else {
                    double st = estimateSpreadingTime(s);
                    endMs = startMs + (long)(st * 60000);
                    if (endMs < nowMs) endMs = nowMs + 300000;
                }
                List<long[]> slots = spreadingOccupied.get(s.tableMatelassage);
                if (slots != null) slots.add(new long[]{startMs, endMs});
            }
        }

        // Get active machines
        Set<String> activeMachines = new HashSet<>();
        for (Object[] row : machinesRaw) {
            String nom = (String) row[1];
            String status = machineStatuses.getOrDefault(nom, "M");
            if ("M".equals(status)) activeMachines.add(nom);
        }

        // ---- ENRICH dueDate / dueShift from CuttingRequestData ----
        // SerieDTO.from() does NOT populate dueDate/dueShift; they must be
        // enriched from the parent CuttingRequestData entity. Without this,
        // dispatch algorithms (SCG, EDF, CR, ATC) see null due dates and
        // cannot prioritize urgent sequences.
        Set<String> allSequences = new HashSet<>();
        for (SerieDTO s : allSeries) allSequences.add(s.sequence);

        Map<String, Map<String, Object>> sequenceInfoMap = new HashMap<>();
        List<String> seqList = new ArrayList<>(allSequences);
        for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
            for (Object[] row : cuttingRequestDataRepository.findSequenceInfoLight(batch)) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("sequence", row[0]);
                info.put("zone", row[1]);
                info.put("dueDate", row[2] != null ? row[2].toString() : null);
                info.put("dueShift", row[3]);
                sequenceInfoMap.put((String) row[0], info);
            }
        }
        for (SerieDTO s : allSeries) {
            Map<String, Object> info = sequenceInfoMap.get(s.sequence);
            if (info != null) {
                if (info.get("dueDate") != null) {
                    s.dueDate = LocalDate.parse((String) info.get("dueDate"));
                }
                if (info.get("dueShift") != null) {
                    s.dueShift = Integer.parseInt(info.get("dueShift").toString());
                }
            }
        }

        // ---- DISPATCH-AWARE MACHINE SELECTION DATA ----
        Map<String, String> dispatchZones = loadDispatchZoneAssignments(allSequences);
        Map<String, Map<String, List<String>>> zoneMachinePools = buildZoneMachinePools(machinesRaw);
        Map<String, String> machineTypeByName = buildMachineTypeByName(machinesRaw);
        Map<String, Double> machineLoads = buildMachineLoads(
                allSeries.stream().filter(s -> s.dateFinCoupe != null).collect(Collectors.toList()),
                allSeries.stream().filter(s -> s.dateFinCoupe == null && s.dateDebutCoupe != null).collect(Collectors.toList()),
                cuttingTimeMap, laserDxf, gerber);

        // Track sequences that have already started (some series in progress/spreading)
        Set<String> startedSequences = new HashSet<>();
        for (SerieDTO s : inProgress) {
            if (s.sequence != null) startedSequences.add(s.sequence);
        }
        for (SerieDTO s : notYet) {
            if (s.sequence != null && !"Waiting".equals(s.statusMatelassage)) {
                startedSequences.add(s.sequence);
            }
        }

        // Get unassigned series (Waiting on both statuses, no tableCoupe)
        // Exclude series with Matelassage Incomplete (not enough material)
        List<SerieDTO> toDispatch = new ArrayList<>();
        for (SerieDTO s : notYet) {
            if ("Incomplete".equals(s.statusMatelassage)) continue; // Skip: not enough material
            if ("Waiting".equals(s.statusMatelassage) && "Waiting".equals(s.statusCoupe)
                    && (s.tableCoupe == null || s.tableCoupe.isEmpty())) {
                toDispatch.add(s);
            }
        }

        // Also include series that have started spreading but no cutting table
        // (but not Incomplete ones — they should never be dispatched to cutting)
        for (SerieDTO s : notYet) {
            if ("Incomplete".equals(s.statusMatelassage)) continue;
            if (!"Waiting".equals(s.statusMatelassage) && (s.tableCoupe == null || s.tableCoupe.isEmpty())) {
                if (!toDispatch.contains(s)) toDispatch.add(s);
            }
        }

        // Sort using the selected dispatch algorithm
        DispatchAlgorithms.DispatchContext dispatchCtx = new DispatchAlgorithms.DispatchContext();
        dispatchCtx.startedSequences = startedSequences;
        dispatchCtx.cuttingTimeMap = cuttingTimeMap;
        dispatchCtx.coefSpreadingPerMetre = COEF_SPREADING_PER_METRE;
        dispatchCtx.coefSetupTime = COEF_SETUP_TIME;
        dispatchCtx.now = now;
        // Compute average processing time for ATC algorithm normalization
        if (!toDispatch.isEmpty()) {
            double totalPTime = 0;
            for (SerieDTO s : toDispatch) {
                double spread = estimateSpreadingTime(s);
                double cut = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                totalPTime += spread + cut;
            }
            dispatchCtx.averageProcessingTime = totalPTime / toDispatch.size();
        }
        toDispatch.sort(DispatchAlgorithms.get(algorithm, dispatchCtx));

        List<Map<String, Object>> dispatched = new ArrayList<>();
        // Track dispatched machines per sequence for stronger affinity
        Map<String, String> dispatchedMachineBySequence = new HashMap<>();

        for (SerieDTO s : toDispatch) {
            double spreadTime = estimateSpreadingTime(s);
            double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
            long spreadMs = (long)(spreadTime * 60000);
            long cuttingMs = (long)(cuttingTime * 60000);

            // Find the best machine for cutting
            String bestMachine = null;
            long bestCoupeStart = Long.MAX_VALUE;
            long bestSpreadStart = Long.MAX_VALUE;
            String bestSpreadTable = null;

            // Determine candidate machines:
            // 1. If serie already has tableMatelassage and is NOT Waiting -> prefer tableMatelassage
            //    ONLY if the table's machine type is compatible with the serie's required type.
            // 2. If serie is Waiting -> search all active machines
            Set<String> candidateMachines;
            boolean matelassageCompatible = false;
            if (s.tableMatelassage != null && !s.tableMatelassage.isEmpty()
                    && !"Waiting".equals(s.statusMatelassage)) {
                String matType = machineTypeByName.get(s.tableMatelassage);
                matelassageCompatible = isCompatibleMachineType(s.machine, matType);
            }
            if (matelassageCompatible) {
                // Serie has started/finished spreading on a compatible table -> prefer that table for cutting
                candidateMachines = new HashSet<>();
                if (activeMachines.contains(s.tableMatelassage)) {
                    candidateMachines.add(s.tableMatelassage);
                } else {
                    // Machine not active, fall back to all active machines
                    candidateMachines = activeMachines;
                }
            } else {
                // Dispatch-aware filtering: same-type (or compatible) machines in dispatched zone first
                candidateMachines = buildCandidateMachinePool(
                        s, s.machine, dispatchZones.get(s.sequence),
                        zoneMachinePools, activeMachines, laserDxf, gerber);
            }

            String preferredZone = s.zoneMatelassage;
            if (preferredZone == null) {
                preferredZone = machineZones.values().stream().findFirst().orElse(null);
            }

            for (String machineNom : candidateMachines) {
                // Find earliest available cutting slot on this machine
                long earliestCoupeStart = findEarliestSlot(cuttingOccupied.get(machineNom), nowMs, cuttingMs);

                // LASER-DXF: spreading happens in parallel with cutting (same machine)
                // So we only need the cutting slot; matelassage starts same time
                boolean isLaserDxf = laserDxf.contains(machineNom);

                // We need spreading to finish BEFORE cutting starts (unless LASER-DXF)
                long neededSpreadEnd = earliestCoupeStart;
                long spreadStart;
                String spreadTable = machineNom; // default: spread on same table

                if (s.dateDebutMatelassage != null) {
                    // Already started spreading
                    if (s.dateFinMatelassage != null) {
                        // Spreading done, just schedule cutting
                        spreadStart = toMs(s.dateDebutMatelassage);
                        neededSpreadEnd = toMs(s.dateFinMatelassage);
                    } else {
                        spreadStart = toMs(s.dateDebutMatelassage);
                        neededSpreadEnd = spreadStart + spreadMs;
                        if (neededSpreadEnd < nowMs) neededSpreadEnd = nowMs + 120000;
                    }
                    spreadTable = s.tableMatelassage;
                } else {
                    // Need to schedule spreading too
                    // Find the earliest spreading slot on this machine
                    spreadStart = findEarliestSlot(
                            spreadingOccupied.getOrDefault(machineNom, new ArrayList<>()), nowMs, spreadMs);
                    neededSpreadEnd = spreadStart + spreadMs;
                    spreadTable = machineNom;
                }

                // Cutting must start after spreading ends (unless LASER-DXF: parallel)
                long actualCoupeStart;
                if (isLaserDxf) {
                    // LASER-DXF: cutting and spreading happen simultaneously
                    // Only look at cutting slot availability
                    actualCoupeStart = earliestCoupeStart;
                    spreadStart = actualCoupeStart; // spreading starts same time as cutting
                    spreadTable = machineNom;
                } else {
                    actualCoupeStart = Math.max(earliestCoupeStart, neededSpreadEnd);
                }
                // Re-check if this slot is still free for cutting
                actualCoupeStart = findEarliestSlot(cuttingOccupied.get(machineNom), actualCoupeStart, cuttingMs);

                // Sequence affinity bonus: prefer machines already working on same sequence
                boolean hasAffinity = false;
                if (s.sequence != null) {
                    // Check in-progress series on this machine
                    for (SerieDTO other : inProgress) {
                        if (machineNom.equals(other.tableCoupe) && s.sequence.equals(other.sequence)) {
                            hasAffinity = true;
                            break;
                        }
                    }
                    // Also check if we already dispatched another serie of same sequence to this machine
                    if (!hasAffinity) {
                        String prevDispatched = dispatchedMachineBySequence.get(s.sequence);
                        if (machineNom.equals(prevDispatched)) {
                            hasAffinity = true;
                        }
                    }
                }

                long effectiveStart = hasAffinity ? actualCoupeStart - 60000 : actualCoupeStart; // slight bonus

                if (effectiveStart < bestCoupeStart) {
                    bestCoupeStart = actualCoupeStart;
                    bestMachine = machineNom;
                    bestSpreadStart = spreadStart;
                    bestSpreadTable = spreadTable;
                }
            }

            if (bestMachine != null) {
                long coupeEnd = bestCoupeStart + cuttingMs;

                // Register occupation
                cuttingOccupied.get(bestMachine).add(new long[]{bestCoupeStart, coupeEnd});
                if (s.dateDebutMatelassage == null && bestSpreadTable != null) {
                    List<long[]> spreadSlots = spreadingOccupied.get(bestSpreadTable);
                    if (spreadSlots != null) {
                        spreadSlots.add(new long[]{bestSpreadStart, bestSpreadStart + spreadMs});
                    }
                }
                // Update load for subsequent series (incremental load balancing)
                machineLoads.merge(bestMachine, cuttingTime, Double::sum);

                Map<String, Object> d = new LinkedHashMap<>();
                d.put("serie", s.serie);
                d.put("sequence", s.sequence);
                d.put("partNumberMaterial", s.partNumberMaterial);
                d.put("longueur", s.longueur);
                d.put("nbrCouche", s.nbrCouche);
                d.put("estimatedCuttingTime", cuttingTime);
                d.put("estimatedSpreadingTime", spreadTime);
                d.put("recommendedMachine", bestMachine);
                d.put("recommendedZone", machineZones.get(bestMachine));
                d.put("spreadTable", bestSpreadTable);

                // All dates as ISO strings
                if (s.dateDebutMatelassage == null) {
                    if (laserDxf.contains(bestMachine)) {
                        // LASER-DXF: matelassage in parallel with coupe
                        d.put("estimatedDebutMatelassage", fromMs(bestCoupeStart));
                        d.put("estimatedFinMatelassage", fromMs(bestCoupeStart + cuttingMs));
                    } else {
                        d.put("estimatedDebutMatelassage", fromMs(bestSpreadStart));
                        d.put("estimatedFinMatelassage", fromMs(bestSpreadStart + spreadMs));
                    }
                } else {
                    d.put("dateDebutMatelassage", s.dateDebutMatelassage.toString());
                    if (s.dateFinMatelassage != null) {
                        d.put("dateFinMatelassage", s.dateFinMatelassage.toString());
                    } else {
                        d.put("estimatedFinMatelassage", fromMs(bestSpreadStart + spreadMs));
                    }
                }
                d.put("estimatedDebutCoupe", fromMs(bestCoupeStart));
                d.put("estimatedFinCoupe", fromMs(coupeEnd));

                // Track dispatched machine for sequence affinity
                if (s.sequence != null) {
                    dispatchedMachineBySequence.put(s.sequence, bestMachine);
                }

                dispatched.add(d);
            }
        }

        result.put("dispatched", dispatched);
        result.put("totalDispatched", dispatched.size());
        result.put("totalUnassigned", Math.max(0, toDispatch.size() - dispatched.size()));
        result.put("algorithm", algorithm);
        result.put("generatedAt", now.toString());

        // Compute basic score metrics
        result.put("score", computeDispatchScore(dispatched, now));

        return result;
    }

    /**
     * Compute a simple dispatch quality score from the list of dispatched assignments.
     * Returns a map with score breakdown.
     */
    private Map<String, Object> computeDispatchScore(List<Map<String, Object>> dispatched, LocalDateTime now) {
        Map<String, Object> score = new LinkedHashMap<>();
        if (dispatched.isEmpty()) {
            score.put("total", 0);
            return score;
        }

        // 1. Count material changeovers per machine
        Map<String, List<String>> materialsByMachine = new LinkedHashMap<>();
        for (Map<String, Object> d : dispatched) {
            String machine = (String) d.get("recommendedMachine");
            String material = (String) d.get("partNumberMaterial");
            materialsByMachine.computeIfAbsent(machine, k -> new ArrayList<>()).add(material);
        }
        int totalChangeovers = 0;
        for (List<String> materials : materialsByMachine.values()) {
            for (int i = 1; i < materials.size(); i++) {
                if (!Objects.equals(materials.get(i), materials.get(i - 1))) {
                    totalChangeovers++;
                }
            }
        }

        // 2. Count unique sequences fully dispatched (all series)
        Map<String, Long> seriesPerSequence = dispatched.stream()
                .filter(d -> d.get("sequence") != null)
                .collect(Collectors.groupingBy(d -> (String) d.get("sequence"), Collectors.counting()));

        // 3. Estimate max span per sequence (box completion time proxy)
        // Using ISO string dates from the dispatched assignments
        double maxBoxTimeMinutes = 0;
        double totalBoxTimeMinutes = 0;
        int sequenceCount = 0;
        Map<String, long[]> sequenceSpan = new LinkedHashMap<>();
        for (Map<String, Object> d : dispatched) {
            String seq = (String) d.get("sequence");
            if (seq == null) continue;
            String coupeEnd = (String) d.get("estimatedFinCoupe");
            String matStart = (String) d.get("estimatedDebutMatelassage");
            if (matStart == null) matStart = (String) d.get("dateDebutMatelassage");
            if (coupeEnd == null || matStart == null) continue;
            try {
                long startMs = toMs(LocalDateTime.parse(matStart));
                long endMs = toMs(LocalDateTime.parse(coupeEnd));
                long[] span = sequenceSpan.get(seq);
                if (span == null) {
                    sequenceSpan.put(seq, new long[]{startMs, endMs});
                } else {
                    span[0] = Math.min(span[0], startMs);
                    span[1] = Math.max(span[1], endMs);
                }
            } catch (Exception ignored) {}
        }
        for (long[] span : sequenceSpan.values()) {
            double minutes = (span[1] - span[0]) / 60000.0;
            totalBoxTimeMinutes += minutes;
            maxBoxTimeMinutes = Math.max(maxBoxTimeMinutes, minutes);
            sequenceCount++;
        }

        score.put("totalChangeovers", totalChangeovers);
        score.put("avgChangeoversPerMachine", materialsByMachine.isEmpty() ? 0 :
                Math.round(totalChangeovers * 10.0 / materialsByMachine.size()) / 10.0);
        score.put("maxBoxCompletionMinutes", Math.round(maxBoxTimeMinutes));
        score.put("avgBoxCompletionMinutes", sequenceCount > 0 ?
                Math.round(totalBoxTimeMinutes / sequenceCount) : 0);
        score.put("machineCount", materialsByMachine.size());
        score.put("sequenceCount", sequenceCount);

        // Composite score (0-1000): lower changeovers + lower box time = higher score
        double changeoverPenalty = Math.min(totalChangeovers * 10, 300);
        double boxTimePenalty = Math.min(maxBoxTimeMinutes / 2, 400);
        int total = (int) Math.max(0, 1000 - changeoverPenalty - boxTimePenalty);
        score.put("total", total);

        return score;
    }

    /**
     * Find the earliest time slot where a task of the given duration can fit without colliding
     * with existing occupied intervals.
     */
    private long findEarliestSlot(List<long[]> occupied, long earliest, long durationMs) {
        if (occupied == null || occupied.isEmpty()) return earliest;

        // Sort intervals by start time
        List<long[]> sorted = new ArrayList<>(occupied);
        sorted.sort(Comparator.comparingLong(a -> a[0]));

        long candidate = earliest;
        for (long[] interval : sorted) {
            if (candidate + durationMs <= interval[0]) {
                // Fits before this interval
                return candidate;
            }
            if (candidate < interval[1]) {
                // Overlaps, push candidate after this interval
                candidate = interval[1];
            }
        }
        return candidate;
    }

    private long toMs(LocalDateTime dt) {
        return dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String fromMs(long ms) {
        return java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().toString();
    }

    private LocalDateTime fromMsToLDT(long ms) {
        return java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Priority for dispatching: Complete first (ready to cut), then In progress (almost ready),
     * then Waiting (not started). Always ordered by dateDebutMatelassage within same priority.
     */
    private int getSerieDispatchPriority(String statusMatelassage) {
        if ("Complete".equals(statusMatelassage)) return 0;
        if ("In progress".equals(statusMatelassage)) return 1;
        return 2;  // Waiting — all dates null
    }

    // ======================== MANUAL ASSIGNMENT ========================

    /**
     * Manually assign a serie to a specific machine for cutting.
     * Uses full entity (write operation, one-off call).
     */
    @Transactional
    public Map<String, Object> assignSerieToMachine(String serieId, String machineNom) {
        Map<String, Object> result = new LinkedHashMap<>();

        CuttingRequestSerieData serie = serieDataRepository.findBySerie(serieId);
        if (serie == null) {
            result.put("success", false);
            result.put("error", "Serie non trouvee: " + serieId);
            return result;
        }

        Optional<ProductionTable> machineOpt = productionTableRepository.findByNom(machineNom);
        if (!machineOpt.isPresent()) {
            result.put("success", false);
            result.put("error", "Machine non trouvee: " + machineNom);
            return result;
        }
        ProductionTable machine = machineOpt.get();

        // Check table space using lightweight queries
        double occupiedLength = calculateOccupiedLengthStandalone(machineNom);
        double tableLen = machine.getTableLength() != null ? machine.getTableLength() : TABLE_LENGTH_DEFAULT;
        double available = tableLen - occupiedLength;
        double longueur = serie.getLongueur() != null ? serie.getLongueur() : 0;

        if (longueur > 0 && longueur > available) {
            result.put("success", false);
            result.put("error", String.format("Espace insuffisant sur %s: %.1fm disponible, %.1fm necessaire",
                    machineNom, available, longueur));
            return result;
        }

        serie.setTableCoupe(machineNom);
        if (machine.getZone() != null) {
            serie.setZoneCoupe(machine.getZone().getNom());
        }
        serieDataRepository.save(serie);

        result.put("success", true);
        result.put("serie", serieId);
        result.put("machine", machineNom);
        result.put("availableLength", available - longueur);

        return result;
    }

    /**
     * Move all series of a sequence to a different zone.
     */
    @Transactional
    public Map<String, Object> assignSequenceToZone(String sequenceId, String zoneName) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<String> statusList = serieDataRepository.getStatusCoupeBySequence(sequenceId);
        if (statusList.isEmpty()) {
            result.put("success", false);
            result.put("error", "Sequence non trouvee: " + sequenceId);
            return result;
        }

        List<ProductionTable> zoneMachines = productionTableRepository.findByZone(zoneName);
        if (zoneMachines.isEmpty()) {
            result.put("success", false);
            result.put("error", "Aucune machine dans la zone: " + zoneName);
            return result;
        }

        result.put("success", true);
        result.put("sequence", sequenceId);
        result.put("targetZone", zoneName);
        result.put("machinesInZone", zoneMachines.stream().map(ProductionTable::getNom).collect(Collectors.toList()));

        return result;
    }

    // ======================== MACHINE ZONE CHANGE ========================

    /**
     * Change a machine's zone assignment.
     */
    @Transactional
    public Map<String, Object> changeMachineZone(String machineNom, String targetZoneName) {
        Map<String, Object> result = new LinkedHashMap<>();

        Optional<ProductionTable> machineOpt = productionTableRepository.findByNom(machineNom);
        if (!machineOpt.isPresent()) {
            result.put("success", false);
            result.put("error", "Machine non trouvée: " + machineNom);
            return result;
        }

        Zone zone = zoneRepository.findByObjId(targetZoneName);
        if (zone == null) {
            result.put("success", false);
            result.put("error", "Zone non trouvée: " + targetZoneName);
            return result;
        }

        ProductionTable machine = machineOpt.get();
        String oldZone = machine.getZone() != null ? machine.getZone().getNom() : null;
        machine.setZone(zone);
        productionTableRepository.save(machine);

        result.put("success", true);
        result.put("machine", machineNom);
        result.put("oldZone", oldZone);
        result.put("newZone", targetZoneName);
        return result;
    }

    /**
     * Get all available zones (for dropdown).
     */
    public List<String> getAllZoneNames() {
        List<String> names = new ArrayList<>();
        zoneRepository.findAll().forEach(z -> names.add(z.getNom()));
        Collections.sort(names);
        return names;
    }

    // ======================== MACHINE QUEUE (SAVE NEXT 3) ========================

    /**
     * Save the next 3 series for each machine to the MachineQueue entity.
     * Loads series data ONCE, then iterates machines in-memory.
     */
    @Transactional
    public Map<String, Object> saveQueues(String username) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Object[]> machinesRaw = productionTableRepository.findAllMachinesLight();
        Map<String, Long> previousVersions = new HashMap<>();
        for (MachineQueue existing : machineQueueRepository.findAllOrdered()) {
            if (existing.getMachineNom() == null) continue;
            long version = existing.getVersion() == null ? 0L : existing.getVersion();
            previousVersions.merge(existing.getMachineNom(), version, Math::max);
        }

        machineQueueRepository.deleteAllQueues();

        // Smart load: relevant sequences only
        List<SerieDTO> allSeries = loadRelevantSeries(Collections.emptyList());
        Map<String, List<SerieDTO>> split = splitSeries(allSeries, LocalDateTime.now().minusHours(12), LocalDateTime.now().plusHours(12));
        List<SerieDTO> inProgress = split.get("inProgress");
        List<SerieDTO> notYet = split.get("notYet");
        Map<String, Double> cuttingTimeMap = buildCuttingTimeMap(inProgress, notYet);
        Set<String> laserDxf = loadLaserDxfMachines();
        Set<String> gerber = loadGerberMachines();

        int totalSaved = 0;

        for (Object[] mRow : machinesRaw) {
            String machineNom = (String) mRow[1];
            long nextVersion = previousVersions.getOrDefault(machineNom, 0L) + 1L;

            List<SerieDTO> queue = getNextSeriesForMachine(machineNom, 3, inProgress, notYet, cuttingTimeMap, laserDxf, gerber);

            LocalDateTime estimatedStart = LocalDateTime.now();
            int position = 1;

            for (SerieDTO serie : queue) {
                double cuttingTime = getEstimatedCuttingTime(serie, cuttingTimeMap, laserDxf, gerber);

                MachineQueue mq = new MachineQueue();
                mq.setMachineNom(machineNom);
                mq.setQueuePosition(position);
                mq.setSerie(serie.serie);
                mq.setSequenceId(serie.sequence);
                mq.setPartNumberMaterial(serie.partNumberMaterial);
                mq.setLongueur(serie.longueur);
                mq.setEstimatedCuttingTime(cuttingTime);
                mq.setEstimatedStartTime(estimatedStart);
                mq.setEstimatedEndTime(estimatedStart.plusMinutes((long) cuttingTime));
                mq.setAssignedBy(username);
                mq.setVersion(nextVersion);

                machineQueueRepository.save(mq);
                totalSaved++;

                estimatedStart = estimatedStart.plusMinutes((long) cuttingTime);
                position++;
            }
        }

        result.put("success", true);
        result.put("totalSaved", totalSaved);
        result.put("machineCount", getAllQueues().size());
        result.put("savedAt", LocalDateTime.now().toString());
        result.put("savedBy", username);

        return result;
    }

    /**
     * Get next N series for a machine from pre-loaded data. No database queries.
     */
    private List<SerieDTO> getNextSeriesForMachine(String machineNom, int limit,
                                                   List<SerieDTO> inProgress,
                                                   List<SerieDTO> notYet,
                                                   Map<String, Double> cuttingTimeMap,
                                                   Set<String> laserDxf,
                                                   Set<String> gerber) {
        List<SerieDTO> result = new ArrayList<>();

        // 1. In-progress cutting on this machine
        for (SerieDTO s : inProgress) {
            if (machineNom.equals(s.tableCoupe)) {
                result.add(s);
                if (result.size() >= limit) return result;
            }
        }

        // 2. Ready to cut (spreading complete, assigned to this machine)
        List<SerieDTO> readyToCut = new ArrayList<>();
        List<SerieDTO> waiting = new ArrayList<>();

        for (SerieDTO s : notYet) {
            if (machineNom.equals(s.tableCoupe)) {
                if ("Complete".equals(s.statusMatelassage)) {
                    readyToCut.add(s);
                } else {
                    waiting.add(s);
                }
            }
        }

        SeriesOrderingStrategy.Context ctx = new SeriesOrderingStrategy.Context();
        ctx.machineNom = machineNom;
        ctx.cuttingTimeMap = cuttingTimeMap;
        ctx.laserDxfMachines = laserDxf;
        ctx.gerberMachines = gerber;

        seriesOrderingStrategy.sortReadyToCut(readyToCut, ctx);

        for (SerieDTO s : readyToCut) {
            result.add(s);
            if (result.size() >= limit) return result;
        }

        // 3. Waiting series
        seriesOrderingStrategy.sortWaiting(waiting, ctx);

        for (SerieDTO s : waiting) {
            result.add(s);
            if (result.size() >= limit) return result;
        }

        return result;
    }

    public List<MachineQueue> getQueueForMachine(String machineNom) {
        return machineQueueRepository.findByMachineNomOrderByQueuePosition(machineNom);
    }

    public Map<String, List<MachineQueue>> getAllQueues() {
        List<MachineQueue> allQueues = machineQueueRepository.findAllOrdered();
        return allQueues.stream().collect(Collectors.groupingBy(
                MachineQueue::getMachineNom,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    // ======================== SHIFT OVERFLOW ========================

    /**
     * Calculate if all due series (current shift + oldest shifts) can finish before the shift ends.
     * If not, returns how many minutes overflow into the next shift, and what percentage
     * of the next shift's plan de charge that overflow represents.
     */
    private Map<String, Object> calculateShiftOverflow(List<SerieDTO> inProgress, List<SerieDTO> notYet,
                                                        Map<String, Double> cuttingTimeMap,
                                                        Set<String> laserDxf, Set<String> gerber,
                                                        LocalDateTime now,
                                                        long activeMachineCount) {
        Map<String, Object> overflow = new LinkedHashMap<>();

        String[] currentShift = getCurrentShiftInfo(now);
        LocalDateTime shiftEnd = getShiftEndTime(currentShift[0], currentShift[1]);
        long minutesUntilShiftEnd = Duration.between(now, shiftEnd).toMinutes();
        if (minutesUntilShiftEnd < 0) minutesUntilShiftEnd = 0;

        // Total remaining cutting time for all in-progress series
        double totalRemainingCuttingMinutes = 0;

        // In-progress: calculate remaining time
        for (SerieDTO s : inProgress) {
            double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
            if (s.dateDebutCoupe != null && cuttingTime > 0) {
                long elapsedMinutes = Duration.between(s.dateDebutCoupe, now).toMinutes();
                double remaining = Math.max(0, cuttingTime - elapsedMinutes);
                totalRemainingCuttingMinutes += remaining;
            } else {
                totalRemainingCuttingMinutes += cuttingTime;
            }
        }

        // Not-yet series that are due this shift or older: add their cutting time
        double totalDueCuttingMinutes = 0;
        for (SerieDTO s : notYet) {
            double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
            totalDueCuttingMinutes += cuttingTime;
        }

        double totalWorkMinutes = totalRemainingCuttingMinutes + totalDueCuttingMinutes;
        double overflowMinutes = Math.max(0, totalWorkMinutes - minutesUntilShiftEnd);

        // Next shift plan de charge = SHIFT_DURATION_MINUTES * activeMachineCount (total capacity across all machines)
        double nextShiftCapacity = SHIFT_DURATION_MINUTES * activeMachineCount;
        double overflowPercentage = nextShiftCapacity > 0
                ? (overflowMinutes / nextShiftCapacity) * 100.0
                : 0;

        overflow.put("currentShiftDate", currentShift[0]);
        overflow.put("currentShiftNumber", currentShift[1]);
        overflow.put("shiftEnd", shiftEnd.toString());
        overflow.put("minutesUntilShiftEnd", minutesUntilShiftEnd);
        overflow.put("totalRemainingCuttingMinutes", Math.round(totalRemainingCuttingMinutes));
        overflow.put("totalDueSeriesCuttingMinutes", Math.round(totalDueCuttingMinutes));
        overflow.put("totalWorkMinutes", Math.round(totalWorkMinutes));
        overflow.put("canFinishBeforeShiftEnd", overflowMinutes <= 0);
        overflow.put("overflowMinutes", Math.round(overflowMinutes));
        overflow.put("overflowPercentageNextShift", Math.round(overflowPercentage * 10.0) / 10.0);
        overflow.put("inProgressCount", inProgress.size());
        overflow.put("dueSeriesCount", notYet.size());

        return overflow;
    }

    // ======================== METRICS ========================

    /**
     * Load real box counts per sequence. Falls back to empty map if no data.
     */
    private Map<String, Integer> loadBoxCounts(Set<String> sequences) {
        Map<String, Integer> result = new HashMap<>();
        if (sequences == null || sequences.isEmpty() || boxInfoRepository == null) {
            return result;
        }
        List<String> seqList = new ArrayList<>(sequences);
        for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
            for (Object[] row : boxInfoRepository.countBoxesBySequences(batch)) {
                String seq = (String) row[0];
                Number count = (Number) row[1];
                if (seq != null && count != null) {
                    result.put(seq, count.intValue());
                }
            }
        }
        return result;
    }

    private Map<String, Object> calculateMetrics(List<Map<String, Object>> blocks,
                                                  List<Map<String, Object>> unassigned,
                                                  Map<String, List<Map<String, Object>>> machinesByZone,
                                                  Map<String, Integer> boxCounts) {
        Map<String, Object> metrics = new LinkedHashMap<>();

        metrics.put("seriesEnAttente", unassigned.size());

        long activeMachines = machinesByZone.values().stream()
                .flatMap(List::stream)
                .filter(m -> "M".equals(m.get("status")))
                .count();
        metrics.put("machinesActives", activeMachines);

        // Active sequences per zone (from blocks + unassigned)
        Map<String, Set<String>> activeSequencesByZone = new LinkedHashMap<>();
        Set<String> allActiveSequences = new HashSet<>();

        for (Map<String, Object> block : blocks) {
            if (!"COMPLETED".equals(block.get("status"))) {
                String seq = (String) block.get("sequence");
                String zone = (String) block.get("zoneCoupe");
                if (seq != null) {
                    allActiveSequences.add(seq);
                    if (zone != null) {
                        activeSequencesByZone.computeIfAbsent(zone, k -> new HashSet<>()).add(seq);
                    }
                }
            }
        }
        for (Map<String, Object> s : unassigned) {
            String seq = (String) s.get("sequence");
            String zone = (String) s.get("sequenceZone");
            if (seq != null) {
                allActiveSequences.add(seq);
                if (zone != null) {
                    activeSequencesByZone.computeIfAbsent(zone, k -> new HashSet<>()).add(seq);
                }
            }
        }

        // Real box counts per sequence (fallback to 0 if unknown)
        int totalBoxes = 0;
        for (String seq : allActiveSequences) {
            totalBoxes += boxCounts.getOrDefault(seq, 0);
        }
        metrics.put("sequencesIncompletes", allActiveSequences.size());
        metrics.put("boitesIncompletes", totalBoxes);

        // Zone capacity using real box counts
        Map<String, Map<String, Object>> zoneCapacity = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : machinesByZone.entrySet()) {
            String zone = entry.getKey();
            int machineCount = entry.getValue().size();
            Set<String> zoneSeqs = activeSequencesByZone.getOrDefault(zone, Collections.emptySet());
            int zoneBoxes = 0;
            for (String seq : zoneSeqs) {
                zoneBoxes += boxCounts.getOrDefault(seq, 0);
            }
            // Max boxes heuristic: average boxes per sequence * machines (fallback to 16 avg)
            int avgBoxesPerSeq = allActiveSequences.isEmpty() ? 16
                    : Math.max(1, totalBoxes / allActiveSequences.size());
            int maxBoxes = machineCount * avgBoxesPerSeq;

            Map<String, Object> zoneInfo = new LinkedHashMap<>();
            zoneInfo.put("machineCount", machineCount);
            zoneInfo.put("maxBoxes", maxBoxes);
            zoneInfo.put("activeBoxes", zoneBoxes);
            zoneInfo.put("activeSequences", zoneSeqs.size());
            zoneInfo.put("available", maxBoxes - zoneBoxes);
            zoneInfo.put("overloaded", zoneBoxes > maxBoxes);
            zoneCapacity.put(zone, zoneInfo);
        }
        metrics.put("zoneCapacity", zoneCapacity);

        double totalCharge = 0;
        for (Map<String, Object> block : blocks) {
            if ("CUTTING".equals(block.get("status")) || "READY_TO_CUT".equals(block.get("status"))) {
                totalCharge += block.get("estimatedCuttingTime") != null ?
                        ((Number) block.get("estimatedCuttingTime")).doubleValue() : 0;
            }
        }
        for (Map<String, Object> s : unassigned) {
            totalCharge += s.get("estimatedCuttingTime") != null ?
                    ((Number) s.get("estimatedCuttingTime")).doubleValue() : 0;
        }
        metrics.put("chargeRestanteMinutes", Math.round(totalCharge));

        return metrics;
    }

    // ======================== MACHINE STATE ========================

    public Map<String, List<Map<String, Object>>> getMachineStates() {
        return buildMachinesWithState();
    }

    // ======================== SPREADING TIME ========================

    private double estimateSpreadingTime(SerieDTO serie) {
        double longueur = serie.longueur != null ? serie.longueur : 0;
        int nbrCouche = serie.nbrCouche != null ? serie.nbrCouche : 1;
        return (longueur * nbrCouche * COEF_SPREADING_PER_METRE) + COEF_SETUP_TIME;
    }

    // ======================== PRODUCTION FORM HELPERS ========================

    /**
     * Get machine schedule info for the production form (Form.js).
     * Returns:
     *   - nextWaiting: top 3 Waiting series on this machine, sorted by dueDate/dueShift
     *   - lastFinish: latest dateFinCoupe (or estimatedFinCoupe) of non-Waiting series on this machine
     */
    public Map<String, Object> getMachineSchedule(String machineNom) {
        LocalDateTime now = LocalDateTime.now();
        List<SerieDTO> allSeries = loadRelevantSeries(Collections.emptyList());
        Map<String, List<SerieDTO>> split = splitSeries(allSeries, now.minusHours(12), now.plusHours(12));
        List<SerieDTO> inProgress = split.get("inProgress");
        List<SerieDTO> notYet = split.get("notYet");

        // Enrich dueDate/dueShift (same as autoDispatch)
        Set<String> allSequences = new HashSet<>();
        for (SerieDTO s : allSeries) allSequences.add(s.sequence);
        Map<String, Map<String, Object>> sequenceInfoMap = new HashMap<>();
        List<String> seqList = new ArrayList<>(allSequences);
        for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
            for (Object[] row : cuttingRequestDataRepository.findSequenceInfoLight(batch)) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("sequence", row[0]);
                info.put("zone", row[1]);
                info.put("dueDate", row[2] != null ? row[2].toString() : null);
                info.put("dueShift", row[3]);
                sequenceInfoMap.put((String) row[0], info);
            }
        }
        for (SerieDTO s : allSeries) {
            Map<String, Object> info = sequenceInfoMap.get(s.sequence);
            if (info != null) {
                if (info.get("dueDate") != null) {
                    s.dueDate = LocalDate.parse((String) info.get("dueDate"));
                }
                if (info.get("dueShift") != null) {
                    s.dueShift = Integer.parseInt(info.get("dueShift").toString());
                }
            }
        }

        // Pre-load cutting time map and machine type sets once
        Map<String, Double> cuttingTimeMap = buildCuttingTimeMap(inProgress, notYet);
        Set<String> laserDxf = loadLaserDxfMachines();
        Set<String> gerber = loadGerberMachines();

        // Filter series on this machine (checks both tableCoupe and tableMatelassage)
        List<SerieDTO> onMachine = new ArrayList<>();
        for (SerieDTO s : inProgress) {
            if (machineNom.equals(s.tableCoupe) || machineNom.equals(s.tableMatelassage)) onMachine.add(s);
        }
        for (SerieDTO s : notYet) {
            if (machineNom.equals(s.tableCoupe) || machineNom.equals(s.tableMatelassage)) onMachine.add(s);
        }

        // Last finish time of non-Waiting series
        LocalDateTime lastFinish = null;
        boolean lastFinishIsEstimated = false;
        for (SerieDTO s : onMachine) {
            if (!"Waiting".equals(s.statusCoupe)) {
                LocalDateTime finish = s.dateFinCoupe;
                if (finish == null && s.dateDebutCoupe != null) {
                    // In progress — estimate finish
                    double cuttingTime = getEstimatedCuttingTime(s, cuttingTimeMap, laserDxf, gerber);
                    finish = s.dateDebutCoupe.plusMinutes((long) cuttingTime);
                    lastFinishIsEstimated = true;
                }
                if (finish != null && (lastFinish == null || finish.isAfter(lastFinish))) {
                    lastFinish = finish;
                }
            }
        }

        // Next 3 Waiting series sorted by priority
        List<SerieDTO> waiting = new ArrayList<>();
        for (SerieDTO s : onMachine) {
            if ("Waiting".equals(s.statusCoupe)) waiting.add(s);
        }
        waiting.sort(Comparator
                .comparing((SerieDTO s) -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparing(s -> s.sequence != null ? s.sequence : "")
                .thenComparing(s -> s.serie != null ? s.serie : ""));

        List<Map<String, Object>> nextWaiting = new ArrayList<>();
        for (int i = 0; i < Math.min(3, waiting.size()); i++) {
            SerieDTO s = waiting.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("serie", s.serie);
            m.put("sequence", s.sequence);
            m.put("partNumberMaterial", s.partNumberMaterial);
            m.put("description", s.description);
            m.put("dueDate", s.dueDate != null ? s.dueDate.toString() : null);
            m.put("dueShift", s.dueShift);
            m.put("statusMatelassage", s.statusMatelassage);
            nextWaiting.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("machineNom", machineNom);
        result.put("lastFinish", lastFinish != null ? lastFinish.toString() : null);
        result.put("lastFinishIsEstimated", lastFinishIsEstimated);
        result.put("nextWaiting", nextWaiting);
        result.put("waitingCount", waiting.size());
        return result;
    }

    // ======================== DISPATCH ENGINE ========================

    public Map<String, Object> getDispatchEngineState() {
        Map<String, Object> state = new LinkedHashMap<>();
        if (continuousDispatchOptimizerService != null) {
            state.put("state", continuousDispatchOptimizerService.getState() != null
                    ? continuousDispatchOptimizerService.getState().name() : null);
            state.put("mode", continuousDispatchOptimizerService.getMode() != null
                    ? continuousDispatchOptimizerService.getMode().name() : null);
            state.put("iteration", continuousDispatchOptimizerService.getIteration());
            state.put("currentSpread", continuousDispatchOptimizerService.getCurrentSpread());
            state.put("bestSpread", continuousDispatchOptimizerService.getBestSpread());
            state.put("currentRunId", continuousDispatchOptimizerService.getCurrentRunId());
        }
        return state;
    }

    public List<Map<String, Object>> getMaterialAlerts() {
        List<SerieDTO> allSeries = loadRelevantSeries(Collections.emptyList());
        return buildMaterialAlerts(allSeries);
    }

    /**
     * Build zone assignment info for relevant sequences.
     */
    private Map<String, Map<String, Object>> buildZoneAssignments(Set<String> sequenceIds, List<SerieDTO> allSeries) {
        if (sequenceIds == null || sequenceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Load dispatch info for active sequences
        Map<String, Object[]> dispatchInfoBySeq = new HashMap<>();
        List<Object[]> requestRows = cuttingRequestRepository.findAllActiveLight();
        for (Object[] r : requestRows) {
            String seq = (String) r[0];
            if (sequenceIds.contains(seq)) {
                dispatchInfoBySeq.put(seq, r);
            }
        }

        // Build table -> zone map for lock resolution
        Set<String> tableNoms = new LinkedHashSet<>();
        for (SerieDTO s : allSeries) {
            if (s.tableCoupe != null && !s.tableCoupe.trim().isEmpty()) {
                tableNoms.add(s.tableCoupe);
            }
        }
        Map<String, LockResolver.TableZoneInfo> tableToZone = loadTableZoneMapForLock(tableNoms);

        // Group series by sequence for lock resolution
        Map<String, List<LockResolver.SerieLockInput>> lockInputsBySeq = new HashMap<>();
        for (SerieDTO s : allSeries) {
            if (s.sequence != null && s.tableCoupe != null) {
                lockInputsBySeq.computeIfAbsent(s.sequence, k -> new ArrayList<>())
                        .add(new LockResolver.SerieLockInput(s.serie, s.statusCoupe, s.tableCoupe, s.dateDebutCoupe));
            }
        }

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String seq : sequenceIds) {
            Object[] info = dispatchInfoBySeq.get(seq);
            if (info == null) continue;

            String dispatchedZone = (String) info[1];
            String zoneAcceptanceStatus = (String) info[2];
            boolean pinnedByChef = info[3] != null && Boolean.TRUE.equals(info[3]);

            Optional<LockResolver.LockResult> lock = LockResolver.resolve(
                    dispatchedZone, zoneAcceptanceStatus,
                    lockInputsBySeq.getOrDefault(seq, Collections.emptyList()),
                    tableToZone);

            String effectiveZone;
            String zoneSource;
            boolean locked;
            if (lock.isPresent()) {
                effectiveZone = lock.get().getLockZoneNom();
                zoneSource = lock.get().getReason().name();
                locked = true;
            } else {
                effectiveZone = dispatchedZone;
                zoneSource = dispatchedZone != null ? "DISPATCHED" : "NONE";
                locked = pinnedByChef || "ACCEPTED".equalsIgnoreCase(zoneAcceptanceStatus);
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dispatchedZone", dispatchedZone);
            m.put("effectiveZone", effectiveZone);
            m.put("zoneSource", zoneSource);
            m.put("locked", locked);
            m.put("zoneAcceptanceStatus", zoneAcceptanceStatus);
            result.put(seq, m);
        }
        return result;
    }

    /**
     * Build material alerts for sequences with MISSING material status.
     */
    private List<Map<String, Object>> buildMaterialAlerts(List<SerieDTO> allSeries) {
        // Group materials by sequence and zone
        Map<String, Set<String>> materialsBySequence = new HashMap<>();
        Map<String, String> zoneBySequence = new HashMap<>();

        for (SerieDTO s : allSeries) {
            if (s.sequence == null || s.partNumberMaterial == null || s.partNumberMaterial.isBlank()) continue;
            materialsBySequence.computeIfAbsent(s.sequence, k -> new HashSet<>()).add(s.partNumberMaterial.trim());
            String zone = s.zoneCoupe != null ? s.zoneCoupe : s.zoneMatelassage;
            if (zone != null && !zone.isBlank()) {
                zoneBySequence.put(s.sequence, zone);
            }
        }

        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : materialsBySequence.entrySet()) {
            String seq = entry.getKey();
            String zone = zoneBySequence.get(seq);
            if (zone == null || zone.isBlank()) continue;

            Map<String, MaterialAvailabilityChecker.MaterialStatus> statusMap =
                    materialAvailabilityChecker.check(entry.getValue(), zone);

            boolean hasMissing = statusMap.values().stream()
                    .anyMatch(st -> st == MaterialAvailabilityChecker.MaterialStatus.NOT_IN_ZONE);
            if (hasMissing) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("sequence", seq);
                alert.put("zone", zone);
                List<String> missing = statusMap.entrySet().stream()
                        .filter(e -> e.getValue() == MaterialAvailabilityChecker.MaterialStatus.NOT_IN_ZONE)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                alert.put("missingMaterials", missing);
                alerts.add(alert);
            }
        }
        return alerts;
    }

    /**
     * Batch resolve tableNom -> ZoneInfo for lock resolution.
     */
    private Map<String, LockResolver.TableZoneInfo> loadTableZoneMapForLock(Set<String> tableNoms) {
        if (tableNoms == null || tableNoms.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> list = new ArrayList<>(tableNoms);
        Map<String, LockResolver.TableZoneInfo> out = new HashMap<>(list.size() * 2);
        for (int i = 0; i < list.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = list.subList(i, Math.min(i + SQL_BATCH_SIZE, list.size()));
            for (Object[] row : productionTableRepository.findZoneInfoByTableNoms(batch)) {
                String tableNom = (String) row[0];
                String zoneNom = (String) row[1];
                Object catObj = row[2];
                Zone.Category category = catObj instanceof Zone.Category
                        ? (Zone.Category) catObj
                        : Zone.Category.valueOf(String.valueOf(catObj));
                out.put(tableNom, new LockResolver.TableZoneInfo(zoneNom, category));
            }
        }
        return out;
    }

    /**
     * Compute per-shift completion metrics: total remaining cutting minutes,
     * max estimated fin coupe, and sequence count for shifts 1-3.
     * Also returns a flat list of sequence details sorted by (dueDate, dueShift).
     */
    public Map<String, Object> computeShiftCompletion() {
        return computeShiftCompletion(null);
    }

    public Map<String, Object> computeShiftCompletion(Map<String, Object> timelineResult) {
        if (timelineResult == null) {
            timelineResult = getTimelineData(12, 12, Collections.emptyList());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> timelineBlocks = (List<Map<String, Object>>) timelineResult.get("timelineBlocks");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> sequenceInfo = (Map<String, Map<String, Object>>) timelineResult.get("sequenceInfo");

        // Extract all sequence IDs from sequenceInfo (or timeline blocks as fallback)
        Set<String> allSequenceIds = new LinkedHashSet<>();
        if (sequenceInfo != null) {
            allSequenceIds.addAll(sequenceInfo.keySet());
        }
        if (timelineBlocks != null) {
            for (Map<String, Object> block : timelineBlocks) {
                String seq = (String) block.get("sequence");
                if (seq != null) {
                    allSequenceIds.add(seq);
                }
            }
        }

        // Optimization 1: find only incomplete sequences at DB level
        List<String> incompleteSeqIds = new ArrayList<>();
        if (!allSequenceIds.isEmpty()) {
            incompleteSeqIds = serieDataRepository.findIncompleteSequences(new ArrayList<>(allSequenceIds));
        }
        // If the query returns nothing (fallback), use all sequences
        Set<String> sequenceIds = incompleteSeqIds.isEmpty() ? allSequenceIds : new LinkedHashSet<>(incompleteSeqIds);

        // Optimization 2: load series with light projection (Object[] instead of full entity)
        Map<String, List<Object[]>> seriesBySequence = new LinkedHashMap<>();
        if (!sequenceIds.isEmpty()) {
            for (Object[] row : serieDataRepository.findShiftCompletionSeriesBySequences(new ArrayList<>(sequenceIds))) {
                String seq = (String) row[1];
                if (seq != null) {
                    seriesBySequence.computeIfAbsent(seq, k -> new ArrayList<>()).add(row);
                }
            }
        }

        // Load CuttingRequest zone/pin info for incomplete sequences only (light projection)
        Map<String, Object[]> requestInfoBySequence = new HashMap<>();
        if (!sequenceIds.isEmpty()) {
            for (Object[] row : cuttingRequestRepository.findBySequencesLight(new ArrayList<>(sequenceIds))) {
                String seq = (String) row[0];
                if (seq != null) {
                    requestInfoBySequence.put(seq, row);
                }
            }
        }

        // Aggregate per-sequence from light projection rows
        // Columns: 0=serie, 1=sequence, 2=statusCoupe, 3=statusMatelassage, 4=tempsDeCoupe,
        // 5=tableCoupe, 6=tableMatelassage, 7=dateDebutCoupe, 8=dateFinCoupe
        Map<String, SequenceAggregate> aggMap = new LinkedHashMap<>();
        for (String seq : sequenceIds) {
            List<Object[]> series = seriesBySequence.getOrDefault(seq, Collections.emptyList());
            if (series.isEmpty()) {
                continue;
            }
            SequenceAggregate agg = new SequenceAggregate();
            agg.sequence = seq;

            for (Object[] row : series) {
                agg.serieCount++;
                String statusCoupe = (String) row[2];
                String statusMatelassage = (String) row[3];

                if (statusCoupe != null && statusCoupe.equalsIgnoreCase("Complete")) {
                    agg.completedCoupe++;
                }
                if (statusMatelassage != null && statusMatelassage.equalsIgnoreCase("Complete")) {
                    agg.completedMatelassage++;
                }

                Number cuttingTimeNum = (Number) row[4];
                if (cuttingTimeNum != null) {
                    double cuttingTime = cuttingTimeNum.doubleValue();
                    if (statusCoupe == null || !statusCoupe.equalsIgnoreCase("Complete")) {
                        agg.remainingMinutes += cuttingTime;
                    }
                    agg.totalMinutes += cuttingTime;
                }

                java.time.LocalDateTime dateDebutCoupe = (java.time.LocalDateTime) row[7];
                java.time.LocalDateTime dateFinCoupe = (java.time.LocalDateTime) row[8];
                if (dateDebutCoupe != null && dateFinCoupe == null) {
                    agg.hasCutting = true;
                }
                if ("Complete".equalsIgnoreCase(statusMatelassage) && dateDebutCoupe == null) {
                    agg.hasReadyToCut = true;
                }
                if ("In progress".equalsIgnoreCase(statusMatelassage) && dateDebutCoupe == null) {
                    agg.hasSpreading = true;
                }
            }

            // Backward-compatible completedCount = completedCoupe
            agg.completedCount = agg.completedCoupe;

            // Zone/pin info from CuttingRequest light projection
            // Columns: 0=sequence, 1=dispatchedZone, 2=zoneAcceptanceStatus, 3=pinnedByChef, 4=zoneNom, 5=dueDate
            Object[] reqInfo = requestInfoBySequence.get(seq);
            if (reqInfo != null) {
                agg.zone = (String) reqInfo[4];
                agg.dispatchedZone = (String) reqInfo[1];
                agg.pinnedByChef = reqInfo[3] != null ? (Boolean) reqInfo[3] : null;
                agg.zoneAcceptanceStatus = (String) reqInfo[2];
            }

            // dueDate/dueShift from sequenceInfo
            Map<String, Object> info = sequenceInfo != null ? sequenceInfo.get(seq) : null;
            if (info != null) {
                if (info.get("dueDate") != null) {
                    agg.dueDate = info.get("dueDate").toString();
                }
                if (info.get("dueShift") != null) {
                    agg.dueShift = Integer.parseInt(info.get("dueShift").toString());
                }
            }

            // Determine overall status
            if (agg.completedCoupe == agg.serieCount && agg.completedMatelassage == agg.serieCount) {
                agg.overallStatus = "Complete";
            } else if (agg.hasCutting) {
                agg.overallStatus = "In progress";
            } else if (agg.hasReadyToCut || agg.hasSpreading) {
                agg.overallStatus = "In progress";
            } else {
                agg.overallStatus = "Waiting";
            }

            // Only keep incomplete sequences (safety net — DB query should already filter)
            if (!"Complete".equals(agg.overallStatus)) {
                aggMap.put(seq, agg);
            }
        }

        // Keep maxEstimatedFinCoupe from timeline scan (latest among visible blocks)
        if (timelineBlocks != null) {
            for (Map<String, Object> block : timelineBlocks) {
                String seq = (String) block.get("sequence");
                if (seq == null) continue;
                SequenceAggregate agg = aggMap.get(seq);
                if (agg == null) continue;
                String estFin = (String) block.get("estimatedFinCoupe");
                if (estFin != null && (agg.maxEstimatedFinCoupe == null || estFin.compareTo(agg.maxEstimatedFinCoupe) > 0)) {
                    agg.maxEstimatedFinCoupe = estFin;
                }
            }
        }

        // Build response with incomplete sequences only + their series details
        int totalIncompleteSeries = 0;
        int totalCompletedInIncomplete = 0;
        List<Map<String, Object>> sequences = new ArrayList<>();
        for (SequenceAggregate agg : aggMap.values()) {
            Map<String, Object> seqData = new LinkedHashMap<>();
            seqData.put("sequence", agg.sequence);
            seqData.put("dueDate", agg.dueDate);
            seqData.put("dueShift", agg.dueShift);
            seqData.put("remainingMinutes", round2(agg.remainingMinutes));
            seqData.put("totalMinutes", round2(agg.totalMinutes));
            seqData.put("serieCount", agg.serieCount);
            seqData.put("completedCount", agg.completedCount);
            seqData.put("completedCoupe", agg.completedCoupe);
            seqData.put("completedMatelassage", agg.completedMatelassage);
            seqData.put("maxEstimatedFinCoupe", agg.maxEstimatedFinCoupe);
            seqData.put("zone", agg.zone);
            seqData.put("dispatchedZone", agg.dispatchedZone);
            seqData.put("pinnedByChef", agg.pinnedByChef);
            seqData.put("zoneAcceptanceStatus", agg.zoneAcceptanceStatus);
            seqData.put("status", agg.overallStatus);

            // Include all series for this incomplete sequence (complete + incomplete)
            List<Object[]> seriesList = seriesBySequence.getOrDefault(agg.sequence, Collections.emptyList());
            List<Map<String, Object>> seriesDetails = new ArrayList<>(seriesList.size());
            for (Object[] row : seriesList) {
                Map<String, Object> sd = new LinkedHashMap<>();
                sd.put("serie", row[0]);
                sd.put("statusCoupe", row[2]);
                sd.put("statusMatelassage", row[3]);
                sd.put("tempsDeCoupe", row[4]);
                sd.put("tableCoupe", row[5]);
                sd.put("tableMatelassage", row[6]);
                sd.put("dateDebutCoupe", row[7]);
                sd.put("dateFinCoupe", row[8]);
                seriesDetails.add(sd);
            }
            seqData.put("series", seriesDetails);
            sequences.add(seqData);

            totalIncompleteSeries += agg.serieCount;
            totalCompletedInIncomplete += agg.completedCount;
        }

        sequences.sort((a, b) -> {
            String da = (String) a.get("dueDate");
            String db = (String) b.get("dueDate");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            int cmp = da.compareTo(db);
            if (cmp != 0) return cmp;
            Integer sa = (Integer) a.get("dueShift");
            Integer sb = (Integer) b.get("dueShift");
            if (sa == null && sb == null) return 0;
            if (sa == null) return 1;
            if (sb == null) return -1;
            return Integer.compare(sa, sb);
        });

        // Group by (dueDate / dueShift) pairs — oldest first, only incomplete sequences
        Map<String, GroupAggregate> groupMap = new LinkedHashMap<>();
        for (Map<String, Object> seqData : sequences) {
            String dueDate = (String) seqData.get("dueDate");
            Integer dueShift = (Integer) seqData.get("dueShift");
            String groupKey = (dueDate != null ? dueDate : "ZZZZ") + "|" + (dueShift != null ? dueShift : 99);

            GroupAggregate grp = groupMap.computeIfAbsent(groupKey, k -> new GroupAggregate());
            grp.dueDate = dueDate;
            grp.dueShift = dueShift;
            grp.sequenceCount++;
            grp.totalRemainingMinutes += ((Number) seqData.getOrDefault("remainingMinutes", 0.0)).doubleValue();
            grp.totalMinutes += ((Number) seqData.getOrDefault("totalMinutes", 0.0)).doubleValue();
            grp.completedCount += ((Number) seqData.getOrDefault("completedCount", 0)).intValue();
            grp.serieCount += ((Number) seqData.getOrDefault("serieCount", 0)).intValue();

            String estFin = (String) seqData.get("maxEstimatedFinCoupe");
            if (estFin != null && (grp.maxEstimatedFinCoupe == null || estFin.compareTo(grp.maxEstimatedFinCoupe) > 0)) {
                grp.maxEstimatedFinCoupe = estFin;
            }
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        for (GroupAggregate grp : groupMap.values()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("dueDate", grp.dueDate);
            g.put("dueShift", grp.dueShift);
            g.put("totalRemainingMinutes", round2(grp.totalRemainingMinutes));
            g.put("totalMinutes", round2(grp.totalMinutes));
            g.put("sequenceCount", grp.sequenceCount);
            g.put("serieCount", grp.serieCount);
            g.put("completedCount", grp.completedCount);
            g.put("maxEstimatedFinCoupe", grp.maxEstimatedFinCoupe);
            groups.add(g);
        }

        // Sort groups by (dueDate asc, dueShift asc) — oldest / highest priority first
        groups.sort((a, b) -> {
            String da = (String) a.get("dueDate");
            String db = (String) b.get("dueDate");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            int cmp = da.compareTo(db);
            if (cmp != 0) return cmp;
            Integer sa = (Integer) a.get("dueShift");
            Integer sb = (Integer) b.get("dueShift");
            if (sa == null && sb == null) return 0;
            if (sa == null) return 1;
            if (sb == null) return -1;
            return Integer.compare(sa, sb);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        result.put("sequences", sequences);
        result.put("shifts", groups);
        // Summary metrics for the UI header
        result.put("totalIncompleteSequences", sequences.size());
        result.put("totalIncompleteSeries", totalIncompleteSeries);
        result.put("totalCompletedInIncomplete", totalCompletedInIncomplete);
        return result;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class SequenceAggregate {
        String sequence;
        String dueDate;
        Integer dueShift;
        double remainingMinutes;
        double totalMinutes;
        int serieCount;
        int completedCount;
        int completedCoupe;
        int completedMatelassage;
        String maxEstimatedFinCoupe;
        boolean hasCutting;
        boolean hasReadyToCut;
        boolean hasSpreading;
        String zone;
        String dispatchedZone;
        Boolean pinnedByChef;
        String zoneAcceptanceStatus;
        String overallStatus;
    }

    private static class GroupAggregate {
        String dueDate;
        Integer dueShift;
        double totalRemainingMinutes;
        double totalMinutes;
        int sequenceCount;
        int serieCount;
        int completedCount;
        String maxEstimatedFinCoupe;
    }

    // ======================== INNER CLASS ========================

    private static class MachineState {
        String nom;
        String zone;
        String machineType;
        double availableLength;
        double tableLength;
        int queueLength;
        Set<String> activeSequences = new HashSet<>();

        /** Zone restriction applies only to Lectra and Lectra IP6 */
        boolean isZoneRestricted() {
            return "Lectra".equalsIgnoreCase(machineType) || "Lectra IP6".equalsIgnoreCase(machineType);
        }
    }
}
