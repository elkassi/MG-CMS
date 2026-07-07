package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestPartNumberData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestPartNumberDataRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Service
public class SerieStatusDateValidator {

    private static final Logger logger = LoggerFactory.getLogger(SerieStatusDateValidator.class);

    // Spreading time constants (same as OrdonnancementService)
    private static final double COEF_SPREADING_PER_METRE = 0.5;
    private static final double COEF_SETUP_TIME = 2.0;

    @Autowired
    private CuttingRequestSerieDataRepository serieDataRepository;

    @Autowired
    private CuttingRequestPartNumberDataRepository partNumberDataRepository;

    @Autowired
    private CuttingRequestRepository cuttingRequestRepository;

    @Autowired
    private CuttingTimeCalculator cuttingTimeCalculator;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("cmsDataSource")
    private DataSource cmsDataSource;

    public static class ValidationIssue {
        public String serie;
        public String sequence;
        public String field;
        public String issueType;
        public String currentValue;
        public String expectedValue;
        public boolean autoCorrected;

        public ValidationIssue() {
        }

        public ValidationIssue(String serie, String sequence, String field, String issueType,
                               String currentValue, String expectedValue, boolean autoCorrected) {
            this.serie = serie;
            this.sequence = sequence;
            this.field = field;
            this.issueType = issueType;
            this.currentValue = currentValue;
            this.expectedValue = expectedValue;
            this.autoCorrected = autoCorrected;
        }
    }

    // ======================== NEW DURATION-BASED VALIDATION ========================

    /**
     * Validate all series — read-only audit mode.
     */
    public List<ValidationIssue> validateAll() {
        List<CuttingRequestSerieData> all = serieDataRepository.findAll();
        List<ValidationIssue> issues = new ArrayList<>();
        for (CuttingRequestSerieData serie : all) {
            validateSingle(serie, issues, false);
        }
        logger.info("Validated {} series, found {} issues", all.size(), issues.size());
        return issues;
    }

    /**
     * Validate specific sequences — optionally auto-correct.
     */
    @Transactional
    public List<ValidationIssue> validateAndCorrect(List<String> sequences, boolean autoCorrect) {
        List<CuttingRequestSerieData> series = serieDataRepository.findBySequencesArr(sequences);
        List<ValidationIssue> issues = new ArrayList<>();
        List<CuttingRequestSerieData> corrected = new ArrayList<>();

        for (CuttingRequestSerieData serie : series) {
            boolean wasCorrected = validateSingle(serie, issues, autoCorrect);
            if (autoCorrect && wasCorrected) {
                corrected.add(serie);
            }
        }

        if (autoCorrect && !corrected.isEmpty()) {
            serieDataRepository.saveAll(corrected);
            logger.info("Auto-corrected {} series out of {} scanned", corrected.size(), series.size());
        }

        return issues;
    }

    private boolean validateSingle(CuttingRequestSerieData serie, List<ValidationIssue> issues, boolean autoCorrect) {
        boolean corrected = false;
        corrected |= validateCoupe(serie, issues, autoCorrect);
        corrected |= validateMatelassage(serie, issues, autoCorrect);
        return corrected;
    }

    /**
     * New rule: for statusCoupe = 'Complete', ensure both dates exist using estimated duration.
     */
    private boolean validateCoupe(CuttingRequestSerieData serie, List<ValidationIssue> issues, boolean autoCorrect) {
        boolean corrected = false;
        String status = serie.getStatusCoupe();
        LocalDateTime debut = serie.getDateDebutCoupe();
        LocalDateTime fin = serie.getDateFinCoupe();

        if (!"Complete".equals(status)) {
            return false;
        }

        double estimatedMinutes = estimateCuttingMinutes(serie);

        // Rule: dateDebutCoupe not null, dateFinCoupe null → dateFinCoupe = dateDebutCoupe + estimated duration
        if (debut != null && fin == null) {
            LocalDateTime expected = debut.plusMinutes((long) estimatedMinutes);
            issues.add(new ValidationIssue(
                    serie.getSerie(), serie.getSequence(), "dateFinCoupe",
                    "MISSING_DATE_ESTIMATED", null, expected.toString(), autoCorrect));
            if (autoCorrect) {
                serie.setDateFinCoupe(expected);
                corrected = true;
            }
        }

        // Rule: dateFinCoupe not null, dateDebutCoupe null → dateDebutCoupe = dateFinCoupe - estimated duration
        if (fin != null && debut == null) {
            LocalDateTime expected = fin.minusMinutes((long) estimatedMinutes);
            if (expected.isAfter(fin)) expected = fin.minusMinutes(1);
            issues.add(new ValidationIssue(
                    serie.getSerie(), serie.getSequence(), "dateDebutCoupe",
                    "MISSING_DATE_ESTIMATED", null, expected.toString(), autoCorrect));
            if (autoCorrect) {
                serie.setDateDebutCoupe(expected);
                corrected = true;
            }
        }

        return corrected;
    }

