package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.ScanRouleau;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.CuttingRequest.SequenceStatus;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository;
import com.lear.MGCMS.repositories.dispatcher.EngineScheduleEntryRepository;
import com.lear.MGCMS.services.dispatcher.ContinuousDispatchOptimizerService;
import com.lear.MGCMS.services.dispatcher.SerieStatusDateValidator;
import com.lear.MGCMS.services.dispatcher.WorkbenchCacheService;
import com.lear.MGCMS.services.OrdonnancementService.SerieDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Material Demand Forecast Service — Prévision Matière.
 *
 * <p>For a selected zone and time horizon (2h, 3h, 4h, 8h), finds series that:
 * <ul>
 *   <li>Are waiting to be spread ({@code statusMatelassage = 'Waiting'})</li>
 *   <li>Will start cutting within the horizon ({@code dateDebutCoupe} is null or within N hours)</li>
 *   <li>Are not already completed ({@code statusCoupe != 'Complete'})</li>
 * </ul>
 *
 * <p>Then compares material demand against roll stock in the zone's roll locations.
 * For shortages, checks <b>all production locations</b> to see if the material exists
 * elsewhere and could be transferred.</p>
 */
@Service
public class MaterialDemandForecastService {

    private static final int SQL_BATCH_SIZE = 2000;

    @Autowired
    private CuttingRequestSerieDataRepository serieDataRepository;

    @Autowired
    private CuttingRequestDataRepository requestDataRepository;

    @Autowired
    private CuttingRequestRepository cuttingRequestRepository;

    @Autowired
    private ScanRouleauRepository scanRouleauRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private EngineScheduleEntryRepository scheduleEntryRepository;

    @Autowired(required = false)
    private SerieStatusDateValidator serieStatusDateValidator;

    @Autowired(required = false)
    private ContinuousDispatchOptimizerService optimizerService;

    @Autowired(required = false)
    private WorkbenchCacheService workbenchCacheService;

    @Autowired(required = false)
    private OrdonnancementService ordonnancementService;

