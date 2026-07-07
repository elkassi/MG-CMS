package com.lear.MGCMS.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/ippm")
@PreAuthorize("hasRole('ADMIN') or hasRole('INDICATEUR') or hasRole('QUALITE') or hasRole('PROCESS')")
public class IppmController {

    private static final Logger log = LoggerFactory.getLogger(IppmController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/report")
    public ResponseEntity<?> getIppmReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String machine,
            @RequestParam(required = false) String placement) {
        try {
            // Default to last 30 days if no dates provided
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            sql.append("  s.machine, ");
            sql.append("  s.placement, ");
            sql.append("  s.tableCoupe, ");
            sql.append("  s.lieuDetection, ");
            sql.append("  cd.code AS codeDefaut, ");
            sql.append("  cd.description AS codeDefautDescription, ");
            sql.append("  cs.code AS codeScrap, ");
            sql.append("  cs.description AS codeScrapDescription, ");
            sql.append("  SUM(ISNULL(s.qteNonConforme, 0)) AS totalNonConforme, ");
            sql.append("  SUM(ISNULL(s.qteScrap, 0)) AS totalScrap, ");
            sql.append("  SUM(ISNULL(s.nbrPieceTotal, 0)) AS totalPieces, ");
            sql.append("  CASE WHEN SUM(ISNULL(s.nbrPieceTotal, 0)) > 0 ");
            sql.append("    THEN (SUM(ISNULL(s.qteNonConforme, 0)) / SUM(s.nbrPieceTotal)) * 1000000 ");
            sql.append("    ELSE 0 END AS ippmNonConforme, ");
            sql.append("  CASE WHEN SUM(ISNULL(s.nbrPieceTotal, 0)) > 0 ");
            sql.append("    THEN (SUM(ISNULL(s.qteScrap, 0)) / SUM(s.nbrPieceTotal)) * 1000000 ");
            sql.append("    ELSE 0 END AS ippmScrap ");
            sql.append("FROM dbo.CuttingRequestSerie s ");
            sql.append("LEFT JOIN dbo.CodeDefaut cd ON s.codeDefaut_code = cd.code ");
            sql.append("LEFT JOIN dbo.CodeScrap cs ON s.codeScrap_code = cs.code ");
            sql.append("WHERE statusCoupe = 'Complete' AND dateDebutCoupe BETWEEN ? AND ? ");
            
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(start));
            params.add(java.sql.Date.valueOf(end));

            if (machine != null && !machine.isEmpty()) {
                sql.append("AND s.machine LIKE ? ");
                params.add("%" + machine + "%");
            }
            if (placement != null && !placement.isEmpty()) {
                sql.append("AND s.placement LIKE ? ");
                params.add("%" + placement + "%");
            }

            sql.append("GROUP BY s.machine, s.placement, s.tableCoupe, s.lieuDetection, ");
            sql.append("cd.code, cd.description, cs.code, cs.description ");
            sql.append("HAVING SUM(ISNULL(s.qteNonConforme, 0)) > 0 OR SUM(ISNULL(s.qteScrap, 0)) > 0 ");
            sql.append("ORDER BY SUM(ISNULL(s.qteNonConforme, 0)) + SUM(ISNULL(s.qteScrap, 0)) DESC");

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("IppmController operation failed", e);
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getIppmSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            // Default to last 30 days if no dates provided
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            String sql = "SELECT " +
                    "  SUM(ISNULL(qteNonConforme, 0)) AS totalNonConforme, " +
                    "  SUM(ISNULL(qteScrap, 0)) AS totalScrap, " +
                    "  SUM(ISNULL(nbrPieceTotal, 0)) AS totalPieces, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteNonConforme, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmNonConforme, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteScrap, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmScrap " +
                    "FROM dbo.CuttingRequestSerie " +
                    "WHERE statusCoupe = 'Complete' AND dateDebutCoupe BETWEEN ? AND ?";

            Map<String, Object> summary = jdbcTemplate.queryForMap(sql, 
                    java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));

            summary.put("startDate", start.format(DateTimeFormatter.ISO_DATE));
            summary.put("endDate", end.format(DateTimeFormatter.ISO_DATE));

