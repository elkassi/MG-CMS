package com.lear.MGCMS.services.scheduling;

import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.domain.scheduling.*;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.scheduling.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for scheduling operations
 * Handles shift schedules, machine status, intervals, sequences, and series scheduling
 */
@Service
@Transactional
public class SchedulingService {

    @Autowired
    private ShiftScheduleRepository shiftScheduleRepository;
    
    @Autowired
    private MachineScheduleStatusRepository machineScheduleStatusRepository;
    
    @Autowired
    private ScheduleIntervalRepository scheduleIntervalRepository;
    
    @Autowired
    private SequenceScheduleRepository sequenceScheduleRepository;
    
    @Autowired
    private SerieScheduleRepository serieScheduleRepository;
    
    @Autowired
    private MaterialLogisticsRepository materialLogisticsRepository;
    
    @Autowired
    private ZoneRepository zoneRepository;
    
    @Autowired
    private ProductionTableRepository productionTableRepository;
    
    @Autowired
    private com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository cuttingRequestDataRepository;
    
    @Autowired
    private com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository cuttingRequestSerieDataRepository;

    // ========== Shift Constants ==========
    public static final LocalTime SHIFT_1_START = LocalTime.of(21, 55);
    public static final LocalTime SHIFT_1_END = LocalTime.of(5, 45);
    public static final LocalTime SHIFT_2_START = LocalTime.of(5, 55);
    public static final LocalTime SHIFT_2_END = LocalTime.of(13, 45);
    public static final LocalTime SHIFT_3_START = LocalTime.of(13, 55);
    public static final LocalTime SHIFT_3_END = LocalTime.of(21, 45);
    
    // Default spreading table capacity in meters
    public static final double DEFAULT_TABLE_CAPACITY = 14.0;

    // ========== Shift Schedule Methods ==========
    
    public ShiftSchedule createOrUpdateShiftSchedule(ShiftSchedule schedule, String username) {
        Optional<ShiftSchedule> existing = shiftScheduleRepository.findByZoneAndShiftDateAndShiftNumber(
            schedule.getZone(), schedule.getShiftDate(), schedule.getShiftNumber());
        
        if (existing.isPresent()) {
            ShiftSchedule existingSchedule = existing.get();
            existingSchedule.setSpreadingPersonnel(schedule.getSpreadingPersonnel());
            existingSchedule.setCuttingPersonnel(schedule.getCuttingPersonnel());
            existingSchedule.setLaserDxfPersonnel(schedule.getLaserDxfPersonnel());
            existingSchedule.setPressDiePersonnel(schedule.getPressDiePersonnel());
            existingSchedule.setQualityControlPersonnel(schedule.getQualityControlPersonnel());
            existingSchedule.setNotes(schedule.getNotes());
            existingSchedule.setStatus(schedule.getStatus());
            existingSchedule.setUpdatedAt(LocalDateTime.now());
            existingSchedule.setUpdatedBy(username);
            return shiftScheduleRepository.save(existingSchedule);
        } else {
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setCreatedBy(username);
            return shiftScheduleRepository.save(schedule);
        }
    }
    
    public List<ShiftSchedule> getShiftSchedulesForDate(LocalDate date) {
        return shiftScheduleRepository.findByShiftDate(date);
    }
    
    public Optional<ShiftSchedule> getCurrentShiftSchedule(String zoneName) {
        int currentShift = getCurrentShiftNumber();
        LocalDate today = LocalDate.now();
        
        // For shift 1 which spans midnight, check if we're in the early morning part
        if (currentShift == 1 && LocalTime.now().isBefore(LocalTime.of(6, 0))) {
            today = today.minusDays(1); // Shift 1 started yesterday
        }
        
        return shiftScheduleRepository.findByZoneNameAndDateAndShift(zoneName, today, currentShift);
    }
    
    public int getCurrentShiftNumber() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int time = hour * 60 + minute;
        
        // Shift 1: 21:55 to 05:45 (1315 to 345)
        // Shift 2: 05:55 to 13:45 (355 to 825)
        // Shift 3: 13:55 to 21:45 (835 to 1305)
        
