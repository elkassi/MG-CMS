package com.lear.MGCMS.services.dispatcher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.domain.dispatcher.EngineScheduleEntry;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.dispatcher.EngineScheduleEntryRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.logistics.AllocationService;

/**
 * Builds the lightweight sequence-focus section embedded in the Process
 * Workbench cache.
 *
 * <p>This is intentionally not an optimizer. It reads the already-balanced
 * live-charge/schedule state and turns it into small chef/logistics actions:
 * finish open sequences, prepare boxes for the next scheduled sequence, and
 * move scanned rolls for the next two hours.</p>
 */
@Service
public class WorkbenchSequenceFocusService {

    private static final int HORIZON_MINUTES = 120;
    private static final int SQL_BATCH_SIZE = 1000;
    private static final int MAX_FOCUS_PER_ZONE = 8;
    private static final int MAX_ALERTS_PER_ZONE = 5;
    private static final int MAX_MATERIALS_PER_ZONE = 20;
    private static final int MAX_ACTIONS_PER_ZONE = 8;
    private static final int JUST_ADDED_MINUTES = 20;

    @Autowired private EngineScheduleEntryRepository scheduleEntryRepository;
    @Autowired private CuttingRequestBoxInfoRepository boxInfoRepository;
    @Autowired private ZoneRepository zoneRepository;
    @Autowired private ProductionTableRepository productionTableRepository;
    @Autowired(required = false) private AllocationService allocationService;
    @Autowired(required = false) private SerieRouleauTempService serieRouleauTempService;

    public Map<String, Object> build(LocalDate date,
                                     int shift,
                                     LiveChargeDto liveCharge,
                                     List<Map<String, Object>> stockRacks,
                                     EngineState engineState) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizonEnd = now.plusMinutes(HORIZON_MINUTES);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", now.toString());
        out.put("horizonMinutes", HORIZON_MINUTES);
        out.put("date", date != null ? date.toString() : null);
        out.put("shift", shift);
        out.put("engineState", engineState != null ? engineState.name() : null);
        out.put("dispatchBalanced", isDispatchBalanced(engineState));

        if (liveCharge == null || liveCharge.getZones() == null) {
            out.put("zones", Collections.emptyList());
            out.put("totals", totals(0, 0, 0, 0, 0, 0));
            return out;
        }

