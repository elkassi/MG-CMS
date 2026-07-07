package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData;
import com.lear.MGCMS.domain.StockStatusReport;
import com.lear.MGCMS.repositories.StockStatusReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StockStatusReportService {

    @Autowired
    private StockStatusReportRepository repository;

    /** Folder holding R100.prn (network share in prod, local copy in dev). */
    @Value("${reportFolder.path}")
    private String reportFolder;

    public StockStatusReport save(StockStatusReport stockStatusReport) {
        return repository.save(stockStatusReport);
    }

    public void saveAll(List<StockStatusReport> batchList) {
        repository.saveAll(batchList);
    }

    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

    public Page<StockStatusReport> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

        Specification<StockStatusReport> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add filters based on the key-value pairs in the 'filters' map
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                System.out.println(entry.getKey() + " : "+ entry.getValue());
                String[] strArr = entry.getKey().split("\\.");

                if(strArr.length >= 2) {
                    Path<String> path = root.get(strArr[1]);
                    for(int i = 2; i < strArr.length; i++) {
                        path = path.get(strArr[i]);
                    }

                    // Handle different data types
                    if (path.getJavaType().equals(String.class)) {
                        System.out.println("String");
                        if (entry.getKey().startsWith("startWith.")) {
                            predicates.add(builder.like(path.as(String.class), entry.getValue() + "%"));
                        } else if (entry.getKey().startsWith("endWith.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue()));
                        } else if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(String.class), entry.getValue()));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(String.class), entry.getValue()));
                        } else if (entry.getKey().startsWith("contains.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
                        }
                    } else if (path.getJavaType().equals(Boolean.class)) {
                        System.out.println("Boolean");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Boolean.class), Boolean.parseBoolean(entry.getValue())));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(Boolean.class), Boolean.parseBoolean(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(LocalDate.class)) {
                        System.out.println("LocalDate");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        }
//                        else if (entry.getKey().startsWith("startWith.")) {
//                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS".substring(0, entry.getValue().length())));
//                            Integer lg = entry.getValue().length() ;
//                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
//                            switch(lg) {
//                            	case 1 :
//                            		endDateTime = startDateTime.plusYears(1000).minusNanos(1);break;
//                            	case 2 :
//                            		endDateTime = startDateTime.plusYears(100).minusNanos(1);break;
//                            	case 3:
//                            		endDateTime = startDateTime.plusYears(10).minusNanos(1);break;
//
//                            }
//                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
//                        }
                    } else if (path.getJavaType().equals(LocalDateTime.class)) {
                        System.out.println("LocalDateTime");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("startWith.")) {
                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue());
                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
                        }
                    } else if (path.getJavaType().equals(Integer.class)) {
                        // Handle Integer conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(Double.class)) {
                        // Handle Double conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(Long.class)) {
                        // Handle Long conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Long.class), Long.parseLong(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Long.class), Long.parseLong(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Long.class), Long.parseLong(entry.getValue())));
                        }
                    }

                    if(entry.getKey().startsWith("isNull.")) {
                        predicates.add(builder.isNull(path));
                    } else if(entry.getKey().startsWith("isNotNull.")) {
                        predicates.add(builder.isNotNull(path));
                    }

                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
    }

    public List<StockStatusReport> getCurrentStock(List<String> refTissus) {
        List<StockStatusReport> arr = new ArrayList<>();
        java.nio.file.Path path = java.nio.file.Paths.get(reportFolder + "\\R100.prn");
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy");
        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(path)) {
            String line;
            String itemNumber = "", um = "", abc = "", site = "", location = "", ref = "", status = "";
            java.time.LocalDate date = null;
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
                if (!line.substring(43, 51).trim().isEmpty() && line.substring(43, 51).trim().contains("/")) {
                    try {
                        date = java.time.LocalDate.parse(line.substring(43, 51).trim(), dateFormatter);
                    } catch (Exception e) {
                        date = null;
                    }
                }
                if (!line.substring(80, 88).trim().isEmpty()) {
                    location = line.substring(80, 88).trim();
                }
                ref = line.substring(89, 107).trim();
                String qtyStr = line.substring(108, 121).trim();
                String statusStr = line.substring(122, 130).trim();
                // Vérifier les conditions
                if (ref.isEmpty() || qtyStr.isEmpty() || !location.toUpperCase().startsWith("T0") || !um.equalsIgnoreCase("MT")) {
                    continue;
                }
                if (!statusStr.equalsIgnoreCase("AVAIL2")) {
                    continue;
                }
                if (!refTissus.contains(itemNumber)) {
                    continue;
                }
                double qty = 0.0;
                try {
                    qty = Double.parseDouble(qtyStr);
                } catch (Exception e) {
                    continue;
                }
                // Création de l'objet StockStatusReport
                StockStatusReport report = new StockStatusReport();
                report.setItemNumber(itemNumber);
                report.setUm(um);
                report.setAbc(abc);
                report.setSite(site);
                report.setLocation(location);
                report.setRef(ref);
                report.setQtyOnHand(qty);
                report.setStatus(statusStr);
//                report.setLastCntDate(date);
                arr.add(report);
            }
        } catch (Exception e) {
            return null;
        }
        return arr;
    }

    public List<StockStatusReport> getStockQLaize() {
        List<StockStatusReport> arr = new ArrayList<>();
//        java.nio.file.Path path = java.nio.file.Paths.get("\\\\matnr-fp01\\Groups\\CMS WEB\\cmsFolder\\reportsNew\\R100.prn");
        java.nio.file.Path path = java.nio.file.Paths.get(reportFolder + "\\R100.prn");
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy");
        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(path)) {
            String line;
            String itemNumber = "", um = "", abc = "", site = "", location = "", ref = "", status = "";
            java.time.LocalDate date = null;
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
                if (!line.substring(43, 51).trim().isEmpty() && line.substring(43, 51).trim().contains("/")) {
                    try {
                        date = java.time.LocalDate.parse(line.substring(43, 51).trim(), dateFormatter);
                    } catch (Exception e) {
                        date = null;
                    }
                }
                if (!line.substring(80, 88).trim().isEmpty()) {
                    location = line.substring(80, 88).trim();
                }
                ref = line.substring(89, 107).trim();
                String qtyStr = line.substring(108, 121).trim();
                String statusStr = line.substring(122, 130).trim();
                // Vérifier les conditions
                if (ref.isEmpty() || qtyStr.isEmpty() || !location.toUpperCase().startsWith("T0") || !um.equalsIgnoreCase("MT")) {
                    continue;
                }
                if (!statusStr.equalsIgnoreCase("AVAIL2")) {
                    continue;
                }
                double qty = 0.0;
                try {
                    qty = Double.parseDouble(qtyStr);
                } catch (Exception e) {
                    continue;
                }
                // Création de l'objet StockStatusReport
                StockStatusReport report = new StockStatusReport();
                report.setItemNumber(itemNumber);
                report.setUm(um);
                report.setAbc(abc);
                report.setSite(site);
                report.setLocation(location);
                report.setRef(ref);
                report.setQtyOnHand(qty);
                report.setStatus(statusStr);
                report.setLastCnt(date);
                // search in arrCrsr by confirmReftissu must equal itemNumber and the idRouleau must end with the ref of the report. if so setSerie in report
                if (location.toUpperCase().startsWith("T0QLAIZ")) {
                    arr.add(report);
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
            return null;
        }
        return  arr;
    }

}