    /**
     * New rule: for statusMatelassage = 'Complete', ensure both dates exist using estimated duration.
     */
    private boolean validateMatelassage(CuttingRequestSerieData serie, List<ValidationIssue> issues, boolean autoCorrect) {
        boolean corrected = false;
        String status = serie.getStatusMatelassage();
        LocalDateTime debut = serie.getDateDebutMatelassage();
        LocalDateTime fin = serie.getDateFinMatelassage();

        if (!"Complete".equals(status)) {
            return false;
        }

        double estimatedMinutes = estimateSpreadingMinutes(serie);

        // Rule: dateDebutMatelassage not null, dateFinMatelassage null → dateFinMatelassage = dateDebutMatelassage + estimated duration
        if (debut != null && fin == null) {
            LocalDateTime expected = debut.plusMinutes((long) estimatedMinutes);
            issues.add(new ValidationIssue(
                    serie.getSerie(), serie.getSequence(), "dateFinMatelassage",
                    "MISSING_DATE_ESTIMATED", null, expected.toString(), autoCorrect));
            if (autoCorrect) {
                serie.setDateFinMatelassage(expected);
                corrected = true;
            }
        }

        // Rule: dateFinMatelassage not null, dateDebutMatelassage null → dateDebutMatelassage = dateFinMatelassage - estimated duration
        if (fin != null && debut == null) {
            LocalDateTime expected = fin.minusMinutes((long) estimatedMinutes);
            if (expected.isAfter(fin)) expected = fin.minusMinutes(1);
            issues.add(new ValidationIssue(
                    serie.getSerie(), serie.getSequence(), "dateDebutMatelassage",
                    "MISSING_DATE_ESTIMATED", null, expected.toString(), autoCorrect));
            if (autoCorrect) {
                serie.setDateDebutMatelassage(expected);
                corrected = true;
            }
        }

        return corrected;
    }

    private double estimateCuttingMinutes(CuttingRequestSerieData serie) {
        String placement = serie.getPlacement();
        Double tempsDeCoupe = serie.getTempsDeCoupe();
        Integer nbrCouche = serie.getNbrCouche();
        String machineType = serie.getMachine();

        // Use CuttingTimeCalculator if placement is available
        if (placement != null && !placement.isEmpty()) {
            Map<String, CuttingTimeCalculator.TimingRow> timingMap = cuttingTimeCalculator.loadTimingMap(
                    Collections.singletonList(placement));
            double mins = cuttingTimeCalculator.resolveMinutes(placement, tempsDeCoupe, nbrCouche, machineType, timingMap);
            if (mins > 0) return mins;
        }

        // Fallback to serie's own tempsDeCoupe
        if (tempsDeCoupe != null && tempsDeCoupe > 0) {
            return tempsDeCoupe;
        }

        return 10.0; // default 10 minutes
    }

    private double estimateSpreadingMinutes(CuttingRequestSerieData serie) {
        Double longueur = serie.getLongueur();
        Integer nbrCouche = serie.getNbrCouche();
        double len = longueur != null ? longueur : 0;
        int layers = nbrCouche != null ? nbrCouche : 1;
        return (len * layers * COEF_SPREADING_PER_METRE) + COEF_SETUP_TIME;
    }

    // ======================== PUBLIC API ========================

    public Map<String, Object> validateStatuses(List<String> sequences, boolean autoCorrect) {
        List<ValidationIssue> issues;
        if (sequences == null || sequences.isEmpty()) {
            issues = validateAll();
        } else {
            issues = validateAndCorrect(sequences, autoCorrect);
        }
        Map<String, Object> result = getSummary(issues);
        result.put("issues", issues);
        result.put("autoCorrect", autoCorrect);
        return result;
    }

    public Map<String, Object> getSummary(List<ValidationIssue> issues) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIssues", issues.size());

        Map<String, Long> countsByType = issues.stream()
                .collect(Collectors.groupingBy(i -> i.issueType, Collectors.counting()));
        summary.put("countsByIssueType", countsByType);

