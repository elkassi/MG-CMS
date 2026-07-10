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
import java.time.LocalDateTime;

@Service
public class RouleauSummaryService {

    @Autowired
    private StockStatusReportRepository stockStatusReportRepository;

    @Autowired
    private ScanRouleauRepository scanRouleauRepository;

    @Autowired
    private SerieRouleauTempRepository serieRouleauTempRepository;

    @Autowired(required = false)
    private com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieRouleauDataRepository cuttingRepo;

    @Autowired(required = false)
    private com.lear.pls.repositories.ProdTicketRepository prodTicketRepo;

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
                    report.setUm(um);
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

    private List<StockStatusReport> getFilteredStockListFromR100(String rollId, String itemNumber) {
        List<StockStatusReport> allData = getR100Data();
        if (allData == null) {
            return null;
        }
        
        boolean hasRollId = rollId != null && !rollId.trim().isEmpty();
        boolean hasItemNumber = itemNumber != null && !itemNumber.trim().isEmpty();
        
        return allData.stream().filter(st -> {
            if (st.getUm() == null || !st.getUm().trim().equalsIgnoreCase("MT")) return false;
            boolean match = true;
            if (hasRollId && (st.getRef() == null || !st.getRef().toLowerCase().contains(rollId.toLowerCase()))) {
                match = false;
            }
            if (hasItemNumber && (st.getItemNumber() == null || !st.getItemNumber().toLowerCase().contains(itemNumber.toLowerCase()))) {
                match = false;
            }
            return match;
        }).collect(Collectors.toList());
    }