        Map<String, SequenceCarrier> sequences = flattenSequences(liveCharge);
        Map<String, List<EngineScheduleEntry>> scheduleBySequence =
                loadScheduleBySequence(new ArrayList<>(sequences.keySet()), horizonEnd);
        Set<String> scheduledSerieIds = scheduleBySequence.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.getId() != null && e.getId().getSerieId() != null)
                .map(e -> e.getId().getSerieId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Integer> boxCounts = loadBoxCounts(new ArrayList<>(sequences.keySet()));

        StockIndex stockIndex = buildStockIndex(stockRacks);
        DemandIndex demandIndex = buildDemandIndex(sequences, scheduledSerieIds, stockIndex);
        Map<String, Integer> configuredMachinesByZone = loadConfiguredMachineCounts(liveCharge.getZones());

        List<Map<String, Object>> zoneReports = new ArrayList<>();
        int totalOpen = 0;
        int totalReady = 0;
        int totalMaterialIssues = 0;
        int totalOccupiedBoxes = 0;
        int totalBoxCapacity = 0;

        for (LiveChargeDto.ZoneChargeDto zone : liveCharge.getZones()) {
            if (zone == null || zone.getZoneNom() == null) continue;
            List<SequenceFocus> seqFocus = new ArrayList<>();
            List<LiveChargeDto.SequenceDto> zoneSequences = new ArrayList<>();
            if (zone.getLockedSequences() != null) zoneSequences.addAll(zone.getLockedSequences());
            if (zone.getPendingSequences() != null) zoneSequences.addAll(zone.getPendingSequences());

            for (LiveChargeDto.SequenceDto seq : zoneSequences) {
                if (seq == null || seq.getSequence() == null) continue;
                ScheduleSummary schedule = summarizeSchedule(scheduleBySequence.get(seq.getSequence()), now);
                SequenceFocus focus = buildSequenceFocus(
                        seq, zone.getZoneNom(), schedule, boxCounts.get(seq.getSequence()),
                        stockIndex, scheduledSerieIds, date);
                seqFocus.add(focus);
            }

            seqFocus.sort(Comparator
                    .comparingDouble((SequenceFocus s) -> s.focusScore).reversed()
                    .thenComparing(s -> s.sequence != null ? s.sequence : ""));

            List<Map<String, Object>> focusRows = seqFocus.stream()
                    .filter(s -> !"BACKLOG".equals(s.state) || s.opened)
                    .limit(MAX_FOCUS_PER_ZONE)
                    .map(SequenceFocus::toMap)
                    .collect(Collectors.toList());
            List<Map<String, Object>> chefAlerts = seqFocus.stream()
                    .filter(s -> "READY_TO_START".equals(s.state) || "JUST_ADDED".equals(s.state))
                    .sorted(Comparator.comparingLong(s -> s.minutesToStart == null ? Long.MAX_VALUE : s.minutesToStart))
                    .limit(MAX_ALERTS_PER_ZONE)
                    .map(SequenceFocus::toMap)
                    .collect(Collectors.toList());

            MaterialZoneReport materialReport = buildMaterialReport(zone.getZoneNom(), demandIndex, stockIndex);
            BoxOccupancy boxOccupancy = buildBoxOccupancy(zone, seqFocus,
                    configuredMachinesByZone.getOrDefault(zone.getZoneNom(), 0));

            int openCount = (int) seqFocus.stream().filter(s -> "OPEN".equals(s.state)).count();
            int readyCount = (int) seqFocus.stream()
                    .filter(s -> "READY_TO_START".equals(s.state) || "JUST_ADDED".equals(s.state))
                    .count();
            totalOpen += openCount;
            totalReady += readyCount;
            totalMaterialIssues += materialReport.issueCount;
            totalOccupiedBoxes += boxOccupancy.occupiedBoxes;
            totalBoxCapacity += boxOccupancy.maxBoxes;

            Map<String, Object> zoneMap = new LinkedHashMap<>();
            zoneMap.put("zone", zone.getZoneNom());
            zoneMap.put("category", zone.getCategory());
            zoneMap.put("openSequenceCount", openCount);
            zoneMap.put("aboutToStartCount", readyCount);
            zoneMap.put("materialIssueCount", materialReport.issueCount);
            zoneMap.put("boxOccupancy", boxOccupancy.toMap());
            zoneMap.put("focusSequences", focusRows);
            zoneMap.put("chefAlerts", chefAlerts);
            zoneMap.put("logistics", materialReport.toMap());
            zoneReports.add(zoneMap);
        }

        out.put("zones", zoneReports);
        out.put("totals", totals(zoneReports.size(), totalOpen, totalReady, totalMaterialIssues,
                totalOccupiedBoxes, totalBoxCapacity));
        return out;
    }

    @SuppressWarnings("unchecked")
    public void updateEngineState(Object sequenceFocus, Map<String, Object> engineState) {
        if (!(sequenceFocus instanceof Map) || engineState == null) return;
        Object stateObj = engineState.get("state");
        String state = stateObj != null ? String.valueOf(stateObj) : null;
        Map<String, Object> focus = (Map<String, Object>) sequenceFocus;
        focus.put("engineState", state);
        focus.put("dispatchBalanced", isDispatchBalanced(state));
        if (engineState.get("phase") != null) {
            focus.put("enginePhase", engineState.get("phase"));
        }
    }

    private boolean isDispatchBalanced(EngineState state) {
        return state != EngineState.WARMING && state != EngineState.IMPROVING;
    }

    private boolean isDispatchBalanced(String state) {
        return !"WARMING".equalsIgnoreCase(state) && !"IMPROVING".equalsIgnoreCase(state);
    }

    private Map<String, Object> totals(int zones, int open, int ready, int materialIssues,
                                       int occupiedBoxes, int boxCapacity) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("zoneCount", zones);
        t.put("openSequenceCount", open);
        t.put("aboutToStartCount", ready);
        t.put("materialIssueCount", materialIssues);
        t.put("occupiedBoxes", occupiedBoxes);
        t.put("boxCapacity", boxCapacity);
        t.put("boxOccupancyPct", boxCapacity > 0 ? round2((occupiedBoxes * 100.0) / boxCapacity) : 0.0);
        return t;
    }

    private Map<String, SequenceCarrier> flattenSequences(LiveChargeDto liveCharge) {
        Map<String, SequenceCarrier> out = new LinkedHashMap<>();
        for (LiveChargeDto.ZoneChargeDto zone : liveCharge.getZones()) {
            if (zone == null) continue;
            putSequences(out, zone.getZoneNom(), zone.getLockedSequences());
            putSequences(out, zone.getZoneNom(), zone.getPendingSequences());
        }
        putSequences(out, null, liveCharge.getUnassigned());
        return out;
    }

    private void putSequences(Map<String, SequenceCarrier> out,
                              String ownerZone,
                              List<LiveChargeDto.SequenceDto> sequences) {
        if (sequences == null) return;
        for (LiveChargeDto.SequenceDto seq : sequences) {
            if (seq != null && seq.getSequence() != null) {
                out.put(seq.getSequence(), new SequenceCarrier(seq, ownerZone));
            }
        }
    }

    private Map<String, List<EngineScheduleEntry>> loadScheduleBySequence(List<String> sequences,
                                                                          LocalDateTime horizonEnd) {
        Map<String, List<EngineScheduleEntry>> out = new LinkedHashMap<>();
        if (sequences == null || sequences.isEmpty()) return out;
        for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
            for (EngineScheduleEntry entry :
                    scheduleEntryRepository.findCoupeEntriesForSequencesWithinHorizon(batch, horizonEnd)) {
                if (entry.getSequenceId() == null) continue;
                out.computeIfAbsent(entry.getSequenceId(), k -> new ArrayList<>()).add(entry);
            }
        }
        return out;
    }

    private Map<String, Integer> loadBoxCounts(List<String> sequences) {
        Map<String, Integer> out = new HashMap<>();
        if (sequences == null || sequences.isEmpty()) return out;
        for (int i = 0; i < sequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + SQL_BATCH_SIZE, sequences.size()));
            for (Object[] row : boxInfoRepository.countBoxesBySequences(batch)) {
                String seq = row[0] != null ? String.valueOf(row[0]) : null;
                Integer count = row[1] != null ? ((Number) row[1]).intValue() : 0;
                if (seq != null) out.put(seq, count);
            }
        }
        return out;
    }

    private ScheduleSummary summarizeSchedule(List<EngineScheduleEntry> entries, LocalDateTime now) {
        ScheduleSummary summary = new ScheduleSummary();
        if (entries == null) return summary;
        for (EngineScheduleEntry e : entries) {
            if (e.getPlannedStart() != null
                    && (summary.firstStart == null || e.getPlannedStart().isBefore(summary.firstStart))) {
                summary.firstStart = e.getPlannedStart();
            }
            if (e.getPlannedEnd() != null
                    && (summary.lastEnd == null || e.getPlannedEnd().isAfter(summary.lastEnd))) {
                summary.lastEnd = e.getPlannedEnd();
            }
            if (e.getPlannedAt() != null
                    && (summary.lastPlannedAt == null || e.getPlannedAt().isAfter(summary.lastPlannedAt))) {
                summary.lastPlannedAt = e.getPlannedAt();
            }
            if (e.getMachineNom() != null) {
                summary.machines.add(e.getMachineNom());
            }
        }
        if (summary.firstStart != null) {
            summary.minutesToStart = Duration.between(now, summary.firstStart).toMinutes();
        }
        return summary;
    }

    private SequenceFocus buildSequenceFocus(LiveChargeDto.SequenceDto seq,
                                             String zone,
                                             ScheduleSummary schedule,
                                             Integer boxCount,
                                             StockIndex stockIndex,
                                             Set<String> scheduledSerieIds,
                                             LocalDate workbenchDate) {
        int totalSeries = seq.getSeries() != null ? seq.getSeries().size() : 0;
        int remainingSeries = 0;
        boolean opened = false;
        for (LiveChargeDto.SerieDto serie : safeSeries(seq)) {
            if (!isComplete(serie.getStatusCoupe())) remainingSeries++;
            if (hasStarted(serie)) opened = true;
        }

        long predictedCloseMinutes = predictCloseMinutes(seq, schedule);
        String materialStatus = sequenceMaterialStatus(seq, zone, stockIndex, scheduledSerieIds);
        boolean materialMissingSuggested = sequenceMaterialMissingSuggested(seq, zone, stockIndex, scheduledSerieIds);
        String state;
        if (opened && remainingSeries > 0) {
            state = "OPEN";
        } else if (schedule.lastPlannedAt != null
                && !schedule.lastPlannedAt.isBefore(LocalDateTime.now().minusMinutes(JUST_ADDED_MINUTES))) {
            state = "JUST_ADDED";
        } else if (schedule.firstStart != null) {
            state = "READY_TO_START";
        } else {
            state = "BACKLOG";
        }

        String action;
        if ("OPEN".equals(state)) action = "FINISH_SEQUENCE";
        else if ("JUST_ADDED".equals(state) || "READY_TO_START".equals(state)) action = "PREPARE_BOXES";
        else action = "WATCH";

        SequenceFocus focus = new SequenceFocus();
        focus.sequence = seq.getSequence();
        focus.zone = zone;
        focus.state = state;
        focus.action = action;
        focus.opened = opened;
        focus.boxCount = boxCount != null ? boxCount : 0;
        focus.remainingSeries = remainingSeries;
        focus.totalSeries = totalSeries;
        focus.totalRemainingMinutes = round2(seq.getTotalRemainingMinutes());
        focus.predictedCloseMinutes = predictedCloseMinutes;
        focus.firstStart = schedule.firstStart;
        focus.lastEnd = schedule.lastEnd;
        focus.minutesToStart = schedule.minutesToStart;
        focus.machines = new ArrayList<>(schedule.machines);
        focus.dueDate = seq.getDueDate();
        focus.zoneSource = seq.getZoneSource();
        focus.zoneAcceptanceStatus = seq.getZoneAcceptanceStatus();
        focus.locked = seq.isLocked();
        focus.materialStatus = materialStatus;
        focus.materialMissingSuggested = materialMissingSuggested;
        focus.focusScore = round2(score(focus, workbenchDate));
        return focus;
    }

    private BoxOccupancy buildBoxOccupancy(LiveChargeDto.ZoneChargeDto zone,
                                           List<SequenceFocus> sequences,
                                           int configuredMachines) {
        BoxOccupancy occupancy = new BoxOccupancy();
        occupancy.activeMachines = activeMachineCount(zone);
        occupancy.configuredMachines = Math.max(0, configuredMachines);
        occupancy.maxBoxes = occupancy.configuredMachines * 16;
        if (sequences == null) return occupancy;

        sequences.stream()
                .filter(s -> s.opened && s.remainingSeries > 0)
                .sorted(Comparator
                        .comparingDouble((SequenceFocus s) -> s.focusScore).reversed()
                        .thenComparing(s -> s.sequence != null ? s.sequence : ""))
                .forEach(s -> {
                    occupancy.occupiedBoxes += s.boxCount;
                    occupancy.occupiedSequences.add(s.toMap());
                });
        occupancy.occupancyPct = occupancy.maxBoxes > 0
                ? round2((occupancy.occupiedBoxes * 100.0) / occupancy.maxBoxes)
                : 0.0;
        return occupancy;
    }

    private Map<String, Integer> loadConfiguredMachineCounts(List<LiveChargeDto.ZoneChargeDto> zones) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (zones == null || zones.isEmpty()) return out;
        List<String> zoneNames = zones.stream()
                .filter(z -> z != null && z.getZoneNom() != null)
                .map(LiveChargeDto.ZoneChargeDto::getZoneNom)
                .distinct()
                .collect(Collectors.toList());
        if (zoneNames.isEmpty()) return out;
        Map<String, Set<String>> machinesByZone = new LinkedHashMap<>();
        for (Object[] row : productionTableRepository.findMachinesWithTypeInZones(zoneNames)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
            machinesByZone.computeIfAbsent(String.valueOf(row[0]), z -> new LinkedHashSet<>())
                    .add(String.valueOf(row[1]));
        }
        for (String zone : zoneNames) {
            out.put(zone, machinesByZone.getOrDefault(zone, Collections.emptySet()).size());
        }
        return out;
    }

    private int activeMachineCount(LiveChargeDto.ZoneChargeDto zone) {
        if (zone == null || zone.getByMachineType() == null) return 0;
        int count = 0;
        for (LiveChargeDto.MachineTypeChargeDto mt : zone.getByMachineType()) {
            if (mt != null && mt.getActiveMachines() > 0) {
                count += mt.getActiveMachines();
            }
        }
        return count;
    }

    private List<LiveChargeDto.SerieDto> safeSeries(LiveChargeDto.SequenceDto seq) {
        return seq.getSeries() == null ? Collections.emptyList() : seq.getSeries();
    }

    private boolean hasStarted(LiveChargeDto.SerieDto serie) {
        if (serie == null) return false;
        if (startedStatus(serie.getStatusCoupe()) || startedStatus(serie.getStatusMatelassage())) return true;
        return serie.getDateDebutCoupe() != null
                || serie.getDateFinCoupe() != null
                || serie.getDateDebutMatelassage() != null
                || serie.getDateFinMatelassage() != null;
    }

    private boolean startedStatus(String status) {
        if (status == null) return false;
        String s = status.trim();
        return !s.isEmpty() && !"Waiting".equalsIgnoreCase(s);
    }

    private boolean isComplete(String status) {
        return status != null && "Complete".equalsIgnoreCase(status.trim());
    }

    private long predictCloseMinutes(LiveChargeDto.SequenceDto seq, ScheduleSummary schedule) {
        LocalDateTime now = LocalDateTime.now();
        if (schedule.lastEnd != null && schedule.lastEnd.isAfter(now)) {
            return Math.max(1, Duration.between(now, schedule.lastEnd).toMinutes());
        }
        double floor = seq.getBoxCycleTimeMinutes() > 0
                ? seq.getBoxCycleTimeMinutes()
                : Math.max(1.0, seq.getTotalRemainingMinutes());
        return Math.max(1, Math.round(floor));
    }

    private double score(SequenceFocus focus, LocalDate workbenchDate) {
        double score = 0.0;
        if ("OPEN".equals(focus.state)) score += 1000.0;
        if ("JUST_ADDED".equals(focus.state)) score += 220.0;
        if ("READY_TO_START".equals(focus.state)) score += 180.0;

        int boxesForScore = Math.max(1, focus.boxCount);
        score += (boxesForScore * 120.0) / Math.max(1.0, focus.predictedCloseMinutes);

        if (focus.dueDate != null && workbenchDate != null) {
            long age = ChronoUnit.DAYS.between(focus.dueDate, workbenchDate);
            score += Math.max(0, age) * 25.0;
        }
        if (focus.minutesToStart != null) {
            score += Math.max(0.0, 120.0 - Math.max(0, focus.minutesToStart)) / 4.0;
        }
        score -= materialPenalty(focus.materialStatus);
        return score;
    }

    private double materialPenalty(String status) {
        if ("NONE".equals(status)) return 120.0;
        if ("SHORTAGE".equals(status)) return 100.0;
        if ("OUT_OF_ZONE".equals(status)) return 55.0;
        return 0.0;
    }

    private String sequenceMaterialStatus(LiveChargeDto.SequenceDto seq,
                                          String zone,
                                          StockIndex stockIndex,
                                          Set<String> scheduledSerieIds) {
        String worst = "OK";
        for (LiveChargeDto.SerieDto serie : safeSeries(seq)) {
            if (isComplete(serie.getStatusCoupe())) continue;
            if (!scheduledSerieIds.isEmpty() && !scheduledSerieIds.contains(serie.getSerie()) && !hasStarted(serie)) {
                continue;
            }
            String material = normalizeMaterial(serie.getRefTissus());
            if (material == null) continue;
            double needed = Math.max(serie.getTableLengthRequired(), 0.0);
            if (needed <= 0) continue;
            String targetZone = serie.getTargetZoneNom() != null ? serie.getTargetZoneNom() : zone;
            double inZone = stockIndex.meters(material, targetZone);
            double total = stockIndex.totalMeters(material);
            String status = materialStatus(needed, inZone, total);
            if (statusOrder(status) < statusOrder(worst)) {
                worst = status;
            }
        }
        return worst;
    }

    private boolean sequenceMaterialMissingSuggested(LiveChargeDto.SequenceDto seq,
                                                     String zone,
                                                     StockIndex stockIndex,
                                                     Set<String> scheduledSerieIds) {
        boolean anyDemand = false;
        boolean anySerieSpreadable = false;
        for (LiveChargeDto.SerieDto serie : safeSeries(seq)) {
            if (isComplete(serie.getStatusCoupe())) continue;
            if (!scheduledSerieIds.isEmpty() && !scheduledSerieIds.contains(serie.getSerie()) && !hasStarted(serie)) {
                continue;
            }
            String material = normalizeMaterial(serie.getRefTissus());
            if (material == null) continue;
            double needed = Math.max(serie.getTableLengthRequired(), 0.0);
            if (needed <= 0) continue;
            anyDemand = true;
            if (stockIndex.totalMeters(material) >= needed) {
                anySerieSpreadable = true;
            }
        }
        return anyDemand && !anySerieSpreadable;
    }

    private DemandIndex buildDemandIndex(Map<String, SequenceCarrier> sequences,
                                         Set<String> scheduledSerieIds,
                                         StockIndex stockIndex) {
        DemandIndex out = new DemandIndex();
        for (SequenceCarrier carrier : sequences.values()) {
            LiveChargeDto.SequenceDto seq = carrier.sequence;
            String ownerZone = carrier.ownerZone;
            for (LiveChargeDto.SerieDto serie : safeSeries(seq)) {
                if (isComplete(serie.getStatusCoupe())) continue;
                if (!scheduledSerieIds.isEmpty() && !scheduledSerieIds.contains(serie.getSerie()) && !hasStarted(serie)) {
                    continue;
                }
                String material = normalizeMaterial(serie.getRefTissus());
                if (material == null) continue;
                double needed = Math.max(0.0, serie.getTableLengthRequired());
                if (needed <= 0) continue;
                String zone = serie.getTargetZoneNom() != null ? serie.getTargetZoneNom() : ownerZone;
                if (zone == null) zone = seq.getEffectiveZone();
                if (zone == null) continue;
                MaterialDemand demand = out.get(zone, material);
                demand.needed += needed;
                demand.series.add(serie.getSerie());
                demand.sequences.add(seq.getSequence());
            }
        }
        return out;
    }

    private MaterialZoneReport buildMaterialReport(String zone,
                                                   DemandIndex demandIndex,
                                                   StockIndex stockIndex) {
        MaterialZoneReport report = new MaterialZoneReport(zone);

        for (MaterialDemand demand : demandIndex.demandsForZone(zone)) {
            double inZone = stockIndex.meters(demand.material, zone);
            double total = stockIndex.totalMeters(demand.material);
            String status = materialStatus(demand.needed, inZone, total);
            MaterialRow row = new MaterialRow();
            row.material = demand.material;
            row.needed = round2(demand.needed);
            row.availableInZone = round2(inZone);
            row.availableTotal = round2(total);
            row.availableOutOfZone = round2(Math.max(0, total - inZone));
            row.zoneGap = round2(Math.max(0, demand.needed - inZone));
            row.deficit = round2(Math.max(0, demand.needed - total));
            row.status = status;
            row.serieCount = demand.series.size();
            row.sequenceCount = demand.sequences.size();
            row.rollCount = stockIndex.rollCount(demand.material, zone);
            row.series = new ArrayList<>(demand.series);
            row.sequences = new ArrayList<>(demand.sequences);
            row.transferOptions = stockIndex.transferOptions(demand.material, zone);
            report.materials.add(row);
            if (!"OK".equals(status)) {
                report.issueCount++;
                if ("OUT_OF_ZONE".equals(status)) {
                    report.transferSuggestions.add(row);
                } else {
                    report.shortages.add(row);
                }
            }
        }

        Set<String> neededMaterials = report.materials.stream()
                .map(r -> r.material)
                .collect(Collectors.toSet());
        for (Map.Entry<String, Double> stock : stockIndex.materialsForZone(zone).entrySet()) {
            if (neededMaterials.contains(stock.getKey())) continue;
            if (stock.getValue() <= 0) continue;
            MaterialRow row = new MaterialRow();
            row.material = stock.getKey();
            row.needed = 0.0;
            row.availableInZone = round2(stock.getValue());
            row.availableTotal = round2(stockIndex.totalMeters(stock.getKey()));
            row.availableOutOfZone = round2(Math.max(0, row.availableTotal - row.availableInZone));
            row.status = "RETURN_TO_STOCK";
            row.rollCount = stockIndex.rollCount(stock.getKey(), zone);
            report.returnCandidates.add(row);
        }

        report.materials.sort(materialComparator());
        report.shortages.sort(materialComparator());
        report.transferSuggestions.sort(materialComparator());
        report.returnCandidates.sort(Comparator
                .comparingDouble((MaterialRow r) -> r.availableInZone).reversed()
                .thenComparing(r -> r.material));

        report.materials = limit(report.materials, MAX_MATERIALS_PER_ZONE);
        report.shortages = limit(report.shortages, MAX_ACTIONS_PER_ZONE);
        report.transferSuggestions = limit(report.transferSuggestions, MAX_ACTIONS_PER_ZONE);
        report.returnCandidates = limit(report.returnCandidates, MAX_ACTIONS_PER_ZONE);
        return report;
    }

    private Comparator<MaterialRow> materialComparator() {
        return Comparator
                .comparingInt((MaterialRow r) -> statusOrder(r.status))
                .thenComparing((MaterialRow a, MaterialRow b) ->
                        Double.compare(Math.max(b.deficit, b.zoneGap), Math.max(a.deficit, a.zoneGap)))
                .thenComparing(r -> r.material != null ? r.material : "");
    }

    private int statusOrder(String status) {
        if ("NONE".equals(status)) return 0;
        if ("SHORTAGE".equals(status)) return 1;
        if ("OUT_OF_ZONE".equals(status)) return 2;
        if ("OK".equals(status)) return 3;
        if ("RETURN_TO_STOCK".equals(status)) return 4;
        return 5;
    }

    private String materialStatus(double needed, double inZone, double total) {
        if (needed <= 0) return "RETURN_TO_STOCK";
        if (inZone >= needed) return "OK";
        if (total >= needed) return "OUT_OF_ZONE";
        if (total <= 0) return "NONE";
        return "SHORTAGE";
    }

    private <T> List<T> limit(List<T> source, int max) {
        if (source == null || source.size() <= max) return source;
        return new ArrayList<>(source.subList(0, max));
    }

    private StockIndex buildStockIndex(List<Map<String, Object>> stockRacks) {
        StockIndex index = new StockIndex(buildLocationToZone());
        if (stockRacks != null) {
            for (Map<String, Object> rack : stockRacks) {
                if (rack == null) continue;
                String material = normalizeMaterial(value(rack.get("reftissu")));
                if (material == null) continue;
                String location = value(rack.get("emplacement"));
                String zone = index.zoneForLocation(location);
                double meters = number(rack.get("metrage"));
                if (meters <= 0) meters = number(rack.get("quantite"));
                if (meters <= 0) continue;
                index.add(material, zone, value(rack.get("serialId")), meters);
            }
        }
        if (allocationService != null) {
            index.applyReservations(allocationService.reservedMetersByMaterialZone());
        }
        index.applyOnTable(collectOnTable());
        return index;
    }

    private List<Object[]> collectOnTable() {
        if (serieRouleauTempService == null) return Collections.emptyList();
        List<Object[]> out = new ArrayList<>();
        List<SerieRouleauTemp> rolls = serieRouleauTempService.getAll();
        if (rolls == null) return out;
        for (SerieRouleauTemp roll : rolls) {
            if (roll == null) continue;
            String material = normalizeMaterial(roll.getReftissu());
            if (material == null) continue;
            double rest = roll.getEstimationRest() != null ? roll.getEstimationRest() : 0.0;
            if (rest <= 0) continue;
            out.add(new Object[]{material, roll.getIdRouleau(), rest});
        }
        return out;
    }

    private Map<String, String> buildLocationToZone() {
        Map<String, String> out = new LinkedHashMap<>();
        List<Zone> zones = zoneRepository.findAllActive();
        if (zones == null) return out;
        for (Zone zone : zones) {
            if (zone == null || zone.getNom() == null) continue;
            for (String loc : parseLocations(zone.getRollLocations())) {
                out.put(norm(loc), zone.getNom());
            }
        }
        return out;
    }

    private List<String> parseLocations(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeMaterial(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (v.startsWith("P") && v.length() > 1) {
            v = v.substring(1);
        }
        return v.toUpperCase(Locale.ROOT);
    }

    private String norm(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private String value(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private double number(Object raw) {
        if (raw == null) return 0.0;
        if (raw instanceof Number) return ((Number) raw).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class SequenceCarrier {
        final LiveChargeDto.SequenceDto sequence;
        final String ownerZone;

        SequenceCarrier(LiveChargeDto.SequenceDto sequence, String ownerZone) {
            this.sequence = sequence;
            this.ownerZone = ownerZone;
        }
    }

    private static final class ScheduleSummary {
        LocalDateTime firstStart;
        LocalDateTime lastEnd;
        LocalDateTime lastPlannedAt;
        Long minutesToStart;
        final Set<String> machines = new LinkedHashSet<>();
    }

    private static final class SequenceFocus {
        String sequence;
        String zone;
        String state;
        String action;
        boolean opened;
        int boxCount;
        int remainingSeries;
        int totalSeries;
        double totalRemainingMinutes;
        long predictedCloseMinutes;
        LocalDateTime firstStart;
        LocalDateTime lastEnd;
        Long minutesToStart;
        List<String> machines = Collections.emptyList();
        LocalDate dueDate;
        String zoneSource;
        String zoneAcceptanceStatus;
        boolean locked;
        String materialStatus;
        boolean materialMissingSuggested;
        double focusScore;

        Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("sequence", sequence);
            out.put("zone", zone);
            out.put("state", state);
            out.put("action", action);
            out.put("opened", opened);
            out.put("boxCount", boxCount);
            out.put("remainingSeries", remainingSeries);
            out.put("totalSeries", totalSeries);
            out.put("totalRemainingMinutes", totalRemainingMinutes);
            out.put("predictedCloseMinutes", predictedCloseMinutes);
            out.put("firstStart", firstStart != null ? firstStart.toString() : null);
            out.put("lastEnd", lastEnd != null ? lastEnd.toString() : null);
            out.put("minutesToStart", minutesToStart);
            out.put("machines", machines);
            out.put("dueDate", dueDate != null ? dueDate.toString() : null);
            out.put("zoneSource", zoneSource);
            out.put("zoneAcceptanceStatus", zoneAcceptanceStatus);
            out.put("locked", locked);
            out.put("materialStatus", materialStatus);
            out.put("materialMissingSuggested", materialMissingSuggested);
            out.put("focusScore", focusScore);
            return out;
        }
    }

    private static final class BoxOccupancy {
        int configuredMachines;
        int activeMachines;
        int maxBoxes;
        int occupiedBoxes;
        double occupancyPct;
        final List<Map<String, Object>> occupiedSequences = new ArrayList<>();

        Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("configuredMachines", configuredMachines);
            out.put("activeMachines", activeMachines);
            out.put("maxBoxes", maxBoxes);
            out.put("occupiedBoxes", occupiedBoxes);
            out.put("occupancyPct", occupancyPct);
            out.put("occupiedSequences", occupiedSequences);
            return out;
        }
    }

    private static final class DemandIndex {
        final Map<String, Map<String, MaterialDemand>> byZone = new LinkedHashMap<>();

        MaterialDemand get(String zone, String material) {
            return byZone.computeIfAbsent(zone, z -> new LinkedHashMap<>())
                    .computeIfAbsent(material, MaterialDemand::new);
        }

        List<MaterialDemand> demandsForZone(String zone) {
            Map<String, MaterialDemand> rows = byZone.get(zone);
            if (rows == null) return Collections.emptyList();
            return new ArrayList<>(rows.values());
        }
    }

    private static final class MaterialDemand {
        final String material;
        double needed;
        final Set<String> series = new LinkedHashSet<>();
        final Set<String> sequences = new LinkedHashSet<>();

        MaterialDemand(String material) {
            this.material = material;
        }
    }

    private final class StockIndex {
        private final Map<String, String> locationToZone;
        private final Map<String, Double> totalByMaterial = new LinkedHashMap<>();
        private final Map<String, Map<String, Double>> byZoneByMaterial = new LinkedHashMap<>();
        private final Map<String, Map<String, Integer>> rollsByZoneByMaterial = new LinkedHashMap<>();
        private final Map<String, String> zoneBySerialId = new LinkedHashMap<>();

        StockIndex(Map<String, String> locationToZone) {
            this.locationToZone = locationToZone != null ? locationToZone : Collections.emptyMap();
        }

        void add(String material, String zone, String serialId, double meters) {
            totalByMaterial.merge(material, meters, Double::sum);
            String z = zone != null ? zone : "UNKNOWN";
            byZoneByMaterial.computeIfAbsent(z, k -> new LinkedHashMap<>()).merge(material, meters, Double::sum);
            rollsByZoneByMaterial.computeIfAbsent(z, k -> new LinkedHashMap<>()).merge(material, 1, Integer::sum);
            if (serialId != null) zoneBySerialId.put(serialId, z);
        }

        void applyReservations(Map<String, Double> reservedByMaterialZone) {
            if (reservedByMaterialZone == null) return;
            for (Map.Entry<String, Double> e : reservedByMaterialZone.entrySet()) {
                String key = e.getKey();
                double reserved = e.getValue() != null ? e.getValue() : 0.0;
                if (key == null || reserved <= 0) continue;
                int sep = key.lastIndexOf('|');
                if (sep <= 0 || sep >= key.length() - 1) continue;
                deduct(key.substring(0, sep), key.substring(sep + 1), reserved);
            }
        }

        void applyOnTable(List<Object[]> onTable) {
            if (onTable == null) return;
            for (Object[] row : onTable) {
                if (row == null || row.length < 3) continue;
                String material = (String) row[0];
                String serialId = (String) row[1];
                double rest = row[2] instanceof Number ? ((Number) row[2]).doubleValue() : 0.0;
                if (material == null || rest <= 0) continue;
                String zone = serialId != null ? zoneBySerialId.get(serialId) : null;
                if (zone != null) {
                    deduct(material, zone, rest);
                } else {
                    double cur = totalByMaterial.getOrDefault(material, 0.0);
                    totalByMaterial.put(material, Math.max(0.0, cur - rest));
                }
            }
        }

        private void deduct(String material, String zone, double amount) {
            double zoneAvail = meters(material, zone);
            double applied = Math.min(zoneAvail, amount);
            if (applied > 0) {
                Map<String, Double> byMaterial = byZoneByMaterial.get(zone);
                if (byMaterial != null) byMaterial.put(material, Math.max(0.0, zoneAvail - applied));
                double cur = totalByMaterial.getOrDefault(material, 0.0);
                totalByMaterial.put(material, Math.max(0.0, cur - applied));
            }
        }

        String zoneForLocation(String location) {
            String key = norm(location);
            if (key.isEmpty()) return "UNKNOWN";
            String exact = locationToZone.get(key);
            if (exact != null) return exact;
            for (Map.Entry<String, String> e : locationToZone.entrySet()) {
                if (!e.getKey().isEmpty() && key.contains(e.getKey())) {
                    return e.getValue();
                }
            }
            return "UNKNOWN";
        }

        double totalMeters(String material) {
            return totalByMaterial.getOrDefault(material, 0.0);
        }

        double meters(String material, String zone) {
            if (zone == null) return 0.0;
            Map<String, Double> byMaterial = byZoneByMaterial.get(zone);
            return byMaterial == null ? 0.0 : byMaterial.getOrDefault(material, 0.0);
        }

        int rollCount(String material, String zone) {
            if (zone == null) return 0;
            Map<String, Integer> byMaterial = rollsByZoneByMaterial.get(zone);
            return byMaterial == null ? 0 : byMaterial.getOrDefault(material, 0);
        }

        Map<String, Double> materialsForZone(String zone) {
            Map<String, Double> rows = byZoneByMaterial.get(zone);
            return rows == null ? Collections.emptyMap() : rows;
        }

        List<Map<String, Object>> transferOptions(String material, String currentZone) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map.Entry<String, Map<String, Double>> e : byZoneByMaterial.entrySet()) {
                String zone = e.getKey();
                if (zone == null || zone.equals(currentZone) || "UNKNOWN".equals(zone)) continue;
                double meters = e.getValue().getOrDefault(material, 0.0);
                if (meters <= 0) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("zone", zone);
                row.put("available", Math.round(meters * 100.0) / 100.0);
                row.put("rollCount", rollCount(material, zone));
                out.add(row);
            }
            out.sort(Comparator
                    .comparingDouble((Map<String, Object> r) -> ((Number) r.get("available")).doubleValue())
                    .reversed());
            if (out.size() > 3) {
                return new ArrayList<>(out.subList(0, 3));
            }
            return out;
        }
    }

    private static final class MaterialZoneReport {
        final String zone;
        int issueCount;
        List<MaterialRow> materials = new ArrayList<>();
        List<MaterialRow> shortages = new ArrayList<>();
        List<MaterialRow> transferSuggestions = new ArrayList<>();
        List<MaterialRow> returnCandidates = new ArrayList<>();

        MaterialZoneReport(String zone) {
            this.zone = zone;
        }

        Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("zone", zone);
            out.put("status", issueCount > 0 ? "ATTENTION" : "OK");
            out.put("issueCount", issueCount);
            out.put("materials", rows(materials));
            out.put("shortages", rows(shortages));
            out.put("transferSuggestions", rows(transferSuggestions));
            out.put("returnCandidates", rows(returnCandidates));
            return out;
        }

        private List<Map<String, Object>> rows(List<MaterialRow> input) {
            return input.stream().map(MaterialRow::toMap).collect(Collectors.toList());
        }
    }

    private static final class MaterialRow {
        String material;
        double needed;
        double availableInZone;
        double availableOutOfZone;
        double availableTotal;
        double deficit;
        double zoneGap;
        String status;
        int serieCount;
        int sequenceCount;
        int rollCount;
        List<String> series = Collections.emptyList();
        List<String> sequences = Collections.emptyList();
        List<Map<String, Object>> transferOptions = Collections.emptyList();

        Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("material", material);
            out.put("needed", needed);
            out.put("availableInZone", availableInZone);
            out.put("availableOutOfZone", availableOutOfZone);
            out.put("availableTotal", availableTotal);
            out.put("deficit", deficit);
            out.put("zoneGap", zoneGap);
            out.put("status", status);
            out.put("serieCount", serieCount);
            out.put("sequenceCount", sequenceCount);
            out.put("rollCount", rollCount);
            out.put("series", series);
            out.put("sequences", sequences);
            out.put("transferOptions", transferOptions);
            return out;
        }
    }
}
