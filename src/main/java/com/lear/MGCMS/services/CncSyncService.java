package com.lear.MGCMS.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lear.MGCMS.domain.*;
import com.lear.MGCMS.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CncSyncService {

    private static final Logger log = LoggerFactory.getLogger(CncSyncService.class);

    @Autowired
    private MachineCncRepository machineCncRepo;

    @Autowired
    private ProgramCNCRepository programCNCRepo;

    @Autowired
    private ProgrammeDistributionRepository programmeDistributionRepo;

    @Autowired
    private CncMachineReportRepository reportRepo;

    @Autowired
    private CncMachineReportPieceRepository pieceRepo;

    @Autowired
    private CodeDefautRepository codeDefautRepo;

    @Autowired
    private CodeScrapRepository codeScrapRepo;

    private final ObjectMapper mapper;

    public CncSyncService() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    /**
     * Export reference data (machines, programs, distributions) as JSON
     * for CMS-CNC to import.
     */
    public byte[] exportForCnc() throws IOException {
        Map<String, Object> export = new HashMap<>();
        export.put("exportDate", LocalDateTime.now().toString());

        // Machines
        List<Map<String, Object>> machines = new ArrayList<>();
        for (MachineCnc m : machineCncRepo.findAll()) {
            Map<String, Object> mMap = new HashMap<>();
            mMap.put("id", m.getId());
            mMap.put("name", m.getName());
            machines.add(mMap);
        }
        export.put("machines", machines);

        // Programs
        List<Map<String, Object>> programs = new ArrayList<>();
        for (ProgramCNC p : programCNCRepo.findAll()) {
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("id", p.getId());
            pMap.put("partNumber", p.getPartNumber());
            pMap.put("version", p.getVersion());
            pMap.put("pattern", p.getPattern());
            pMap.put("programNumber", p.getProgramNumber());
            pMap.put("casette", p.getCasette());
            pMap.put("code1", p.getCode1());
            pMap.put("type", p.getType());
            pMap.put("profil", p.getProfil());
            programs.add(pMap);
        }
        export.put("programs", programs);

        // Distributions
        List<Map<String, Object>> distributions = new ArrayList<>();
        for (ProgrammeDistribution d : programmeDistributionRepo.findAll()) {
            Map<String, Object> dMap = new HashMap<>();
            dMap.put("id", d.getId());
            dMap.put("machineCncId", d.getMachine() != null ? d.getMachine().getId() : null);
            dMap.put("programmeNumber", d.getProgrammeNumber());
            distributions.add(dMap);
        }
        export.put("distributions", distributions);

        // Codes Defaut (CNC-specific)
        List<Map<String, Object>> codesDefaut = new ArrayList<>();
        for (CodeDefaut cd : codeDefautRepo.findAllCNC()) {
            Map<String, Object> cdMap = new HashMap<>();
            cdMap.put("code", cd.getCode());
            cdMap.put("description", cd.getDescription());
            cdMap.put("departement", cd.getDepartement());
            cdMap.put("type", cd.getType());
            cdMap.put("active", cd.getActive());
            codesDefaut.add(cdMap);
        }
        export.put("codesDefaut", codesDefaut);

        // Codes Scrap (CNC-specific)
        List<Map<String, Object>> codesScrap = new ArrayList<>();
        for (CodeScrap cs : codeScrapRepo.findAllCNC()) {
            Map<String, Object> csMap = new HashMap<>();
            csMap.put("code", cs.getCode());
            csMap.put("description", cs.getDescription());
            csMap.put("departement", cs.getDepartement());
            csMap.put("type", cs.getType());
            codesScrap.add(csMap);
        }
        export.put("codesScrap", codesScrap);

        log.info("CNC Export: {} machines, {} programs, {} distributions, {} codesDefaut, {} codesScrap",
                machines.size(), programs.size(), distributions.size(), codesDefaut.size(), codesScrap.size());

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export);
    }

    /**
     * Import session data exported from CMS-CNC.
     * Expected JSON structure from CMS-CNC /api/sync/export:
     * {
     *   "exportDate": "...",
     *   "sessionCount": N,
     *   "sessions": [
     *     {
     *       "id": ...,
     *       "boxId": "...",
     *       "programNumber": "...",
     *       "partNumberImp": "...",
     *       "operator": "...",
     *       "productionOperator": "...",
     *       "productionStatus": "...",
     *       "quantiteImp": ...,
     *       "machineCnc": { "id": ..., "name": "..." },
     *       "shiftNumber": ...,
     *       "shiftDate": "...",
     *       "startProductionDate": "...",
     *       "endProductionDate": "...",
     *       "createdAt": "...",
     *       "workPieces": [...],   // may or may not be serialized
     *       "anomalies": [...]
     *     }
     *   ]
     * }
     */
    @Transactional
    public Map<String, Object> importFromCnc(MultipartFile file, String importedBy) throws IOException {
        Map<String, Object> data = mapper.readValue(file.getInputStream(),
                new TypeReference<Map<String, Object>>() {});

        int imported = 0;
        int skipped = 0;
        int totalPiecesImported = 0;
        List<String> errors = new ArrayList<>();

        List<Map<String, Object>> sessions = (List<Map<String, Object>>) data.get("sessions");
        if (sessions == null) {
            throw new IOException("Invalid file: no 'sessions' key found");
        }

        for (Map<String, Object> s : sessions) {
            try {
                Long sourceId = toLong(s.get("id"));

                // Skip if already imported
                if (sourceId != null && reportRepo.findBySourceSessionId(sourceId).isPresent()) {
                    skipped++;
                    continue;
                }

                CncMachineReport report = new CncMachineReport();
                report.setSourceSessionId(sourceId);
                report.setBoxId((String) s.get("boxId"));
                report.setProgramNumber((String) s.get("programNumber"));
                report.setPartNumber((String) s.get("partNumberImp"));
                report.setOperator((String) s.get("operator"));
                report.setProductionOperator((String) s.get("productionOperator"));
                report.setProductionStatus((String) s.get("productionStatus"));
                report.setQuantiteImp(toInt(s.get("quantiteImp")));
                report.setShiftNumber(toInt(s.get("shiftNumber")));
                report.setShiftDate((String) s.get("shiftDate"));
                report.setImportedAt(LocalDateTime.now());
                report.setImportedBy(importedBy);

                // Machine name
                Map<String, Object> machine = (Map<String, Object>) s.get("machineCnc");
                if (machine != null) {
                    report.setMachineName((String) machine.get("name"));
                }

                // Dates
                report.setStartProductionDate(parseDateTime(s.get("startProductionDate")));
                report.setEndProductionDate(parseDateTime(s.get("endProductionDate")));
                report.setSessionCreatedAt(parseDateTime(s.get("createdAt")));

                // Count pieces from workPieces if available
                List<Map<String, Object>> pieces = (List<Map<String, Object>>) s.get("workPieces");
                int totalPieces = 0, okPieces = 0, defautPieces = 0, scrapPieces = 0;
                if (pieces != null) {
                    totalPieces = pieces.size();
                    for (Map<String, Object> p : pieces) {
                        String qs = (String) p.get("qualityStatus");
                        if ("OK".equals(qs)) okPieces++;
                        else if ("DEFAUT".equals(qs)) defautPieces++;
                        else if ("SCRAP".equals(qs)) scrapPieces++;
                    }
                }
                report.setTotalPieces(totalPieces);
                report.setOkPieces(okPieces);
                report.setDefautPieces(defautPieces);
                report.setScrapPieces(scrapPieces);

                CncMachineReport saved = reportRepo.save(report);

                // Save individual pieces
                if (pieces != null) {
                    for (Map<String, Object> p : pieces) {
                        CncMachineReportPiece piece = new CncMachineReportPiece();
                        piece.setReport(saved);
                        piece.setProgramNumber((String) p.get("programNumber"));
                        piece.setStatus((String) p.get("status"));
                        piece.setQualityStatus((String) p.get("qualityStatus"));
                        piece.setCodeDefaut((String) p.get("codeDefaut"));
                        piece.setCodeScrap((String) p.get("codeScrap"));
                        piece.setQualityComment((String) p.get("qualityComment"));
                        piece.setOperatorUsername((String) p.get("operatorUsername"));
                        piece.setImageCount(toInt(p.get("imageCount")));
                        piece.setStartDate(parseDateTime(p.get("startDate")));
                        piece.setEndDate(parseDateTime(p.get("endDate")));
                        piece.setSourcePieceId(toLong(p.get("id")));
                        pieceRepo.save(piece);
                    }
                }

                imported++;
                totalPiecesImported += totalPieces;
            } catch (Exception e) {
                String boxId = (String) s.get("boxId");
                errors.add("Session " + boxId + ": " + e.getMessage());
                log.warn("Error importing session {}: {}", s.get("boxId"), e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("totalPieces", totalPiecesImported);
        result.put("errors", errors);
        log.info("CNC Import: {} imported, {} skipped, {} errors", imported, skipped, errors.size());
        return result;
    }

    /**
     * Find all reports with pagination and filters (same pattern as other services).
     */
    public Page<CncMachineReport> findAllReports(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String sortProp = sortArr[0];
        Sort.Direction direction = sortArr.length > 1 && "asc".equalsIgnoreCase(sortArr[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(new Sort.Order(direction, sortProp).ignoreCase());

        Specification<CncMachineReport> spec = buildSpecification(filters);
        return reportRepo.findAll(spec, PageRequest.of(page, size, sortObj));
    }

    /**
     * Find all report pieces for a given report.
     */
    public List<CncMachineReportPiece> findPiecesByReport(Long reportId) {
        return pieceRepo.findByReportId(reportId);
    }

    /**
     * Find all pieces with pagination and filters.
     */
    public Page<CncMachineReportPiece> findAllPieces(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String sortProp = sortArr[0];
        Sort.Direction direction = sortArr.length > 1 && "asc".equalsIgnoreCase(sortArr[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(new Sort.Order(direction, sortProp).ignoreCase());

        Specification<CncMachineReportPiece> spec = buildPieceSpecification(filters);
        return pieceRepo.findAll(spec, PageRequest.of(page, size, sortObj));
    }

    private Specification<CncMachineReport> buildSpecification(Map<String, String> filters) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String[] strArr = entry.getKey().split("\\.");
                if (strArr.length >= 2) {
                    Path<String> path = root.get(strArr[1]);
                    for (int i = 2; i < strArr.length; i++) {
                        path = path.get(strArr[i]);
                    }
                    if (path.getJavaType().equals(String.class)) {
                        if (entry.getKey().startsWith("startWith.")) {
                            predicates.add(builder.like(path.as(String.class), entry.getValue() + "%"));
                        } else if (entry.getKey().startsWith("contains.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
                        } else if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(String.class), entry.getValue()));
                        }
                    } else if (path.getJavaType().equals(LocalDateTime.class)) {
                        if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(Integer.class)) {
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        }
                    }
                }
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<CncMachineReportPiece> buildPieceSpecification(Map<String, String> filters) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String[] strArr = entry.getKey().split("\\.");
                if (strArr.length >= 2) {
                    Path<String> path = root.get(strArr[1]);
                    for (int i = 2; i < strArr.length; i++) {
                        path = path.get(strArr[i]);
                    }
                    if (path.getJavaType().equals(String.class)) {
                        if (entry.getKey().startsWith("startWith.")) {
                            predicates.add(builder.like(path.as(String.class), entry.getValue() + "%"));
                        } else if (entry.getKey().startsWith("contains.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
                        } else if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(String.class), entry.getValue()));
                        }
                    } else if (path.getJavaType().equals(LocalDateTime.class)) {
                        if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        }
                    }
                }
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime parseDateTime(Object val) {
        if (val == null) return null;
        try {
            String s = val.toString().replace(" ", "T");
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
