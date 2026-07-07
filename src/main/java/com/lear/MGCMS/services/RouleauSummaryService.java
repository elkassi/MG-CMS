package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.ScanRouleau;
import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.domain.StockStatusReport;
import com.lear.MGCMS.domain.dto.RouleauSummaryDto;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.SerieRouleauTempRepository;
import com.lear.MGCMS.repositories.StockStatusReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class RouleauSummaryService {

    @Autowired
    private StockStatusReportRepository stockStatusReportRepository;

    @Autowired
    private ScanRouleauRepository scanRouleauRepository;

    @Autowired
    private SerieRouleauTempRepository serieRouleauTempRepository;

    /**
     * Build a lookup map from ScanRouleau keyed by reftissu (normalized, without P prefix).
     * Since multiple ScanRouleau can share the same reftissu, we store a list.
     */
    private Map<String, List<ScanRouleau>> buildScanRouleauMap() {
        List<ScanRouleau> allScans = scanRouleauRepository.findAll();
        Map<String, List<ScanRouleau>> map = new HashMap<>();
        for (ScanRouleau sc : allScans) {
            if (sc.getReftissu() != null) {
                String key = sc.getReftissu().trim();
                // Normalize: remove P prefix if present
                if (key.toUpperCase().startsWith("P")) {
                    key = key.substring(1);
                }
                key = key.trim();
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(sc);
            }
        }
        return map;
    }

    /**
     * Build a set of all idRouleau values from SerieRouleauTemp (rolls currently in use on tables).
     * idRouleau in SerieRouleauTemp does NOT have "S" prefix.
     */
    private Map<String, SerieRouleauTemp> buildInUseMap() {
        Iterable<SerieRouleauTemp> all = serieRouleauTempRepository.findAll();
        Map<String, SerieRouleauTemp> map = new HashMap<>();
        for (SerieRouleauTemp srt : all) {
            if (srt.getIdRouleau() != null) {
                map.put(srt.getIdRouleau().trim(), srt);
            }
        }
        return map;
    }

    private List<StockStatusReport> cachedR100 = null;
    private long lastModifiedR100 = 0;

    @org.springframework.beans.factory.annotation.Value("${reportFolder.path}")
    private String reportFolder;

    private synchronized List<StockStatusReport> getR100Data() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(reportFolder + "\\R100.prn");
            java.io.File file = path.toFile();
            if (!file.exists()) {
                System.out.println("R100.prn not found at " + path.toAbsolutePath());
                return null; // Return null to trigger DB fallback
            }
            long currentModified = file.lastModified();
            if (cachedR100 != null && currentModified == lastModifiedR100) {
                return cachedR100;
            }
            List<StockStatusReport> arr = new ArrayList<>();
            try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(path)) {
                String line;
                String itemNumber = "", um = "", abc = "", site = "", location = "", ref = "", status = "";
                while ((line = reader.readLine()) != null) {
                    if (line.contains("--------------------------")
                            || line.contains("Item Number")
                            || line.contains("Page:")
                            || line.contains("3.6.1 Stock Status Report")
                            || line.contains("Lot/Serial")
                            || line.contains("End of Report") || line.contains("TANGIER-TRIM")
                            || line.contains("Output:") || line.contains("Batch ID:")
                            || line.contains("Report Submitted") || line.contains("To:") || line.contains("Summary/Detail:") || line.contains("Include Zero Quantity:")
                            || line.trim().isEmpty()) {
                        continue;
                    }
                    if (line.length() < 131) {
                        line = line + String.join("", java.util.Collections.nCopies(131 - line.length(), " "));
                    }
                    if (line.contains("MA10TR01")) {
                        itemNumber = line.substring(0, 26).trim();
                        um = line.substring(27, 29).trim();
                        abc = line.substring(30, 32).trim();
                        site = line.substring(34, 42).trim();
                    }
                    if (itemNumber.isEmpty()) {
                        continue;
                    }
                    if (!line.substring(80, 88).trim().isEmpty()) {
                        location = line.substring(80, 88).trim();
                    }
                    ref = line.substring(89, 107).trim();
                    String qtyStr = line.substring(108, 121).trim();
                    String statusStr = line.substring(122, 130).trim();
                    
                    if (ref.isEmpty() || qtyStr.isEmpty() || !location.toUpperCase().startsWith("T0") || !um.equalsIgnoreCase("MT")) {
                        continue;
                    }
                    
                    double qty = 0.0;
                    try {
                        qty = Double.parseDouble(qtyStr);
                    } catch (Exception e) {
                        continue;
                    }
                    
                    StockStatusReport report = new StockStatusReport();
                    report.setItemNumber(itemNumber);
                    report.setLocation(location);
                    report.setRef(ref);
                    report.setQtyOnHand(qty);
                    report.setStatus(statusStr);
                    report.setIsDeleted(false);
                    arr.add(report);
                }
            }
            cachedR100 = arr;
            lastModifiedR100 = currentModified;
            return cachedR100;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Page<StockStatusReport> getStockPageFromR100(int page, int size, String rollId, String itemNumber) {
        List<StockStatusReport> allData = getR100Data();
        if (allData == null) {
            return null; // Trigger DB fallback
        }
        
        boolean hasRollId = rollId != null && !rollId.trim().isEmpty();
        boolean hasItemNumber = itemNumber != null && !itemNumber.trim().isEmpty();
        
        List<StockStatusReport> filtered = allData.stream().filter(st -> {
            boolean match = true;
            if (hasRollId && (st.getRef() == null || !st.getRef().toLowerCase().contains(rollId.toLowerCase()))) {
                match = false;
            }
            if (hasItemNumber && (st.getItemNumber() == null || !st.getItemNumber().toLowerCase().contains(itemNumber.toLowerCase()))) {
                match = false;
            }
            return match;
        }).collect(Collectors.toList());
        
        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<StockStatusReport> pageContent = filtered.subList(start, end);
        
        return new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());
    }

    public Page<RouleauSummaryDto> getRouleauSummary(int page, int size, String rollId, String itemNumber) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StockStatusReport> stockPage = getStockPageFromR100(page, size, rollId, itemNumber);
        
        // DB Fallback if R100.prn is missing
        if (stockPage == null) {
            System.out.println("Falling back to StockStatusReportRepository because R100.prn is missing or could not be read.");
            if (rollId != null && !rollId.trim().isEmpty() && itemNumber != null && !itemNumber.trim().isEmpty()) {
                stockPage = stockStatusReportRepository.findByRefContainingIgnoreCaseAndItemNumberContainingIgnoreCaseAndIsDeletedFalse(rollId, itemNumber, pageable);
            } else if (rollId != null && !rollId.trim().isEmpty()) {
                stockPage = stockStatusReportRepository.findByRefContainingIgnoreCaseAndIsDeletedFalse(rollId, pageable);
            } else if (itemNumber != null && !itemNumber.trim().isEmpty()) {
                stockPage = stockStatusReportRepository.findByItemNumberContainingIgnoreCaseAndIsDeletedFalse(itemNumber, pageable);
            } else {
                stockPage = stockStatusReportRepository.findByIsDeletedFalse(pageable);
            }
        }

        // Build lookup maps
        Map<String, List<ScanRouleau>> scanMap = buildScanRouleauMap();
        Map<String, SerieRouleauTemp> inUseMap = buildInUseMap();

        List<RouleauSummaryDto> dtos = new ArrayList<>();

        for (StockStatusReport st : stockPage.getContent()) {
            RouleauSummaryDto dto = new RouleauSummaryDto();
            dto.setRollId(st.getRef());
            dto.setItemNumber(st.getItemNumber());
            dto.setQtyMeters(st.getQtyOnHand());
            dto.setR100Location(st.getLocation());

            // --- Join with ScanRouleau ---
            String normalizedItemNumber = st.getItemNumber() != null ? st.getItemNumber().trim() : "";
            List<ScanRouleau> matchingScans = scanMap.get(normalizedItemNumber);
            if (matchingScans != null && !matchingScans.isEmpty()) {
                // Try to find best match by metrage/qtyOnHand
                ScanRouleau bestMatch = null;
                for (ScanRouleau sc : matchingScans) {
                    Double scanQty = sc.getMetrage();
                    if (scanQty == null) {
                        try {
                            scanQty = Double.parseDouble(sc.getQuantite());
                        } catch (Exception e) {
                            scanQty = null;
                        }
                    }
                    if (scanQty != null && st.getQtyOnHand() != null 
                            && Math.abs(scanQty - st.getQtyOnHand()) < 0.01) {
                        bestMatch = sc;
                        break;
                    }
                }
                if (bestMatch == null) {
                    bestMatch = matchingScans.get(0);
                }
                
                dto.setSerialId(bestMatch.getSerialId());
                dto.setLot(bestMatch.getLot());
                dto.setEmplacement(bestMatch.getEmplacement());
            }

            // --- Determine Location Type ---
            String serialIdForLookup = dto.getSerialId();
            if (serialIdForLookup != null && serialIdForLookup.toUpperCase().startsWith("S")) {
                serialIdForLookup = serialIdForLookup.substring(1);
            }
            
            SerieRouleauTemp inUseEntry = null;
            if (serialIdForLookup != null) {
                inUseEntry = inUseMap.get(serialIdForLookup);
            }

            if (inUseEntry != null) {
                dto.setLocationType("In use");
                // Show which table it's on
                if (inUseEntry.getTableMatelassage() != null) {
                    dto.setEmplacement("Table: " + inUseEntry.getTableMatelassage());
                }
            } else if (!Boolean.TRUE.equals(st.getIsDeleted()) &&
                    (st.getStatus() != null && 
                     (st.getStatus().equalsIgnoreCase("AVAIL") || st.getStatus().equalsIgnoreCase("AVAIL2")))) {
                dto.setLocationType("In stock");
            } else {
                dto.setLocationType("Not in stock");
                // User requested: "if the Location Type not instock then he will be in Emplacement"
                // For "INSPECT" and "TRANSI" etc., show it in the Emplacement column
                dto.setEmplacement(st.getStatus());
            }

            dtos.add(dto);
        }

        return new PageImpl<>(dtos, pageable, stockPage.getTotalElements());
    }
}