    /**
     * Forecast material demand for a specific zone over the next N hours.
     *
     * @param zoneName Zone name (e.g., "TFZ")
     * @param horizonHours Forecast horizon in hours (e.g., 4)
     * @return Map with demand report
     */
    public Map<String, Object> forecastDemand(String zoneName, int horizonHours) {
        boolean allZones = isAllZones(zoneName);
        String normalizedZone = allZones ? "ALL" : zoneName;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("zone", normalizedZone);
        result.put("horizonHours", horizonHours);
        result.put("generatedAt", LocalDateTime.now().toString());

        Zone zone = null;
        if (!allZones) {
            zone = zoneRepository.findById(zoneName).orElse(null);
        }
        if (!allZones && zone == null) {
            result.put("error", "Zone non trouvée: " + zoneName);
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizonEnd = now.plusHours(horizonHours);

        // 1. Load all series that belong to this zone and match the criteria
        List<SerieDTO> plannedSeries = loadPlannedSeriesForZone(allZones ? null : zoneName, now, horizonEnd);

        // 2. Group demand by material (strip "P" prefix)
        Map<String, MaterialDemand> demandMap = new LinkedHashMap<>();
        for (SerieDTO s : plannedSeries) {
            String material = stripPrefix(s.partNumberMaterial, "P");
            if (material == null || material.isEmpty()) continue;

            MaterialDemand demand = demandMap.computeIfAbsent(material, k -> new MaterialDemand(k));
            double serieMetrage = (s.longueur != null ? s.longueur : 0)
                                 * (s.nbrCouche != null ? s.nbrCouche : 1);
            demand.totalMetrageNeeded += serieMetrage;
            demand.serieCount++;
            demand.serieIds.add(s.serie);
            demand.sequences.add(s.sequence);
        }

        // 3. Get roll stock in the selected scope and in all production.
        Map<String, RollStock> selectedStockMap = loadStockForScope(allZones ? null : zone);
        Map<String, RollStock> allProductionStockMap = loadAllStockByMaterial();

        // 4. Build report: compare demand vs stock
        List<Map<String, Object>> materials = new ArrayList<>();
        int shortageCount = 0;
        int noneCount = 0;
        double totalDeficit = 0;

        for (Map.Entry<String, MaterialDemand> entry : demandMap.entrySet()) {
            String material = entry.getKey();
            MaterialDemand demand = entry.getValue();
            RollStock selectedStock = selectedStockMap.get(material);
            RollStock allStock = allProductionStockMap.get(material);

            double availableInZone = selectedStock != null ? selectedStock.totalMetrage : 0;
            int rollCountInZone = selectedStock != null ? selectedStock.rollCount : 0;
            double availableTotal = allStock != null ? allStock.totalMetrage : 0;
            double availableOutOfZone = Math.max(0, availableTotal - availableInZone);
            double deficit = Math.max(0, demand.totalMetrageNeeded - availableTotal);
            double zoneGap = Math.max(0, demand.totalMetrageNeeded - availableInZone);

            String status;
            if (demand.totalMetrageNeeded <= 0) {
                status = "RETURN_TO_STOCK";
            } else if (zoneGap == 0) {
                status = "OK";
            } else if (availableTotal >= demand.totalMetrageNeeded) {
                status = "OUT_OF_ZONE";
                shortageCount++;
            } else if (availableTotal == 0) {
                status = "NONE";
                noneCount++;
            } else {
                status = "SHORTAGE";
                shortageCount++;
            }

            Map<String, Object> materialReport = new LinkedHashMap<>();
            materialReport.put("material", material);
            materialReport.put("needed", Math.round(demand.totalMetrageNeeded * 100.0) / 100.0);
            materialReport.put("available", Math.round(availableInZone * 100.0) / 100.0);
            materialReport.put("availableInZone", Math.round(availableInZone * 100.0) / 100.0);
            materialReport.put("availableOutOfZone", Math.round(availableOutOfZone * 100.0) / 100.0);
            materialReport.put("availableTotal", Math.round(availableTotal * 100.0) / 100.0);
            materialReport.put("deficit", Math.round(deficit * 100.0) / 100.0);
            materialReport.put("zoneGap", Math.round(zoneGap * 100.0) / 100.0);
            materialReport.put("status", status);
            materialReport.put("serieCount", demand.serieCount);
            materialReport.put("rollCount", rollCountInZone);
            materialReport.put("locations", selectedStock != null ? new ArrayList<>(selectedStock.locations) : Collections.emptyList());
            materialReport.put("serieIds", demand.serieIds);
            materialReport.put("sequences", new ArrayList<>(demand.sequences));

            // 5. For shortages, find where material exists in production
            if (zoneGap > 0) {
                totalDeficit += deficit;
                Map<String, Object> transferInfo = allZones
                        ? buildAllZonesSuggestion(material, deficit, availableOutOfZone)
                        : findAllTransferOptions(zoneName, material, zoneGap);
                materialReport.put("suggestion", transferInfo);
            }

            materials.add(materialReport);
        }

        // 6. Materials physically present in the selected zone/scope but not
        // needed in the horizon should be returned to stock or moved away.
        for (Map.Entry<String, RollStock> entry : selectedStockMap.entrySet()) {
            if (demandMap.containsKey(entry.getKey())) continue;
            RollStock stock = entry.getValue();
            Map<String, Object> materialReport = new LinkedHashMap<>();
            materialReport.put("material", entry.getKey());
            materialReport.put("needed", 0.0);
            materialReport.put("available", Math.round(stock.totalMetrage * 100.0) / 100.0);
            materialReport.put("availableInZone", Math.round(stock.totalMetrage * 100.0) / 100.0);
            materialReport.put("availableOutOfZone", 0.0);
            materialReport.put("availableTotal", Math.round(stock.totalMetrage * 100.0) / 100.0);
            materialReport.put("deficit", 0.0);
            materialReport.put("zoneGap", 0.0);
            materialReport.put("status", "RETURN_TO_STOCK");
            materialReport.put("serieCount", 0);
            materialReport.put("rollCount", stock.rollCount);
            materialReport.put("locations", new ArrayList<>(stock.locations));
            materialReport.put("serieIds", Collections.emptyList());
            materialReport.put("sequences", Collections.emptyList());
            materials.add(materialReport);
        }

        // Sort: NONE first, then SHORTAGE, then OK; within each group sort by deficit desc
        materials.sort((a, b) -> {
            int order = statusOrder((String) a.get("status")) - statusOrder((String) b.get("status"));
            if (order != 0) return order;
            return Double.compare((Double) b.get("deficit"), (Double) a.get("deficit"));
        });

        result.put("materials", materials);
        result.put("totalMaterials", materials.size());
        result.put("shortageCount", shortageCount);
        result.put("noneCount", noneCount);
        result.put("totalDeficit", Math.round(totalDeficit * 100.0) / 100.0);
        result.put("totalSeries", plannedSeries.size());
        result.put("scope", allZones ? "ALL_ZONES" : "ZONE");

        return result;
    }

    /**
     * Load planned series for a zone that are:
     * 1. Waiting to be spread (statusMatelassage = 'Waiting')
     * 2. Not finished cutting (statusCoupe != 'Complete')
     * 3. Will start cutting within the horizon (dateDebutCoupe is null or <= horizonEnd)
     */
    private List<SerieDTO> loadPlannedSeriesForZone(String zoneName, LocalDateTime now, LocalDateTime horizonEnd) {
        List<SerieDTO> scheduledSeries = loadScheduledSeriesForForecast(zoneName, horizonEnd);
        if (!scheduledSeries.isEmpty()) {
            return filterForecastSeries(scheduledSeries, horizonEnd);
        }

        // Step 1: Get active sequences dispatched to this zone
        Map<String, String> seqToDispatchedZone = new LinkedHashMap<>();
        for (Object[] r : cuttingRequestRepository.findAllActiveLight()) {
            String seq = (String) r[0];
            String dispZone = (String) r[1];
            if (seq != null) {
                seqToDispatchedZone.put(seq, dispZone);
            }
        }

        // Collect sequences belonging to this zone (dispatched or preferred)
        Set<String> zoneSequences = new HashSet<>();
        for (Map.Entry<String, String> e : seqToDispatchedZone.entrySet()) {
            if (zoneName == null || zoneName.equalsIgnoreCase(e.getValue())) {
                zoneSequences.add(e.getKey());
            }
        }

        // Step 2: Load all series for relevant sequences
        List<SerieDTO> allSeries = new ArrayList<>();
        if (!zoneSequences.isEmpty()) {
            List<String> seqList = new ArrayList<>(zoneSequences);
            for (int i = 0; i < seqList.size(); i += SQL_BATCH_SIZE) {
                List<String> batch = seqList.subList(i, Math.min(i + SQL_BATCH_SIZE, seqList.size()));
                for (Object[] row : serieDataRepository.findSeriesBySequencesLight(batch)) {
                    allSeries.add(SerieDTO.from(row));
                }
            }
        }

        // Also load series where zoneCoupe or zoneMatelassage directly matches
        // (covers SHARED zone series and explicit table assignments)
        if (zoneName != null) {
            List<Object[]> directZoneSeries = serieDataRepository.findSeriesByZoneCoupeOrMatelassage(zoneName);
            Set<String> alreadyLoaded = allSeries.stream().map(s -> s.serie).collect(Collectors.toSet());
            for (Object[] row : directZoneSeries) {
                SerieDTO s = SerieDTO.from(row);
                if (!alreadyLoaded.contains(s.serie)) {
                    allSeries.add(s);
                }
            }
        }

        // Step 3: Filter for waiting + upcoming cut start
        return filterForecastSeries(allSeries, horizonEnd);
    }

    private List<SerieDTO> loadScheduledSeriesForForecast(String zoneName, LocalDateTime horizonEnd) {
        List<EngineScheduleEntry> entries = scheduleEntryRepository.findCoupeEntriesForForecast(zoneName, horizonEnd);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, EngineScheduleEntry> bySerie = new LinkedHashMap<>();
        for (EngineScheduleEntry entry : entries) {
            if (entry.getId() == null || entry.getId().getSerieId() == null) continue;
            bySerie.putIfAbsent(entry.getId().getSerieId(), entry);
        }

        List<SerieDTO> out = new ArrayList<>();
        List<String> serieIds = new ArrayList<>(bySerie.keySet());
        for (int i = 0; i < serieIds.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = serieIds.subList(i, Math.min(i + SQL_BATCH_SIZE, serieIds.size()));
            for (CuttingRequestSerieData row : serieDataRepository.findSeries(batch)) {
                SerieDTO dto = fromEntity(row);
                EngineScheduleEntry entry = bySerie.get(dto.serie);
                if (entry != null) {
                    dto.dateDebutCoupe = entry.getPlannedStart();
                    dto.dateFinCoupe = entry.getPlannedEnd();
                    dto.zoneCoupe = entry.getZoneNom();
                    dto.tableCoupe = entry.getMachineNom();
                }
                out.add(dto);
            }
        }
        return out;
    }

    private List<SerieDTO> filterForecastSeries(List<SerieDTO> series, LocalDateTime horizonEnd) {
        return series.stream()
                .filter(s -> "Waiting".equals(s.statusMatelassage))           // not spread yet
                .filter(s -> !"Complete".equals(s.statusCoupe))               // not finished cutting
                .filter(s -> {
                    // Include if no cut start date yet (upcoming)
                    if (s.dateDebutCoupe == null) return true;
                    // Include if cut start is within horizon (or already started but not finished)
                    return !s.dateDebutCoupe.isAfter(horizonEnd);
                })
                .collect(Collectors.toList());
    }

    private SerieDTO fromEntity(CuttingRequestSerieData row) {
        SerieDTO d = new SerieDTO();
        d.serie = row.getSerie();
        d.sequence = row.getSequence();
        d.machine = row.getMachine();
        d.partNumberMaterial = row.getPartNumberMaterial();
        d.description = row.getDescription();
        d.longueur = row.getLongueur();
        d.nbrCouche = row.getNbrCouche();
        d.placement = row.getPlacement();
        d.tempsDeCoupe = row.getTempsDeCoupe();
        d.tableCoupe = row.getTableCoupe();
        d.tableMatelassage = row.getTableMatelassage();
        d.statusCoupe = row.getStatusCoupe();
        d.statusMatelassage = row.getStatusMatelassage();
        d.dateDebutCoupe = row.getDateDebutCoupe();
        d.dateFinCoupe = row.getDateFinCoupe();
        d.dateDebutMatelassage = row.getDateDebutMatelassage();
        d.dateFinMatelassage = row.getDateFinMatelassage();
        d.zoneCoupe = row.getZoneCoupe();
        d.zoneMatelassage = row.getZoneMatelassage();
        d.planningDate = row.getPlanningDate();
        return d;
    }

    /**
     * Load roll stock grouped by material for a list of locations.
     */
    private Map<String, RollStock> loadStockByLocations(List<String> locations) {
        Map<String, RollStock> stockMap = new LinkedHashMap<>();
        if (locations.isEmpty()) return stockMap;

        List<ScanRouleau> rolls = scanRouleauRepository.findByLocations(locations);
        for (ScanRouleau roll : rolls) {
            String material = stripPrefix(roll.getReftissu(), "P");
            if (material == null || material.isEmpty()) continue;
            RollStock stock = stockMap.computeIfAbsent(material, k -> new RollStock(k));
            stock.totalMetrage += (roll.getMetrage() != null ? roll.getMetrage() : 0);
            stock.rollCount++;
            stock.locations.add(roll.getEmplacement());
        }
        return stockMap;
    }

    private Map<String, RollStock> loadStockForScope(Zone zone) {
        if (zone != null) {
            return loadStockByLocations(parseLocations(zone.getRollLocations()));
        }

        Set<String> allZoneLocations = new LinkedHashSet<>();
        for (Zone z : StreamSupport.stream(zoneRepository.findAll().spliterator(), false)
                .collect(Collectors.toList())) {
            allZoneLocations.addAll(parseLocations(z.getRollLocations()));
        }
        return loadStockByLocations(new ArrayList<>(allZoneLocations));
    }

    private Map<String, RollStock> loadAllStockByMaterial() {
        Map<String, RollStock> stockMap = new LinkedHashMap<>();
        for (ScanRouleau roll : scanRouleauRepository.findAll()) {
            String material = stripPrefix(roll.getReftissu(), "P");
            if (material == null || material.isEmpty()) continue;
            RollStock stock = stockMap.computeIfAbsent(material, k -> new RollStock(k));
            stock.totalMetrage += (roll.getMetrage() != null ? roll.getMetrage() : 0);
            stock.rollCount++;
            stock.locations.add(roll.getEmplacement());
        }
        return stockMap;
    }

    /**
     * Find all places where a material exists in production (all zones + all locations).
     * Returns a rich suggestion map with transfer options and total available.
     */
    private Map<String, Object> findAllTransferOptions(String currentZone, String material, double deficit) {
        // Load ALL rolls for this material across all production
        List<ScanRouleau> allRolls = scanRouleauRepository.findAll();
        Map<String, List<ScanRouleau>> rollsByLocation = new LinkedHashMap<>();
        double totalAvailable = 0;

        for (ScanRouleau roll : allRolls) {
            String mat = stripPrefix(roll.getReftissu(), "P");
            if (material.equals(mat)) {
                String loc = roll.getEmplacement() != null ? roll.getEmplacement() : "UNKNOWN";
                rollsByLocation.computeIfAbsent(loc, k -> new ArrayList<>()).add(roll);
                totalAvailable += (roll.getMetrage() != null ? roll.getMetrage() : 0);
            }
        }

        // Group by zone (based on zone roll locations)
        List<Zone> allZones = StreamSupport.stream(zoneRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        List<Map<String, Object>> zoneOptions = new ArrayList<>();
        for (Zone z : allZones) {
            if (z.getNom().equals(currentZone)) continue;
            List<String> zLocs = parseLocations(z.getRollLocations());
            double zoneAvailable = 0;
            int zoneRollCount = 0;
            for (String loc : zLocs) {
                List<ScanRouleau> rolls = rollsByLocation.get(loc);
                if (rolls != null) {
                    for (ScanRouleau r : rolls) {
                        zoneAvailable += (r.getMetrage() != null ? r.getMetrage() : 0);
                        zoneRollCount++;
                    }
                }
            }
            if (zoneAvailable > 0) {
                Map<String, Object> opt = new LinkedHashMap<>();
                opt.put("zone", z.getNom());
                opt.put("availableMetrage", Math.round(zoneAvailable * 100.0) / 100.0);
                opt.put("rollCount", zoneRollCount);
                zoneOptions.add(opt);
            }
        }

        // Sort by available quantity descending
        zoneOptions.sort((a, b) -> Double.compare((Double) b.get("availableMetrage"), (Double) a.get("availableMetrage")));

        Map<String, Object> result = new LinkedHashMap<>();
        if (!zoneOptions.isEmpty()) {
            Map<String, Object> best = zoneOptions.get(0);
            double bestAvailable = (Double) best.get("availableMetrage");
            if (bestAvailable >= deficit) {
                result.put("type", "TRANSFER");
                result.put("fromZone", best.get("zone"));
                result.put("availableMetrage", bestAvailable);
                result.put("rollCount", best.get("rollCount"));
                result.put("allOptions", zoneOptions);
            } else {
                result.put("type", "PARTIAL");
                result.put("message", "Stock insuffisant partout — transfert partiel possible");
                result.put("totalAvailableElsewhere", Math.round(totalAvailable * 100.0) / 100.0);
                result.put("allOptions", zoneOptions);
            }
        } else if (totalAvailable > 0) {
            result.put("type", "OTHER_LOCATION");
            result.put("message", "Matière en production mais hors zones connues");
            result.put("totalAvailableElsewhere", Math.round(totalAvailable * 100.0) / 100.0);
        } else {
            result.put("type", "CENTRAL_STOCK");
            result.put("message", "Demander au stock central");
        }
        return result;
    }

    private Map<String, Object> buildAllZonesSuggestion(String material, double deficit, double availableOutOfZone) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (deficit <= 0 && availableOutOfZone > 0) {
            result.put("type", "OUT_OF_ZONE");
            result.put("message", "Matière disponible hors rack zone sélectionné");
            result.put("availableOutOfZone", Math.round(availableOutOfZone * 100.0) / 100.0);
        } else {
            result.put("type", "CENTRAL_STOCK");
            result.put("message", "Demander au stock central");
            result.put("material", material);
            result.put("deficit", Math.round(deficit * 100.0) / 100.0);
        }
        return result;
    }

    /**
     * Defer all series of a specific material in a zone: set statusMatelassage to "Incomplete".
     */
    public Map<String, Object> deferMaterialSeries(String zoneName, String material) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizonEnd = now.plusHours(24); // generous horizon for deferral
        List<SerieDTO> series = loadPlannedSeriesForZone(zoneName, now, horizonEnd);

        String strippedMaterial = stripPrefix(material, "P");
        List<String> deferred = new ArrayList<>();

        for (SerieDTO s : series) {
            String mat = stripPrefix(s.partNumberMaterial, "P");
            if (strippedMaterial.equals(mat) && "Waiting".equals(s.statusMatelassage)) {
                var serieEntity = serieDataRepository.findBySerie(s.serie);
                if (serieEntity != null) {
                    serieEntity.setStatusMatelassage("Incomplete");
                    serieDataRepository.save(serieEntity);
                    deferred.add(s.serie);
                }
            }
        }

        result.put("success", true);
        result.put("material", material);
        result.put("deferredCount", deferred.size());
        result.put("deferredSeries", deferred);
        afterProductionDataChange();
        return result;
    }