            return new ResponseEntity<>(summary, HttpStatus.OK);
        } catch (Exception e) {
            log.error("IppmController operation failed", e);
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/byMachine")
    public ResponseEntity<?> getIppmByMachine(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            String sql = "SELECT " +
                    "  machine, " +
                    "  SUM(ISNULL(qteNonConforme, 0)) AS totalNonConforme, " +
                    "  SUM(ISNULL(qteScrap, 0)) AS totalScrap, " +
                    "  SUM(ISNULL(nbrPieceTotal, 0)) AS totalPieces, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteNonConforme, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmNonConforme, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteScrap, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmScrap " +
                    "FROM dbo.CuttingRequestSerie " +
                    "WHERE statusCoupe = 'Complete' AND dateDebutCoupe BETWEEN ? AND ? AND machine IS NOT NULL " +
                    "GROUP BY machine " +
                    "ORDER BY machine";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, 
                    java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));

            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("IppmController operation failed", e);
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/byCodeDefaut")
    public ResponseEntity<?> getIppmByCodeDefaut(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            String sql = "SELECT " +
                    "  cd.code AS codeDefaut, " +
                    "  cd.description AS description, " +
                    "  COUNT(*) AS occurrences, " +
                    "  SUM(ISNULL(s.qteNonConforme, 0)) AS totalNonConforme " +
                    "FROM dbo.CuttingRequestSerie s " +
                    "JOIN dbo.CodeDefaut cd ON s.codeDefaut_code = cd.code " +
                    "WHERE s.statusCoupe = 'Complete' AND s.dateDebutCoupe BETWEEN ? AND ? " +
                    "GROUP BY cd.code, cd.description " +
                    "ORDER BY SUM(ISNULL(s.qteNonConforme, 0)) DESC";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, 
                    java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));

            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("IppmController operation failed", e);
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/byLieuDetection")
    public ResponseEntity<?> getIppmByLieuDetection(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            String sql = "SELECT " +
                    "  ISNULL(lieuDetection, 'Non spécifié') AS lieuDetection, " +
                    "  SUM(ISNULL(qteNonConforme, 0)) AS totalNonConforme, " +
                    "  SUM(ISNULL(qteScrap, 0)) AS totalScrap, " +
                    "  COUNT(*) AS occurrences " +
                    "FROM dbo.CuttingRequestSerie " +
                    "WHERE statusCoupe = 'Complete' AND dateDebutCoupe BETWEEN ? AND ? " +
                    "AND (qteNonConforme > 0 OR qteScrap > 0) " +
                    "GROUP BY lieuDetection " +
                    "ORDER BY SUM(ISNULL(qteNonConforme, 0)) + SUM(ISNULL(qteScrap, 0)) DESC";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, 
                    java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));

            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("IppmController operation failed", e);
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/byTableCoupe")
    public ResponseEntity<?> getIppmByTableCoupe(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            String sql = "SELECT " +
                    "  ISNULL(tableCoupe, 'Non spécifié') AS tableCoupe, " +
                    "  SUM(ISNULL(qteNonConforme, 0)) AS totalNonConforme, " +
                    "  SUM(ISNULL(qteScrap, 0)) AS totalScrap, " +
                    "  SUM(ISNULL(nbrPieceTotal, 0)) AS totalPieces, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteNonConforme, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmNonConforme, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteScrap, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmScrap " +
                    "FROM dbo.CuttingRequestSerie " +
                    "WHERE statusCoupe = 'Complete' AND dateDebutCoupe BETWEEN ? AND ? " +
                    "GROUP BY tableCoupe " +
                    "ORDER BY SUM(ISNULL(qteNonConforme, 0)) + SUM(ISNULL(qteScrap, 0)) DESC";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, 
                    java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));

            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("IppmController operation failed", e);
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/byPartNumberMaterial")
    public ResponseEntity<?> getIppmByPartNumberMaterial(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            String sql = "SELECT " +
                    "  ISNULL(partNumberMaterial, 'Non spécifié') AS partNumberMaterial, " +
                    "  SUM(ISNULL(qteNonConforme, 0)) AS totalNonConforme, " +
                    "  SUM(ISNULL(qteScrap, 0)) AS totalScrap, " +
                    "  SUM(ISNULL(nbrPieceTotal, 0)) AS totalPieces, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteNonConforme, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmNonConforme, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteScrap, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmScrap " +
                    "FROM dbo.CuttingRequestSerie " +
                    "WHERE statusCoupe = 'Complete' AND dateDebutCoupe BETWEEN ? AND ? " +
                    "GROUP BY partNumberMaterial " +
                    "HAVING SUM(ISNULL(qteNonConforme, 0)) > 0 OR SUM(ISNULL(qteScrap, 0)) > 0 " +
                    "ORDER BY SUM(ISNULL(qteNonConforme, 0)) + SUM(ISNULL(qteScrap, 0)) DESC";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, 
                    java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));

            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("IppmController operation failed", e);
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/byCoupeur1")
    public ResponseEntity<?> getIppmByCoupeur(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            String sql = "SELECT " +
                    "  ISNULL(coupeur1, 'Non spécifié') AS coupeur, " +
                    "  SUM(ISNULL(qteNonConforme, 0)) AS totalNonConforme, " +
                    "  SUM(ISNULL(qteScrap, 0)) AS totalScrap, " +
                    "  SUM(ISNULL(nbrPieceTotal, 0)) AS totalPieces, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteNonConforme, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmNonConforme, " +
                    "  CASE WHEN SUM(ISNULL(nbrPieceTotal, 0)) > 0 " +
                    "    THEN (SUM(ISNULL(qteScrap, 0)) / SUM(nbrPieceTotal)) * 1000000 " +
                    "    ELSE 0 END AS ippmScrap " +
                    "FROM dbo.CuttingRequestSerie " +
                    "WHERE statusCoupe = 'Complete' AND dateDebutCoupe BETWEEN ? AND ? " +
                    "GROUP BY coupeur1 " +
                    "HAVING SUM(ISNULL(qteNonConforme, 0)) > 0 OR SUM(ISNULL(qteScrap, 0)) > 0 " +
                    "ORDER BY SUM(ISNULL(qteNonConforme, 0)) + SUM(ISNULL(qteScrap, 0)) DESC";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, 
                    java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));

            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("IppmController operation failed", e);
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
