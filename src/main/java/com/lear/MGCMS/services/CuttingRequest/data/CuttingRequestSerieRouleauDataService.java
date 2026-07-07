package com.lear.MGCMS.services.CuttingRequest.data;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import com.lear.MGCMS.domain.StockStatusReport;
import com.lear.MGCMS.payload.RapportShortageUrgent;
import com.lear.MGCMS.payload.RouleauRapport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieRouleauDataRepository;

@Service
public class CuttingRequestSerieRouleauDataService {

    @Autowired
    private CuttingRequestSerieRouleauDataRepository repo;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CuttingRequestSerieRouleauDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public CuttingRequestSerieRouleauData findById(Long id) {
        Optional<CuttingRequestSerieRouleauData> obj = repo.findById(id);
        if (!obj.isPresent()) {
            return null;
        }
        return obj.get();
    }

    public CuttingRequestSerieRouleauData save(CuttingRequestSerieRouleauData obj) {
        return repo.save(obj);
    }

    public void delete(CuttingRequestSerieRouleauData obj) {
        repo.delete(obj);
    }

    public List<RouleauRapport> findRest(String reftissu) {
        String sql = "WITH RankedRows AS ( " +
                "SELECT  " +
                "crsr.[confirmReftissu]," +
                "        crsr.[idRouleau], " +
                "        crsr.[laize], " +
                "        crsr.[lotFrs], " +
                "        crsr.[metrage], " +
                "        crsr.[nbrCouche], " +
                "        crsr.[retour], " +
                "        crsr.[cuttingRequestSerie_serie], " +
                "        crsr.[createdAt], " +
                "        crs.tableMatelassage, " +
                "            crsr.[confirmRetour], " +
                "        ROW_NUMBER() OVER (PARTITION BY crsr.[lotFrs], crsr.[idRouleau] ORDER BY crsr.[metrage] ASC) AS RowNum " +
                "    FROM  " +
                "        [dbo].[CuttingRequestSerieRouleau] AS crsr " +
                "        JOIN [dbo].[CuttingRequestSerie] AS crs ON crs.serie = crsr.cuttingRequestSerie_serie " +
                "    WHERE  " +
                "            crsr.[createdAt] >= DATEADD(HOUR, -48, GETDATE()) " +
                ") " +
                "SELECT  " +
                "[confirmReftissu]," +
                "    [lotFrs], " +
                "    [idRouleau], " +
                "    [laize], " +
                "    [retour], " +
                "    [cuttingRequestSerie_serie], " +
                "    [createdAt], " +
                "    tableMatelassage " +
                "FROM  RankedRows " +
                "WHERE RowNum = 1 and confirmRetour = 1 " +
                "ORDER BY [confirmReftissu], [createdAt] DESC;";
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    RouleauRapport result = new RouleauRapport();
                    result.setReftissu(rs.getString(1));
                    result.setLotFrs(rs.getString(2));
                    result.setIdRouleau(rs.getString(3));
                    result.setLaize(rs.getDouble(4));
                    result.setRetour(rs.getDouble(5));
                    result.setSerie(rs.getString(6));
                    result.setCreatedAt(convertTimestampToLocalDateTime(rs.getTimestamp(7)));
                    result.setTableMatelassage(rs.getString(8));
                    return result;
                }
        );
    }

    public static LocalDateTime convertTimestampToLocalDateTime(Timestamp timestampToConvert) {
        if (timestampToConvert != null) {
            return timestampToConvert.toLocalDateTime();
        } else {
            return null;
        }
    }


    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }

    public Page<CuttingRequestSerieRouleauData> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

        Specification<CuttingRequestSerieRouleauData> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add filters based on the key-value pairs in the 'filters' map
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
                String[] strArr = entry.getKey().split("\\.");

                if (strArr.length >= 2) {
                    Path<String> path = root.get(strArr[1]);
                    for (int i = 2; i < strArr.length; i++) {
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
                        String valueEntry = entry.getValue();
                        if (valueEntry.equalsIgnoreCase("1")) {
                            valueEntry = "TRUE";
                        }
                        if (valueEntry.equalsIgnoreCase("0")) {
                            valueEntry = "FALSE";
                        }
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Boolean.class), Boolean.parseBoolean(valueEntry)));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(Boolean.class), Boolean.parseBoolean(valueEntry)));
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

                    if (entry.getKey().startsWith("isNull.")) {
                        predicates.add(builder.isNull(path));
                    } else if (entry.getKey().startsWith("isNotNull.")) {
                        predicates.add(builder.isNotNull(path));
                    }

                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
    }

    public List<CuttingRequestSerieRouleauData> findEmptySerie() {
        return repo.findEmptySerie();
    }

    public List<CuttingRequestSerieRouleauData> findDefaut() {
        return repo.findDefaut();
    }

    public List<CuttingRequestSerieRouleauData> findExcess(Double max, LocalDateTime date1, LocalDateTime date2) {
        return repo.findExcess(max, date1, date2);
    }

    public List<CuttingRequestSerieRouleauData> findBySeries(List<String> listSeries) {
        return repo.findBySeries(listSeries);
    }

    public List<RapportShortageUrgent> findShrotageUrgent(Double max, LocalDateTime date1, LocalDateTime date2) {
        /*
        select top 2000 idRouleau,
  sum(totalUsage) as 'Total Usage',
  MAX(metrage) as 'Metrage Initial',
  min(excess) as 'Shortage',
  MAX(createdAt) as 'Date',
    MAX(confirmReftissu) as 'Référence tissu'
    FROM [dbo].[CuttingRequestSerieRouleau]
	group by idRouleau
	having COUNT(*) = 1 and min(excess) < -1
	order by MAX(createdAt) desc
         */
        String sql = """
                    SELECT TOP 2000 idRouleau,
                           SUM(totalUsage) AS TotalUsage,
                           MAX(metrage) AS MetrageInitial,
                           MIN(excess) AS Shortage,
                           MAX(createdAt) AS Date,
                           MAX(confirmReftissu) AS Reftissu
                    FROM dbo.CuttingRequestSerieRouleau
                    GROUP BY idRouleau
                    HAVING COUNT(*) = 1 AND MAX(createdAt) BETWEEN ? AND ? AND MIN(excess) < ?
                    ORDER BY MAX(createdAt) DESC
                """;

        return jdbcTemplate.query(sql, new Object[]{date1, date2, max},
                (rs, rowNum) -> {
                    RapportShortageUrgent rapport = new RapportShortageUrgent();
                    rapport.setIdRouleau(rs.getString("idRouleau"));
                    rapport.setTotalUsage(rs.getDouble("TotalUsage"));
                    rapport.setMetrageInitial(rs.getDouble("MetrageInitial"));
                    rapport.setShortage(rs.getDouble("Shortage"));
                    rapport.setDate(rs.getTimestamp("Date").toLocalDateTime());
                    rapport.setReftissu(rs.getString("Reftissu"));
                    return rapport;
                });

    }

    public List<StockStatusReport> findBadLinesInReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<CuttingRequestSerieRouleauData> arrCrsr = new ArrayList<>();
        String sql = "SELECT cuttingRequestSerie_serie, idRouleau, confirmReftissu from CuttingRequestSerieRouleau where createdAt >= ? and createdAt <= ? and confirmRetour = 0 order by confirmReftissu, idRouleau";
        List<String> refTissus = new ArrayList<>();
        jdbcTemplate.query(sql, new Object[]{startDate, endDate},
                (rs) -> {
                    CuttingRequestSerieRouleauData obj = new CuttingRequestSerieRouleauData();
                    obj.setSerie(rs.getString("cuttingRequestSerie_serie"));
                    obj.setIdRouleau(rs.getString("idRouleau"));
                    obj.setConfirmReftissu(rs.getString("confirmReftissu"));
                    arrCrsr.add(obj);
                    if (!refTissus.contains(obj.getConfirmReftissu())) {
                        refTissus.add(obj.getConfirmReftissu());
                    }
                });
        // refTissus has to have
        List<StockStatusReport> arr = new ArrayList<>();
        java.nio.file.Path path = java.nio.file.Paths.get("\\\\matnr-fp01\\Groups\\CMS WEB\\cmsFolder\\reportsNew\\R100.prn");
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
                // search in arrCrsr by confirmReftissu must equal itemNumber and the idRouleau must end with the ref of the report. if so setSerie in report
                for (CuttingRequestSerieRouleauData crsr : arrCrsr) {
                    if (crsr.getConfirmReftissu().equalsIgnoreCase(itemNumber) && crsr.getIdRouleau().endsWith(ref)) {
                        report.setSerie(crsr.getSerie());
                        arr.add(report);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return arr;
    }

    public List<String> getPartNumberMateriallToBeUsed(List<String> sequenceStartsWith) {
        /*
        select partNumberMaterial , COUNT(*), sum(nbrCouche*longueur)
     FROM [dbo].[CuttingRequestSerie]
   where statusMatelassage = 'waiting' and (cuttingRequest_sequence like '120725%' or cuttingRequest_sequence like '130725%' or cuttingRequest_sequence like '140725%')
   group by partNumberMaterial
         */

        String sql = "SELECT partNumberMaterial FROM CuttingRequestSerie WHERE statusMatelassage = 'waiting' AND (";

        for (int i = 0; i < sequenceStartsWith.size(); i++) {
            sql += "cuttingRequest_sequence LIKE '" + sequenceStartsWith.get(i) + "%'";
            if (i < sequenceStartsWith.size() - 1) {
                sql += " OR ";
            }
        }
        sql += ") GROUP BY partNumberMaterial";
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