    /**
     * Change sequence status. Allowed values are the new lifecycle vocabulary:
     * IMPORTED, RELEASED, STARTED, COMPLETED, MATERIAL_MISSING, INCOMPLETE
     * (see {@link com.lear.MGCMS.domain.CuttingRequest.SequenceStatus}).
     */
    public Map<String, Object> changeSequenceStatus(String sequenceId, String newStatus) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> allowed = Arrays.asList(
                SequenceStatus.IMPORTED, SequenceStatus.RELEASED, SequenceStatus.STARTED,
                SequenceStatus.COMPLETED, SequenceStatus.MATERIAL_MISSING, SequenceStatus.INCOMPLETE);
        if (!allowed.contains(newStatus)) {
            result.put("success", false);
            result.put("error", "Statut invalide: " + newStatus);
            return result;
        }

        var request = requestDataRepository.findBySequence(sequenceId);
        if (request == null) {
            result.put("success", false);
            result.put("error", "Séquence non trouvée: " + sequenceId);
            return result;
        }

        String oldStatus = request.getSequenceStatus();
        request.setSequenceStatus(newStatus);
        requestDataRepository.save(request);

        result.put("success", true);
        result.put("sequence", sequenceId);
        result.put("oldStatus", oldStatus);
        result.put("newStatus", newStatus);
        afterProductionDataChange();
        return result;
    }

    private void afterProductionDataChange() {
        try {
            if (serieStatusDateValidator != null) {
                serieStatusDateValidator.normalizeProductionProgress();
            }
            if (ordonnancementService != null) {
                ordonnancementService.invalidateTimelineCache();
            }
            if (workbenchCacheService != null) {
                workbenchCacheService.invalidateAll();
            }
            if (optimizerService != null) {
                optimizerService.reloadActiveSnapshotFromGroundTruth();
            }
        } catch (Exception ignored) {
            // Derived views refresh on the next poll if this eager refresh fails.
        }
    }

    // ======================== UTILITY METHODS ========================

    private String stripPrefix(String value, String prefix) {
        if (value == null) return null;
        value = value.trim();
        if (value.startsWith(prefix)) return value.substring(prefix.length());
        return value;
    }

    private boolean isAllZones(String zoneName) {
        return zoneName == null
                || zoneName.trim().isEmpty()
                || "ALL".equalsIgnoreCase(zoneName.trim())
                || "All".equalsIgnoreCase(zoneName.trim())
                || "Toutes".equalsIgnoreCase(zoneName.trim());
    }

    private List<String> parseLocations(String rollLocations) {
        if (rollLocations == null || rollLocations.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(rollLocations.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private int statusOrder(String status) {
        if ("NONE".equals(status)) return 0;
        if ("SHORTAGE".equals(status)) return 1;
        if ("OUT_OF_ZONE".equals(status)) return 2;
        if ("OK".equals(status)) return 3;
        if ("RETURN_TO_STOCK".equals(status)) return 4;
        return 5;
    }

    // ======================== INNER CLASSES ========================

    private static class MaterialDemand {
        String material;
        double totalMetrageNeeded;
        int serieCount;
        List<String> serieIds = new ArrayList<>();
        Set<String> sequences = new HashSet<>();

        MaterialDemand(String material) {
            this.material = material;
        }
    }

    private static class RollStock {
        String material;
        double totalMetrage;
        int rollCount;
        Set<String> locations = new HashSet<>();

        RollStock(String material) {
            this.material = material;
        }
    }
}