    public Page<RouleauSummaryDto> getRouleauSummary(int page, int size, String rollId, String itemNumber, String statusFilter) {
        long startTime = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(page, size);
        List<StockStatusReport> stockList = getFilteredStockListFromR100(rollId, itemNumber);
        
        // DB Fallback if R100.prn is missing
        if (stockList == null) {
            System.out.println("Falling back to DB");
            Page<StockStatusReport> fallbackPage;
            if (rollId != null && !rollId.trim().isEmpty() && itemNumber != null && !itemNumber.trim().isEmpty()) {
                fallbackPage = stockStatusReportRepository.findByRefContainingIgnoreCaseAndItemNumberContainingIgnoreCaseAndUmAndIsDeletedFalse(rollId, itemNumber, "MT", PageRequest.of(0, 10000));
            } else if (rollId != null && !rollId.trim().isEmpty()) {
                fallbackPage = stockStatusReportRepository.findByRefContainingIgnoreCaseAndUmAndIsDeletedFalse(rollId, "MT", PageRequest.of(0, 10000));
            } else if (itemNumber != null && !itemNumber.trim().isEmpty()) {
                fallbackPage = stockStatusReportRepository.findByItemNumberContainingIgnoreCaseAndUmAndIsDeletedFalse(itemNumber, "MT", PageRequest.of(0, 10000));
            } else {
                fallbackPage = stockStatusReportRepository.findByUmAndIsDeletedFalse("MT", PageRequest.of(0, 10000));
            }
            stockList = fallbackPage.getContent();
        }
        System.out.println("Time to load stockList: " + (System.currentTimeMillis() - startTime) + "ms. Size: " + stockList.size());

        long t1 = System.currentTimeMillis();
        // Build lookup maps
        Map<String, List<ScanRouleau>> scanMap = buildScanRouleauMap();
        Map<String, SerieRouleauTemp> inUseMap = buildInUseMap();
        System.out.println("Time to build lookups: " + (System.currentTimeMillis() - t1) + "ms");

        boolean filterIsAll = (statusFilter == null || statusFilter.trim().isEmpty() || statusFilter.equalsIgnoreCase("All"));
        
        // If the filter is ALL, we can slice the stockList FIRST and only process those 100 items!
        List<StockStatusReport> itemsToProcess = stockList;
        if (filterIsAll) {
            int start = Math.min(page * size, stockList.size());
            int end = Math.min(start + size, stockList.size());
            itemsToProcess = stockList.subList(start, end);
        }

        long t2 = System.currentTimeMillis();
        // Collect IDs to fetch consumption using HashSet for O(1) deduplication
        Set<String> idRouleauxSet = new HashSet<>();
        for (StockStatusReport st : itemsToProcess) {
            String sid = st.getRef();
            if (sid != null) {
                if (sid.toUpperCase().startsWith("S")) {
                    sid = sid.substring(1);
                }
                idRouleauxSet.add(sid);
                idRouleauxSet.add("6" + sid);
                idRouleauxSet.add("S" + sid);
                idRouleauxSet.add("S6" + sid);
            }
        }
        List<String> idRouleaux = new ArrayList<>(idRouleauxSet);
        System.out.println("Time to deduplicate IDs: " + (System.currentTimeMillis() - t2) + "ms. Process size: " + itemsToProcess.size());

        long t3 = System.currentTimeMillis();
        // Partition ID list for IN clause limit (max 2100 in SQL Server)
        List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData> cuttingDataList = new ArrayList<>();
        List<com.lear.pls.domain.ProdTicket> prodTickets = new ArrayList<>();
        
        if (!idRouleaux.isEmpty()) {
            for (int i = 0; i < idRouleaux.size(); i += 2000) {
                List<String> partition = idRouleaux.subList(i, Math.min(i + 2000, idRouleaux.size()));
                if (cuttingRepo != null) {
                    try { cuttingDataList.addAll(cuttingRepo.findByIdRouleaux(partition)); } catch(Exception e) {}
                }
                if (prodTicketRepo != null) {
                    try { prodTickets.addAll(prodTicketRepo.findObjIdRouleauInThis(partition)); } catch(Exception e) {}
                }
            }
        }
        System.out.println("Time to fetch DB records: " + (System.currentTimeMillis() - t3) + "ms. CuttingData size: " + cuttingDataList.size() + ", ProdTickets size: " + prodTickets.size());

        long t4 = System.currentTimeMillis();
        // Pre-build lookup maps to prevent O(N*M) performance issues
        Map<String, List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData>> cuttingMap = new HashMap<>();
        for(com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData c : cuttingDataList) {
            String cId = c.getIdRouleau();
            if (cId != null) {
                if (cId.toUpperCase().startsWith("S")) cId = cId.substring(1);
                if (cId.startsWith("6")) cId = cId.substring(1);
                cuttingMap.computeIfAbsent(cId, k -> new ArrayList<>()).add(c);
            }
        }
        
        Map<String, List<com.lear.pls.domain.ProdTicket>> ticketMap = new HashMap<>();
        for(com.lear.pls.domain.ProdTicket p : prodTickets) {
            String pId = p.getLabelId();
            if (pId != null) {
                if (pId.toUpperCase().startsWith("S")) pId = pId.substring(1);
                if (pId.startsWith("6")) pId = pId.substring(1);
                ticketMap.computeIfAbsent(pId, k -> new ArrayList<>()).add(p);
            }
        }
        System.out.println("Time to build DB maps: " + (System.currentTimeMillis() - t4) + "ms");

        long t5 = System.currentTimeMillis();
        List<RouleauSummaryDto> dtos = new ArrayList<>();

        for (StockStatusReport st : itemsToProcess) {
            RouleauSummaryDto dto = new RouleauSummaryDto();
            dto.setRollId(st.getRef());
            dto.setItemNumber(st.getItemNumber());
            dto.setR100Location(st.getLocation());
            dto.setIsFullyConsumed(false);
            
            Double currentQty = st.getQtyOnHand() != null ? st.getQtyOnHand() : 0.0;
            String r100Status = st.getStatus() != null ? st.getStatus().trim() : "";
            LocalDateTime latestEventTime = LocalDateTime.MIN;
            String currentEmplacement = null;

            // --- Join with ScanRouleau ---
            String normalizedItemNumber = st.getItemNumber() != null ? st.getItemNumber().trim() : "";
            List<ScanRouleau> matchingScans = scanMap.get(normalizedItemNumber);
            ScanRouleau bestMatch = null;
            if (matchingScans != null && !matchingScans.isEmpty()) {
                for (ScanRouleau sc : matchingScans) {
                    Double scanQty = sc.getMetrage();
                    if (scanQty == null) {
                        try { scanQty = Double.parseDouble(sc.getQuantite()); } catch (Exception e) {}
                    }
                    if (scanQty != null && st.getQtyOnHand() != null && Math.abs(scanQty - st.getQtyOnHand()) < 0.01) {
                        bestMatch = sc;
                        break;
                    }
                }
                if (bestMatch == null) {
                    bestMatch = matchingScans.get(0);
                }
                
                String scanSerial = bestMatch.getSerialId();
                if (scanSerial != null && scanSerial.toUpperCase().startsWith("S")) {
                    dto.setSerialId(scanSerial);
                }
                dto.setLot(bestMatch.getLot());
                
                if (bestMatch.getDate() != null && bestMatch.getDate().isAfter(latestEventTime)) {
                    latestEventTime = bestMatch.getDate();
                    currentEmplacement = bestMatch.getEmplacement();
                }
            }

            // --- Join with SerieRouleauTemp (Active on table) ---
            String serialIdForLookup = dto.getSerialId();
            if (serialIdForLookup == null) {
                serialIdForLookup = st.getRef();
            }
            if (serialIdForLookup != null && serialIdForLookup.toUpperCase().startsWith("S")) {
                serialIdForLookup = serialIdForLookup.substring(1);
            }
            
            SerieRouleauTemp inUseEntry = serialIdForLookup != null ? inUseMap.get(serialIdForLookup) : null;
            if (inUseEntry != null) {
                if (inUseEntry.getDate() != null && inUseEntry.getDate().isAfter(latestEventTime)) {
                    latestEventTime = inUseEntry.getDate();
                } else if (latestEventTime == LocalDateTime.MIN) {
                    latestEventTime = LocalDateTime.now();
                }
                if (inUseEntry.getTableMatelassage() != null) {
                    currentEmplacement = "Table: " + inUseEntry.getTableMatelassage();
                }
            }

            // --- Join with Consumption Data ---
            List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData> rollCuttings = new ArrayList<>();
            List<com.lear.pls.domain.ProdTicket> rollTickets = new ArrayList<>();
            
            if (serialIdForLookup != null) {
                String lookupKey = serialIdForLookup;
                if (lookupKey.startsWith("6")) lookupKey = lookupKey.substring(1);
                
                List<com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData> listC = cuttingMap.get(lookupKey);
                if (listC != null) {
                    rollCuttings = new ArrayList<>(listC);
                    rollCuttings.sort((c1, c2) -> {
                        if (c1.getCreatedAt() == null && c2.getCreatedAt() == null) return 0;
                        if (c1.getCreatedAt() == null) return 1;
                        if (c2.getCreatedAt() == null) return -1;
                        return c2.getCreatedAt().compareTo(c1.getCreatedAt());
                    });
                }
                
                List<com.lear.pls.domain.ProdTicket> listP = ticketMap.get(lookupKey);
                if (listP != null) {
                    rollTickets = new ArrayList<>(listP);
                    rollTickets.sort((p1, p2) -> {
                        if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                        if (p1.getCreatedAt() == null) return 1;
                        if (p2.getCreatedAt() == null) return -1;
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                    });
                }
            }
                
            LocalDateTime latestCutting = rollCuttings.isEmpty() ? null : rollCuttings.get(0).getCreatedAt();
            LocalDateTime latestTicket = rollTickets.isEmpty() ? null : rollTickets.get(0).getCreatedAt();
            
            boolean useTicketAsLatest = false;
            if (latestTicket != null && latestCutting != null) {
                useTicketAsLatest = latestTicket.isAfter(latestCutting);
            } else if (latestTicket != null) {
                useTicketAsLatest = true;
            }
            
            boolean isFullyConsumedFlag = false;
            
            if (useTicketAsLatest) {
                com.lear.pls.domain.ProdTicket latest = rollTickets.get(0);
                if (latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(latestEventTime)) {
                    latestEventTime = latest.getCreatedAt();
                    currentQty = latest.getQuantity() != null ? latest.getQuantity() : 0.0;
                }
                if (currentQty <= 0) {
                    isFullyConsumedFlag = true;
                }
            } else if (!rollCuttings.isEmpty()) {
                com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData latest = rollCuttings.get(0);
                if (latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(latestEventTime)) {
                    latestEventTime = latest.getCreatedAt();
                    currentQty = latest.getRetour() != null ? latest.getRetour() : 0.0;
                    if (latest.getLocation() != null && !latest.getLocation().isEmpty()) {
                        currentEmplacement = latest.getLocation();
                    }
                }
                if (Boolean.FALSE.equals(latest.getConfirmRetour()) || currentQty <= 0) {
                    isFullyConsumedFlag = true;
                }
            }
            
            String finalStatus = "In stock";
            if (isFullyConsumedFlag || currentQty <= 0) {
                finalStatus = "Consommé";
                dto.setIsFullyConsumed(true);
                
                currentQty = 0.0;
                if (!rollCuttings.isEmpty()) {
                    Double exc = rollCuttings.get(0).getExcess();
                    if (exc != null) {
                        currentQty = exc;
                    }
                }
            } else if (latestEventTime.isAfter(LocalDateTime.MIN)) {
                finalStatus = "In production";
            } else {
                if (!r100Status.equalsIgnoreCase("AVAIL") && !r100Status.equalsIgnoreCase("AVAIL2")) {
                    finalStatus = "Blocked";
                }
            }
            
            if (finalStatus.equals("In stock") || finalStatus.equals("Blocked")) {
                currentEmplacement = null;
            } else {
                dto.setR100Location(null);
            }
            
            dto.setQuantity(Math.round(currentQty * 1000.0) / 1000.0);
            dto.setEmplacement(currentEmplacement);
            dto.setStatus(finalStatus);
            
            if (filterIsAll || statusFilter.equalsIgnoreCase(finalStatus)) {
                dtos.add(dto);
            }
        }
        System.out.println("Time to map results: " + (System.currentTimeMillis() - t5) + "ms. Total DTOs: " + dtos.size());
        
        long t6 = System.currentTimeMillis();
        List<RouleauSummaryDto> pagedDtos;
        if (filterIsAll) {
            pagedDtos = dtos;
        } else {
            int start = Math.min(page * size, dtos.size());
            int end = Math.min(start + size, dtos.size());
            pagedDtos = dtos.subList(start, end);
        }

        PageImpl<RouleauSummaryDto> result = new PageImpl<>(pagedDtos, pageable, filterIsAll ? stockList.size() : dtos.size());
        System.out.println("Total Time: " + (System.currentTimeMillis() - startTime) + "ms");
        return result;
    }
}