        if (time >= 1315 || time < 345) return 1;
        if (time >= 355 && time < 825) return 2;
        if (time >= 835 && time < 1305) return 3;
        return 1;
    }
    
    public LocalDateTime getShiftStartTime(LocalDate date, int shiftNumber) {
        switch (shiftNumber) {
            case 1: return LocalDateTime.of(date, SHIFT_1_START);
            case 2: return LocalDateTime.of(date, SHIFT_2_START);
            case 3: return LocalDateTime.of(date, SHIFT_3_START);
            default: return LocalDateTime.of(date, SHIFT_1_START);
        }
    }
    
    public LocalDateTime getShiftEndTime(LocalDate date, int shiftNumber) {
        switch (shiftNumber) {
            case 1: return LocalDateTime.of(date.plusDays(1), SHIFT_1_END);
            case 2: return LocalDateTime.of(date, SHIFT_2_END);
            case 3: return LocalDateTime.of(date, SHIFT_3_END);
            default: return LocalDateTime.of(date.plusDays(1), SHIFT_1_END);
        }
    }

    // ========== Machine Schedule Status Methods ==========
    
    public MachineScheduleStatus updateMachineStatus(MachineScheduleStatus status, String username) {
        Optional<MachineScheduleStatus> existing = machineScheduleStatusRepository
            .findByMachineAndEffectiveDateAndShiftNumber(
                status.getMachine(), status.getEffectiveDate(), status.getShiftNumber());
        
        if (existing.isPresent()) {
            MachineScheduleStatus existingStatus = existing.get();
            existingStatus.setAvailable(status.getAvailable());
            existingStatus.setUnavailableReason(status.getUnavailableReason());
            existingStatus.setUnavailableUntil(status.getUnavailableUntil());
            existingStatus.setOverrideZone(status.getOverrideZone());
            existingStatus.setScheduledHours(status.getScheduledHours());
            existingStatus.setLoadPercentage(status.getLoadPercentage());
            existingStatus.setUpdatedAt(LocalDateTime.now());
            existingStatus.setUpdatedBy(username);
            return machineScheduleStatusRepository.save(existingStatus);
        } else {
            status.setCreatedAt(LocalDateTime.now());
            status.setCreatedBy(username);
            return machineScheduleStatusRepository.save(status);
        }
    }
    
    public List<MachineScheduleStatus> getMachineStatusForShift(LocalDate date, int shiftNumber) {
        return machineScheduleStatusRepository.findByEffectiveDateAndShiftNumber(date, shiftNumber);
    }
    
    public List<MachineScheduleStatus> getUnavailableMachines(LocalDate startDate) {
        return machineScheduleStatusRepository.findUnavailableMachines(startDate);
    }
    
    public List<ProductionTable> getAvailableMachinesForZone(String zoneName, LocalDate date, int shiftNumber) {
        Zone zone = zoneRepository.findById(zoneName).orElse(null);
        if (zone == null) return Collections.emptyList();
        
        List<ProductionTable> zoneMachines = productionTableRepository.findByZone(zoneName);
        List<MachineScheduleStatus> statuses = machineScheduleStatusRepository
            .findByZoneAndDateAndShift(zone, date, shiftNumber);
        
        // Get machines with zone override to this zone
        List<MachineScheduleStatus> overrides = machineScheduleStatusRepository
            .findByOverrideZoneAndDateAndShift(zone, date, shiftNumber);
        
        Set<String> unavailableMachineNames = statuses.stream()
            .filter(s -> !s.getAvailable())
            .map(s -> s.getMachine().getNom())
            .collect(Collectors.toSet());
        
        List<ProductionTable> available = zoneMachines.stream()
            .filter(m -> !unavailableMachineNames.contains(m.getNom()))
            .collect(Collectors.toList());
        
        // Add machines from other zones with override
        for (MachineScheduleStatus override : overrides) {
            if (override.getAvailable() && !available.contains(override.getMachine())) {
                available.add(override.getMachine());
            }
        }
        
        return available;
    }

    // ========== Schedule Interval Methods ==========
    
    public ScheduleInterval createInterval(ScheduleInterval interval, String username) {
        interval.setCreatedAt(LocalDateTime.now());
        interval.setCreatedBy(username);
        return scheduleIntervalRepository.save(interval);
    }
    
    public void deleteInterval(Long id) {
        scheduleIntervalRepository.deleteById(id);
    }
    
    public List<ScheduleInterval> getIntervalsForTimeRange(LocalDateTime start, LocalDateTime end) {
        return scheduleIntervalRepository.findOverlappingIntervals(start, end);
    }
    
    public List<ScheduleInterval> getIntervalsForMachine(String machineName, LocalDateTime start, LocalDateTime end) {
        return scheduleIntervalRepository.findIntervalsForMachineInRange(machineName, start, end);
    }
    
    public List<ScheduleInterval> getAllIntervals() {
        return scheduleIntervalRepository.findAll();
    }

    // ========== Sequence Schedule Methods ==========
    
    public SequenceSchedule createOrUpdateSequenceSchedule(SequenceSchedule schedule, String username) {
        Optional<SequenceSchedule> existing = sequenceScheduleRepository.findBySequenceId(schedule.getSequenceId());
        
        if (existing.isPresent()) {
            SequenceSchedule existingSchedule = existing.get();
            existingSchedule.setAssignedZone(schedule.getAssignedZone());
            existingSchedule.setPriority(schedule.getPriority());
            existingSchedule.setStatus(schedule.getStatus());
            existingSchedule.setExcluded(schedule.getExcluded());
            existingSchedule.setNotes(schedule.getNotes());
            existingSchedule.setScheduledDate(schedule.getScheduledDate());
            existingSchedule.setScheduledShift(schedule.getScheduledShift());
            existingSchedule.setEstimatedStartTime(schedule.getEstimatedStartTime());
            existingSchedule.setEstimatedEndTime(schedule.getEstimatedEndTime());
            existingSchedule.setTotalSeries(schedule.getTotalSeries());
            existingSchedule.setCompletedSeries(schedule.getCompletedSeries());
            existingSchedule.setCompletionPercentage(schedule.getCompletionPercentage());
            existingSchedule.setUpdatedAt(LocalDateTime.now());
            existingSchedule.setUpdatedBy(username);
            return sequenceScheduleRepository.save(existingSchedule);
        } else {
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setCreatedBy(username);
            return sequenceScheduleRepository.save(schedule);
        }
    }
    
    public List<SequenceSchedule> getActiveSequencesOrderByPriority() {
        return sequenceScheduleRepository.findAllActiveOrderByPriority();
    }
    
    public List<SequenceSchedule> getSequencesByZone(String zoneName) {
        return sequenceScheduleRepository.findActiveByZoneOrderByPriority(zoneName);
    }
    
    public void excludeSequence(String sequenceId, String username) {
        Optional<SequenceSchedule> schedule = sequenceScheduleRepository.findBySequenceId(sequenceId);
        if (schedule.isPresent()) {
            SequenceSchedule ss = schedule.get();
            ss.setExcluded(true);
            ss.setStatus("EXCLUDED");
            ss.setUpdatedAt(LocalDateTime.now());
            ss.setUpdatedBy(username);
            sequenceScheduleRepository.save(ss);
        }
    }
    
    public void updateSequencePriorities(List<Map<String, Object>> priorities, String username) {
        for (Map<String, Object> p : priorities) {
            String sequenceId = (String) p.get("sequenceId");
            Integer priority = (Integer) p.get("priority");
            
            Optional<SequenceSchedule> schedule = sequenceScheduleRepository.findBySequenceId(sequenceId);
            if (schedule.isPresent()) {
                SequenceSchedule ss = schedule.get();
                ss.setPriority(priority);
                ss.setUpdatedAt(LocalDateTime.now());
                ss.setUpdatedBy(username);
                sequenceScheduleRepository.save(ss);
            } else {
                SequenceSchedule newSchedule = new SequenceSchedule();
                newSchedule.setSequenceId(sequenceId);
                newSchedule.setPriority(priority);
                newSchedule.setCreatedBy(username);
                sequenceScheduleRepository.save(newSchedule);
            }
        }
    }

    // ========== Serie Schedule Methods ==========
    
    public SerieSchedule createOrUpdateSerieSchedule(SerieSchedule schedule, String username) {
        Optional<SerieSchedule> existing = serieScheduleRepository.findBySerieId(schedule.getSerieId());
        
        if (existing.isPresent()) {
            SerieSchedule existingSchedule = existing.get();
            existingSchedule.setSpreadingTable(schedule.getSpreadingTable());
            existingSchedule.setSpreadingStartEstimated(schedule.getSpreadingStartEstimated());
            existingSchedule.setSpreadingEndEstimated(schedule.getSpreadingEndEstimated());
            existingSchedule.setSpreadingTimeMinutes(schedule.getSpreadingTimeMinutes());
            existingSchedule.setCuttingMachine(schedule.getCuttingMachine());
            existingSchedule.setCuttingMachineName(schedule.getCuttingMachineName());
            existingSchedule.setCuttingStartEstimated(schedule.getCuttingStartEstimated());
            existingSchedule.setCuttingEndEstimated(schedule.getCuttingEndEstimated());
            existingSchedule.setCuttingTimeMinutes(schedule.getCuttingTimeMinutes());
            existingSchedule.setSpreadingStatus(schedule.getSpreadingStatus());
            existingSchedule.setCuttingStatus(schedule.getCuttingStatus());
            existingSchedule.setSchedulingOrder(schedule.getSchedulingOrder());
            existingSchedule.setAssignmentReason(schedule.getAssignmentReason());
            existingSchedule.setUpdatedAt(LocalDateTime.now());
            return serieScheduleRepository.save(existingSchedule);
        } else {
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setCreatedBy(username);
            return serieScheduleRepository.save(schedule);
        }
    }
    
    public List<SerieSchedule> getSeriesForMachine(String machineName, LocalDateTime start, LocalDateTime end) {
        return serieScheduleRepository.findScheduledForMachineInRange(machineName, start, end);
    }
    
    public List<SerieSchedule> getSeriesForSpreadingTable(String tableName, LocalDateTime start, LocalDateTime end) {
        return serieScheduleRepository.findScheduledForSpreadingTableInRange(tableName, start, end);
    }
    
    public List<SerieSchedule> getSeriesWaitingForSpreading() {
        return serieScheduleRepository.findWaitingForSpreading();
    }
    
    public List<SerieSchedule> getSeriesReadyForCutting() {
        return serieScheduleRepository.findReadyForCutting();
    }

    // ========== Material Logistics Methods ==========
    
    public MaterialLogistics createMaterialRequest(MaterialLogistics request, String username) {
        request.setCreatedAt(LocalDateTime.now());
        request.setCreatedBy(username);
        return materialLogisticsRepository.save(request);
    }
    
    public List<MaterialLogistics> getPendingMaterialRequests() {
        return materialLogisticsRepository.findPendingOrderByNeededBy();
    }
    
    public List<MaterialLogistics> getMaterialRequestsByZone(String zoneName) {
        return materialLogisticsRepository.findActiveByZone(zoneName);
    }
    
    public List<MaterialLogistics> getUrgentMaterialRequests(int hoursAhead) {
        LocalDateTime deadline = LocalDateTime.now().plusHours(hoursAhead);
        return materialLogisticsRepository.findUrgentPending(deadline);
    }
    
    public void updateMaterialRequestStatus(Long id, String status, String username) {
        materialLogisticsRepository.findById(id).ifPresent(request -> {
            request.setStatus(status);
            request.setUpdatedAt(LocalDateTime.now());
            request.setUpdatedBy(username);
            if ("DELIVERED".equals(status)) {
                request.setCompletedAt(LocalDateTime.now());
                request.setCompletedBy(username);
            }
            materialLogisticsRepository.save(request);
        });
    }

    // ========== Helper Methods ==========
    
    public int calculateSpreadingTime(int nbrCouche, double longueur) {
        // Base time + (layers * length * rate)
        int baseTime = 15;
        double rate = 0.5;
        int spreadingTime = baseTime + (int) Math.ceil(nbrCouche * longueur * rate);
        return Math.max(15, spreadingTime);
    }
    
    public double calculateTableCapacityUsed(String tableName, LocalDateTime now) {
        // Get all series on this table that are spread but not cut
        List<SerieSchedule> seriesOnTable = serieScheduleRepository.findBySpreadingTable(tableName);
        
        double usedCapacity = 0;
        for (SerieSchedule serie : seriesOnTable) {
            if ("COMPLETE".equals(serie.getSpreadingStatus()) && !"COMPLETE".equals(serie.getCuttingStatus())) {
                double remainingLength = serie.getLongueur() != null ? serie.getLongueur() : 0;
                
                // If cutting is in progress, calculate remaining based on progress
                if ("IN_PROGRESS".equals(serie.getCuttingStatus()) && serie.getCuttingStartEstimated() != null) {
                    int cuttingTime = serie.getCuttingTimeMinutes() != null ? serie.getCuttingTimeMinutes() : 60;
                    long elapsedMinutes = java.time.Duration.between(serie.getCuttingStartEstimated(), now).toMinutes();
                    double progress = Math.min(1.0, Math.max(0, (double) elapsedMinutes / cuttingTime));
                    remainingLength = remainingLength * (1 - progress);
                }
                
                usedCapacity += remainingLength;
            }
        }
        
        return usedCapacity;
    }
    
    public double getAvailableTableCapacity(String tableName, LocalDateTime now) {
        return DEFAULT_TABLE_CAPACITY - calculateTableCapacityUsed(tableName, now);
    }
    
    // ========== Sequence Initialization Methods ==========
    
    /**
     * Parse sequence ID to extract date
     * Format: ddMMyyHHmm + 2 digit sequence number
     * Example: 010124085001 = 01 Jan 2024, 08:50, sequence 01
     */
    public LocalDate parseSequenceDate(String sequenceId) {
        if (sequenceId == null || sequenceId.length() < 10) {
            return null;
        }
        try {
            String datePart = sequenceId.substring(0, 10); // ddMMyyHHmm
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyHHmm");
            LocalDateTime dateTime = LocalDateTime.parse(datePart, formatter);
            return dateTime.toLocalDate();
        } catch (DateTimeParseException e) {
            // If parsing fails, try using createdAt from the sequence data
            return null;
        }
    }
    
    /**
     * Check if sequence is within last N days
     */
    public boolean isSequenceWithinDays(String sequenceId, int days) {
        LocalDate seqDate = parseSequenceDate(sequenceId);
        if (seqDate == null) {
            // Fallback: check if sequence exists and use createdAt if available
            com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData seq = 
                cuttingRequestDataRepository.findBySequence(sequenceId);
            if (seq != null && seq.getCreatedAt() != null) {
                seqDate = seq.getCreatedAt().toLocalDate();
            } else {
                return false; // Can't determine date, exclude it
            }
        }
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return !seqDate.isBefore(cutoffDate);
    }
    
    /**
     * Initialize sequences from existing CuttingRequestData
     * This method imports sequences from last 2 days (or all if specified)
     */
    public Map<String, Object> initializeSequencesFromExistingData(String username, boolean onlyIncomplete, boolean lastTwoDaysOnly) {
        Map<String, Object> result = new HashMap<>();
        int sequencesCreated = 0;
        int seriesCreated = 0;
        int sequencesSkipped = 0;
        int seriesSkipped = 0;
        
        // Get all sequences
        List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData> allSequences = cuttingRequestDataRepository.findAll();
        
        // Filter by last 2 days if requested
        if (lastTwoDaysOnly) {
            allSequences = allSequences.stream()
                .filter(seq -> isSequenceWithinDays(seq.getSequence(), 2))
                .collect(Collectors.toList());
        }
        
        for (com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData sequenceData : allSequences) {
            String sequenceId = sequenceData.getSequence();
            
            // Check if already exists
            Optional<SequenceSchedule> existing = sequenceScheduleRepository.findBySequenceId(sequenceId);
            if (existing.isPresent()) {
                sequencesSkipped++;
                continue;
            }
            
            // Get all series for this sequence
            List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData> seriesList = 
                cuttingRequestSerieDataRepository.findBySequence(sequenceId);
            
            if (seriesList.isEmpty()) {
                continue;
            }
            
            // Determine sequence status based on series statuses
            long completedSeries = seriesList.stream()
                .filter(s -> "Complete".equals(s.getStatusCoupe()))
                .count();
            long inProgressSeries = seriesList.stream()
                .filter(s -> "In progress".equals(s.getStatusMatelassage()) || 
                           "In progress".equals(s.getStatusCoupe()))
                .count();
            
            String sequenceStatus;
            if (completedSeries == seriesList.size()) {
                sequenceStatus = "COMPLETED";
                if (onlyIncomplete) {
                    sequencesSkipped++;
                    continue; // Skip completed sequences if requested
                }
            } else if (inProgressSeries > 0 || completedSeries > 0) {
                sequenceStatus = "IN_PROGRESS";
            } else {
                sequenceStatus = "NOT_STARTED";
            }
            
            // Create SequenceSchedule
            SequenceSchedule seqSchedule = new SequenceSchedule();
            seqSchedule.setSequenceId(sequenceId);
            seqSchedule.setAssignedZone(sequenceData.getZone());
            seqSchedule.setStatus(sequenceStatus);
            seqSchedule.setTotalSeries(seriesList.size());
            seqSchedule.setCompletedSeries((int) completedSeries);
            seqSchedule.setCompletionPercentage(seriesList.isEmpty() ? 0.0 : (double) completedSeries / seriesList.size() * 100);
            seqSchedule.setPriority(999); // Default priority, can be adjusted later
            seqSchedule.setExcluded(false);
            seqSchedule.setCreatedAt(LocalDateTime.now());
            seqSchedule.setCreatedBy(username);
            
            // Set planning date if available
            if (sequenceData.getPlanningDate() != null) {
                seqSchedule.setScheduledDate(sequenceData.getPlanningDate());
            }
            
            sequenceScheduleRepository.save(seqSchedule);
            sequencesCreated++;
            
            // Create SerieSchedule for each serie
            for (com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData serieData : seriesList) {
                // Check if already exists
                Optional<SerieSchedule> existingSerie = serieScheduleRepository.findBySerieId(serieData.getSerie());
                if (existingSerie.isPresent()) {
                    seriesSkipped++;
                    continue;
                }
                
                SerieSchedule serieSchedule = new SerieSchedule();
                serieSchedule.setSerieId(serieData.getSerie());
                serieSchedule.setSequenceId(sequenceId);
                serieSchedule.setPartNumberMaterial(serieData.getPartNumberMaterial());
                serieSchedule.setLongueur(serieData.getLongueur());
                serieSchedule.setNbrCouche(serieData.getNbrCouche());
                
                // Map spreading status
                String spreadingStatus = mapStatusToScheduleStatus(serieData.getStatusMatelassage());
                serieSchedule.setSpreadingStatus(spreadingStatus);
                serieSchedule.setSpreadingTable(serieData.getTableMatelassage());
                
                if (serieData.getDateDebutMatelassage() != null) {
                    serieSchedule.setSpreadingStartEstimated(serieData.getDateDebutMatelassage());
                }
                if (serieData.getDateFinMatelassage() != null) {
                    serieSchedule.setSpreadingEndEstimated(serieData.getDateFinMatelassage());
                }
                
                // Calculate spreading time if not set
                if (serieData.getNbrCouche() != null && serieData.getLongueur() != null) {
                    int spreadingTime = calculateSpreadingTime(serieData.getNbrCouche(), serieData.getLongueur());
                    serieSchedule.setSpreadingTimeMinutes(spreadingTime);
                }
                
                // Map cutting status
                String cuttingStatus = mapStatusToScheduleStatus(serieData.getStatusCoupe());
                serieSchedule.setCuttingStatus(cuttingStatus);
                serieSchedule.setCuttingMachineName(serieData.getTableCoupe());
                
                if (serieData.getDateDebutCoupe() != null) {
                    serieSchedule.setCuttingStartEstimated(serieData.getDateDebutCoupe());
                }
                if (serieData.getDateFinCoupe() != null) {
                    serieSchedule.setCuttingEndEstimated(serieData.getDateFinCoupe());
                }
                
                // Set cutting time from existing data if available
                if (serieData.getTempsDeCoupe() != null) {
                    serieSchedule.setCuttingTimeMinutes((int) Math.round(serieData.getTempsDeCoupe()));
                }
                
                // Try to find ProductionTable for cutting machine
                if (serieData.getTableCoupe() != null) {
                    productionTableRepository.findByNom(serieData.getTableCoupe())
                        .ifPresent(serieSchedule::setCuttingMachine);
                }
                
                serieSchedule.setCreatedAt(LocalDateTime.now());
                serieSchedule.setCreatedBy(username);
                
                serieScheduleRepository.save(serieSchedule);
                seriesCreated++;
            }
        }
        
        result.put("sequencesCreated", sequencesCreated);
        result.put("seriesCreated", seriesCreated);
        result.put("sequencesSkipped", sequencesSkipped);
        result.put("seriesSkipped", seriesSkipped);
        result.put("success", true);
        result.put("message", String.format(
            "Initialized %d sequences (%d series). Skipped %d sequences (%d series) that already exist.",
            sequencesCreated, seriesCreated, sequencesSkipped, seriesSkipped));
        
        return result;
    }
    
    /**
     * Map old status values to new schedule status values
     */
    private String mapStatusToScheduleStatus(String oldStatus) {
        if (oldStatus == null) return "WAITING";
        
        switch (oldStatus.toLowerCase()) {
            case "complete":
            case "completed":
                return "COMPLETE";
            case "in progress":
            case "in_progress":
                return "IN_PROGRESS";
            case "waiting":
            default:
                return "WAITING";
        }
    }
    
    /**
     * Get sequences that need to be initialized (from last 2 days by default)
     */
    public List<Map<String, Object>> getSequencesToInitialize(boolean onlyIncomplete, boolean lastTwoDaysOnly) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData> allSequences = cuttingRequestDataRepository.findAll();
        
        // Filter by last 2 days if requested
        if (lastTwoDaysOnly) {
            allSequences = allSequences.stream()
                .filter(seq -> isSequenceWithinDays(seq.getSequence(), 2))
                .collect(Collectors.toList());
        }
        
        for (com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData sequenceData : allSequences) {
            String sequenceId = sequenceData.getSequence();
            
            // Check if already exists
            if (sequenceScheduleRepository.findBySequenceId(sequenceId).isPresent()) {
                continue;
            }
            
            // Get series for this sequence
            List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData> seriesList = 
                cuttingRequestSerieDataRepository.findBySequence(sequenceId);
            
            if (seriesList.isEmpty()) {
                continue;
            }
            
            // Count statuses
            long completedSeries = seriesList.stream()
                .filter(s -> "Complete".equals(s.getStatusCoupe()))
                .count();
            
            if (onlyIncomplete && completedSeries == seriesList.size()) {
                continue; // Skip completed sequences
            }
            
            Map<String, Object> seqInfo = new HashMap<>();
            seqInfo.put("sequenceId", sequenceId);
            seqInfo.put("zone", sequenceData.getZone() != null ? sequenceData.getZone().getNom() : null);
            seqInfo.put("totalSeries", seriesList.size());
            seqInfo.put("completedSeries", (int) completedSeries);
            seqInfo.put("waitingSeries", (int) seriesList.stream()
                .filter(s -> "Waiting".equals(s.getStatusMatelassage()) && "Waiting".equals(s.getStatusCoupe()))
                .count());
            seqInfo.put("inProgressSeries", (int) seriesList.stream()
                .filter(s -> "In progress".equals(s.getStatusMatelassage()) || "In progress".equals(s.getStatusCoupe()))
                .count());
            seqInfo.put("status", completedSeries == seriesList.size() ? "COMPLETED" : 
                       (completedSeries > 0 || seriesList.stream().anyMatch(s -> 
                           "In progress".equals(s.getStatusMatelassage()) || "In progress".equals(s.getStatusCoupe()))) ? 
                       "IN_PROGRESS" : "NOT_STARTED");
            
            result.add(seqInfo);
        }
        
        return result;
    }
    
    /**
     * Load a specific sequence and its series into scheduling
     */
    public Map<String, Object> loadSequence(String sequenceId, String username) {
        Map<String, Object> result = new HashMap<>();
        
        // Check if sequence exists
        com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData sequenceData = 
            cuttingRequestDataRepository.findBySequence(sequenceId);
        
        if (sequenceData == null) {
            result.put("success", false);
            result.put("message", "Sequence not found: " + sequenceId);
            return result;
        }
        
        // Check if already initialized
        Optional<SequenceSchedule> existing = sequenceScheduleRepository.findBySequenceId(sequenceId);
        if (existing.isPresent()) {
            result.put("success", false);
            result.put("message", "Sequence already initialized: " + sequenceId);
            result.put("sequenceSchedule", existing.get());
            return result;
        }
        
        // Get series
        List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData> seriesList = 
            cuttingRequestSerieDataRepository.findBySequence(sequenceId);
        
        if (seriesList.isEmpty()) {
            result.put("success", false);
            result.put("message", "No series found for sequence: " + sequenceId);
            return result;
        }
        
        // Create sequence schedule (same logic as initializeSequencesFromExistingData)
        long completedSeries = seriesList.stream()
            .filter(s -> "Complete".equals(s.getStatusCoupe()))
            .count();
        long inProgressSeries = seriesList.stream()
            .filter(s -> "In progress".equals(s.getStatusMatelassage()) || 
                       "In progress".equals(s.getStatusCoupe()))
            .count();
        
        String sequenceStatus = completedSeries == seriesList.size() ? "COMPLETED" :
                               (inProgressSeries > 0 || completedSeries > 0) ? "IN_PROGRESS" : "NOT_STARTED";
        
        SequenceSchedule seqSchedule = new SequenceSchedule();
        seqSchedule.setSequenceId(sequenceId);
        seqSchedule.setAssignedZone(sequenceData.getZone());
        seqSchedule.setStatus(sequenceStatus);
        seqSchedule.setTotalSeries(seriesList.size());
        seqSchedule.setCompletedSeries((int) completedSeries);
        seqSchedule.setCompletionPercentage(seriesList.isEmpty() ? 0.0 : (double) completedSeries / seriesList.size() * 100);
        seqSchedule.setPriority(999);
        seqSchedule.setExcluded(false);
        seqSchedule.setCreatedAt(LocalDateTime.now());
        seqSchedule.setCreatedBy(username);
        if (sequenceData.getPlanningDate() != null) {
            seqSchedule.setScheduledDate(sequenceData.getPlanningDate());
        }
        
        sequenceScheduleRepository.save(seqSchedule);
        
        // Create serie schedules
        int seriesCreated = 0;
        for (com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData serieData : seriesList) {
            if (serieScheduleRepository.findBySerieId(serieData.getSerie()).isPresent()) {
                continue;
            }
            
            SerieSchedule serieSchedule = createSerieScheduleFromSerieData(serieData, sequenceId, username);
            serieScheduleRepository.save(serieSchedule);
            seriesCreated++;
        }
        
        result.put("success", true);
        result.put("message", String.format("Loaded sequence %s with %d series", sequenceId, seriesCreated));
        result.put("sequenceSchedule", seqSchedule);
        result.put("seriesCreated", seriesCreated);
        
        return result;
    }
    
    /**
     * Helper to create SerieSchedule from CuttingRequestSerieData
     */
    public SerieSchedule createSerieScheduleFromSerieData(
            com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData serieData, 
            String sequenceId, 
            String username) {
        SerieSchedule serieSchedule = new SerieSchedule();
        serieSchedule.setSerieId(serieData.getSerie());
        serieSchedule.setSequenceId(sequenceId);
        serieSchedule.setPartNumberMaterial(serieData.getPartNumberMaterial());
        serieSchedule.setLongueur(serieData.getLongueur());
        serieSchedule.setNbrCouche(serieData.getNbrCouche());
        
        String spreadingStatus = mapStatusToScheduleStatus(serieData.getStatusMatelassage());
        serieSchedule.setSpreadingStatus(spreadingStatus);
        serieSchedule.setSpreadingTable(serieData.getTableMatelassage());
        
        if (serieData.getDateDebutMatelassage() != null) {
            serieSchedule.setSpreadingStartEstimated(serieData.getDateDebutMatelassage());
        }
        if (serieData.getDateFinMatelassage() != null) {
            serieSchedule.setSpreadingEndEstimated(serieData.getDateFinMatelassage());
        }
        if (serieData.getNbrCouche() != null && serieData.getLongueur() != null) {
            serieSchedule.setSpreadingTimeMinutes(
                calculateSpreadingTime(serieData.getNbrCouche(), serieData.getLongueur()));
        }
        
        String cuttingStatus = mapStatusToScheduleStatus(serieData.getStatusCoupe());
        serieSchedule.setCuttingStatus(cuttingStatus);
        serieSchedule.setCuttingMachineName(serieData.getTableCoupe());
        
        if (serieData.getDateDebutCoupe() != null) {
            serieSchedule.setCuttingStartEstimated(serieData.getDateDebutCoupe());
        }
        if (serieData.getDateFinCoupe() != null) {
            serieSchedule.setCuttingEndEstimated(serieData.getDateFinCoupe());
        }
        if (serieData.getTempsDeCoupe() != null) {
            serieSchedule.setCuttingTimeMinutes((int) Math.round(serieData.getTempsDeCoupe()));
        }
        if (serieData.getTableCoupe() != null) {
            productionTableRepository.findByNom(serieData.getTableCoupe())
                .ifPresent(serieSchedule::setCuttingMachine);
        }
        
        serieSchedule.setCreatedAt(LocalDateTime.now());
        serieSchedule.setCreatedBy(username);
        
        return serieSchedule;
    }
    
    /**
     * Remove a sequence from scheduling (but keep in database, just exclude it)
     */
    public Map<String, Object> removeSequence(String sequenceId, String username) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<SequenceSchedule> schedule = sequenceScheduleRepository.findBySequenceId(sequenceId);
        if (!schedule.isPresent()) {
            result.put("success", false);
            result.put("message", "Sequence not found in scheduling: " + sequenceId);
            return result;
        }
        
        SequenceSchedule seqSchedule = schedule.get();
        seqSchedule.setExcluded(true);
        seqSchedule.setStatus("EXCLUDED");
        seqSchedule.setUpdatedAt(LocalDateTime.now());
        seqSchedule.setUpdatedBy(username);
        sequenceScheduleRepository.save(seqSchedule);
        
        result.put("success", true);
        result.put("message", "Sequence removed from scheduling: " + sequenceId);
        
        return result;
    }
    
    /**
     * Change zone assignment for a sequence
     */
    public Map<String, Object> changeSequenceZone(String sequenceId, String zoneName, String username) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<SequenceSchedule> scheduleOpt = sequenceScheduleRepository.findBySequenceId(sequenceId);
        if (!scheduleOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "Sequence not found in scheduling: " + sequenceId);
            return result;
        }
        
        Zone newZone = zoneRepository.findById(zoneName).orElse(null);
        if (newZone == null) {
            result.put("success", false);
            result.put("message", "Zone not found: " + zoneName);
            return result;
        }
        
        SequenceSchedule seqSchedule = scheduleOpt.get();
        seqSchedule.setAssignedZone(newZone);
        seqSchedule.setUpdatedAt(LocalDateTime.now());
        seqSchedule.setUpdatedBy(username);
        sequenceScheduleRepository.save(seqSchedule);
        
        result.put("success", true);
        result.put("message", String.format("Sequence %s assigned to zone %s", sequenceId, zoneName));
        result.put("sequenceSchedule", seqSchedule);
        
        return result;
    }
    
    /**
     * Get sequences for dashboard (last 2 days by default)
     */
    public List<SequenceSchedule> getActiveSequencesForDashboard(boolean lastTwoDaysOnly) {
        List<SequenceSchedule> allActive = sequenceScheduleRepository.findAllActiveOrderByPriority();
        
        if (lastTwoDaysOnly) {
            return allActive.stream()
                .filter(seq -> isSequenceWithinDays(seq.getSequenceId(), 2))
                .collect(Collectors.toList());
        }
        
        return allActive;
    }
}