        return summary;
    }

    // ======================== PASS-THROUGH PRODUCTION NORMALIZATION ========================

    /**
     * Correct floor "pass-through" data:
     * <ul>
     *   <li>if the next coupe starts on the same table, close the previous
     *       statusCoupe='In progress' serie at that next start time;</li>
     *   <li>same rule for matelassage;</li>
     *   <li>if every serie of a sequence is Complete in coupe, mark the parent
     *       sequenceStatus as COMPLETED.</li>
     * </ul>
     */
    @Transactional
    public Map<String, Integer> normalizeProductionProgress() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("coupeClosed", normalizeCoupePassThrough());
        result.put("matelassageClosed", normalizeMatelassagePassThrough());
        result.put("sequencesCompleted", autoCompleteSequencesFromSeries());
        return result;
    }

    /**
     * Backward-compatible endpoint name used by existing controls.
     */
    @Transactional
    public int normalizeCoupeOverlaps() {
        return normalizeCoupePassThrough();
    }

    @Transactional
    public int normalizeCoupePassThrough() {
        int fixed = 0;
        for (Object[] row : serieDataRepository.findCoupePassThroughClosures()) {
            String serie = (String) row[0];
            LocalDateTime nextStart = toLocalDateTime(row[1]);
            if (serie == null || nextStart == null) continue;
            fixed += serieDataRepository.closeCoupePassThrough(serie, nextStart);
        }
        if (fixed > 0) {
            logger.info("normalizeCoupePassThrough: closed {} 'In progress' series", fixed);
        }
        return fixed;
    }

    @Transactional
    public int normalizeMatelassagePassThrough() {
        int fixed = 0;
        for (Object[] row : serieDataRepository.findMatelassagePassThroughClosures()) {
            String serie = (String) row[0];
            LocalDateTime nextStart = toLocalDateTime(row[1]);
            if (serie == null || nextStart == null) continue;
            fixed += serieDataRepository.closeMatelassagePassThrough(serie, nextStart);
        }
        if (fixed > 0) {
            logger.info("normalizeMatelassagePassThrough: closed {} 'In progress' series", fixed);
        }
        return fixed;
    }

    /**
     * If every serie of a sequence has statusCoupe='Complete', close the parent
     * sequence so active engine/workbench queries stop pulling finished work.
     */
    @Transactional
    public int autoCompleteSequencesFromSeries() {
        List<String> nonCompletedSeqs = cuttingRequestRepository.findNonCompletedSequences();
        if (nonCompletedSeqs.isEmpty()) {
            return 0;
        }

        List<String> toComplete = new ArrayList<>();
        for (int i = 0; i < nonCompletedSeqs.size(); i += 2000) {
            List<String> batch = nonCompletedSeqs.subList(i, Math.min(i + 2000, nonCompletedSeqs.size()));
            toComplete.addAll(serieDataRepository.findSequencesWhereAllCoupeComplete(batch));
        }

        if (toComplete.isEmpty()) {
            return 0;
        }

        List<CuttingRequest> updated = new ArrayList<>();
        for (String seq : toComplete) {
            CuttingRequest cr = cuttingRequestRepository.findById(seq).orElse(null);
            if (cr != null && !isStatus(cr.getSequenceStatus(), "COMPLETED")) {
                cr.setSequenceStatus("COMPLETED");
                updated.add(cr);
            }
        }
        if (!updated.isEmpty()) {
            cuttingRequestRepository.saveAll(updated);
            logger.info("autoCompleteSequencesFromSeries: marked {} sequences as COMPLETED", updated.size());
        }
        return updated.size();
    }

    private boolean isStatus(String actual, String expected) {
        return actual != null && expected != null && expected.equalsIgnoreCase(actual.trim());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime();
        throw new IllegalArgumentException("Unsupported timestamp value: " + value.getClass().getName());
    }

    // ======================== SEQUENCE STATUS AUTO-CORRECTION ========================

    /**
     * Auto-correct sequenceStatus in CuttingRequest based on Work Order completion.
     * Logic:
     * 1. Find sequences where sequenceStatus is null or != 'COMPLETED'
     * 2. For each sequence, get its WOs from CuttingRequestPartNumber
     * 3. Check if ALL WOs have Order_Schedule.Status_Demande = 'C' (Completed)
     * 4. If yes, mark the sequence as 'COMPLETED'
     *
     * <p>Runs as a batch operation. Returns count of sequences corrected.</p>
     */
    @Transactional
    public int autoCorrectSequenceStatuses() {
        long startMs = System.currentTimeMillis();

        // 1. Find non-completed sequences
        List<String> nonCompletedSeqs = cuttingRequestRepository.findNonCompletedSequences();
        if (nonCompletedSeqs.isEmpty()) {
            return 0;
        }
        logger.info("Auto-correct sequenceStatus: {} non-completed sequences found", nonCompletedSeqs.size());

        // 2. Get all WOs per sequence (batch)
        Map<String, Set<String>> wosBySequence = new HashMap<>();
        for (int i = 0; i < nonCompletedSeqs.size(); i += 2000) {
            List<String> batch = nonCompletedSeqs.subList(i, Math.min(i + 2000, nonCompletedSeqs.size()));
            for (CuttingRequestPartNumberData pn : partNumberDataRepository.findBySequences(batch)) {
                String seq = pn.getCuttingRequest();
                String wo = pn.getWo();
                if (seq != null && wo != null && !wo.isEmpty()) {
                    wosBySequence.computeIfAbsent(seq, k -> new HashSet<>()).add(wo.trim());
                }
            }
        }

        if (wosBySequence.isEmpty()) {
            logger.info("Auto-correct sequenceStatus: no WOs found for non-completed sequences");
            return 0;
        }

        // 3. Collect all WO IDs
        Set<String> allWoIds = new HashSet<>();
        for (Set<String> wos : wosBySequence.values()) {
            allWoIds.addAll(wos);
        }
        logger.info("Auto-correct sequenceStatus: {} sequences have WOs, {} unique WO IDs",
                wosBySequence.size(), allWoIds.size());

        // 4. Find which WOs are completed (Status_Demande = 'C')
        Set<String> completedWoIds = new HashSet<>();
        List<Long> woIdList = new ArrayList<>();
        for (String woStr : allWoIds) {
            try {
                woIdList.add(Long.parseLong(woStr.trim()));
            } catch (NumberFormatException e) {
                logger.debug("Auto-correct sequenceStatus: non-numeric WO skipped: '{}'", woStr);
            }
        }
        logger.info("Auto-correct sequenceStatus: {} numeric WO IDs to check in OrderSchedule", woIdList.size());

        if (!woIdList.isEmpty()) {
            // Query Order_Schedule via JdbcTemplate using cmsDataSource (LEAR_qualite)
            JdbcTemplate cmsJdbcTemplate = new JdbcTemplate(cmsDataSource);
            int foundCount = 0;
            int completedCount = 0;
            int batchSize = 1000;
            for (int i = 0; i < woIdList.size(); i += batchSize) {
                List<Long> batch = woIdList.subList(i, Math.min(i + batchSize, woIdList.size()));
                String inClause = batch.stream().map(String::valueOf).collect(Collectors.joining(","));
                String sql = "SELECT ID_Demande, Status_Demande FROM dbo.Order_Schedule WHERE ID_Demande IN (" + inClause + ")";
                try {
                    List<Map<String, Object>> rows = cmsJdbcTemplate.queryForList(sql);
                    for (Map<String, Object> row : rows) {
                        foundCount++;
                        Object idObj = row.get("ID_Demande");
                        Object statusObj = row.get("Status_Demande");
                        String idStr = idObj != null ? idObj.toString() : null;
                        String status = statusObj != null ? statusObj.toString() : null;
                        if (idStr != null && "C".equals(status)) {
                            completedWoIds.add(idStr);
                            completedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Auto-correct sequenceStatus: JdbcTemplate query failed for batch {}-{}: {}",
                            i, Math.min(i + batchSize, woIdList.size()), e.getMessage());
                }
            }
            logger.info("Auto-correct sequenceStatus: found {} OrderSchedule records, {} with status 'C'",
                    foundCount, completedCount);
        }

        // 5. Determine which sequences have ALL WOs completed
        List<String> toComplete = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : wosBySequence.entrySet()) {
            String seq = entry.getKey();
            Set<String> wos = entry.getValue();
            if (wos.isEmpty()) continue;

            boolean allCompleted = true;
            int missingCount = 0;
            for (String wo : wos) {
                if (!completedWoIds.contains(wo)) {
                    allCompleted = false;
                    missingCount++;
                }
            }
            if (allCompleted) {
                toComplete.add(seq);
            } else if (missingCount > 0 && toComplete.isEmpty()) {
                // Log first incomplete sequence for debugging
                logger.debug("Auto-correct sequenceStatus: sequence {} has {}/{} WOs not completed",
                        seq, missingCount, wos.size());
            }
        }

        // 6. Update sequences
        if (!toComplete.isEmpty()) {
            for (String seq : toComplete) {
                CuttingRequest cr = cuttingRequestRepository.findById(seq).orElse(null);
                if (cr != null) {
                    cr.setSequenceStatus("COMPLETED");
                    cuttingRequestRepository.save(cr);
                }
            }
            logger.info("Auto-correct sequenceStatus: marked {} sequences as COMPLETED (took {} ms)",
                    toComplete.size(), System.currentTimeMillis() - startMs);
        } else {
            logger.info("Auto-correct sequenceStatus: no sequences fully completed by WOs (took {} ms)",
                    System.currentTimeMillis() - startMs);
        }

        return toComplete.size();
    }
}
