package com.lear.MGCMS.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * ArchivingService — moves aged rows out of the hot production tables into
 * same-DB {@code <table>_archive} copies, replacing the by-hand ClearCoupeHistory.sql.
 *
 * V1 scope is a hard WHITELIST of the 9 SAFE_LEAF tables (no other table keys off
 * them, so a delete can never orphan a row). The CuttingRequest* lifecycle group is
 * deliberately NOT here — it needs a COMPLETED-sequence child->parent cascade (phase 2).
 * Table + date column come ONLY from the whitelist, never from the request, so the
 * dynamic SQL has no injectable identifier; the cutoff date is always parameterised.
 */
@Service
public class ArchivingService {

    private static final Logger log = LoggerFactory.getLogger(ArchivingService.class);
    private static final int BATCH = 5000;
    private static final int MAX_BATCHES = 4000; // 20M-row guard against a runaway loop

    /** table -> date column to age on. Only these may be archived. */
    private static final Map<String, String> WHITELIST = new LinkedHashMap<>();
    static {
        WHITELIST.put("CoupeMachineHistory", "lineDate");
        WHITELIST.put("CoupeDrill", "lineDate");
        WHITELIST.put("CoupePerformance", "date");
        WHITELIST.put("overlap_saving", "created_at");
        WHITELIST.put("GammeTechniqueImprimerHistorique", "dateModification");
        WHITELIST.put("AuditQualite", "date");
        WHITELIST.put("FirstCheck", "date");
        WHITELIST.put("CuttingPlanHistory", "createdAt");
        WHITELIST.put("PartNumberMaterialConfigHistory", "createdAt");
    }

    /** Existing cold tables shown for reference only (already archives / drop candidates). */
    private static final List<String> ALREADY_ARCHIVE = List.of(
            "FirstCheckold", "Intervention_Archive", "CuttingRequestSerie2024",
            "CuttingRequestSerieRouleau2024", "CuttingRequestBox2024", "CuttingRequestPartNumber2024",
            "CuttingRequestSerieRouleauHistory2024");

    private final JdbcTemplate jdbc;

    public ArchivingService(@Qualifier("dataSource") DataSource primary) {
        this.jdbc = new JdbcTemplate(primary);
    }

    // ====================================================================
    // Inventory — what can be archived + the existing cold tables
    // ====================================================================
    public Map<String, Object> inventory() {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map.Entry<String, String> e : WHITELIST.entrySet()) {
            candidates.add(describe(e.getKey(), e.getValue(), true));
        }
        List<Map<String, Object>> cold = new ArrayList<>();
        for (String t : ALREADY_ARCHIVE) {
            if (tableExists(t)) cold.add(describe(t, null, false));
        }
        out.put("candidates", candidates);
        out.put("alreadyArchived", cold);
        return out;
    }

    private Map<String, Object> describe(String table, String dateCol, boolean archivable) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("table", table);
        m.put("dateColumn", dateCol);
        m.put("archivable", archivable);
        try {
            m.put("rows", jdbc.queryForObject(
                    "SELECT SUM(p.row_count) FROM sys.dm_db_partition_stats p WHERE p.object_id = OBJECT_ID(?) AND p.index_id IN (0,1)",
                    Long.class, table));
            m.put("sizeMb", jdbc.queryForObject(
                    "SELECT SUM(p.reserved_page_count) * 8 / 1024 FROM sys.dm_db_partition_stats p WHERE p.object_id = OBJECT_ID(?) AND p.index_id IN (0,1)",
                    Long.class, table));
            if (dateCol != null) {
                Map<String, Object> span = jdbc.queryForMap(
                        "SELECT MIN([" + dateCol + "]) AS minDate, MAX([" + dateCol + "]) AS maxDate FROM [" + table + "]");
                m.put("minDate", str(span.get("minDate")));
                m.put("maxDate", str(span.get("maxDate")));
            }
            m.put("archiveTable", table + "_archive");
            m.put("archiveExists", tableExists(table + "_archive"));
        } catch (Exception ex) {
            m.put("error", ex.getMessage());
        }
        return m;
    }

    // ====================================================================
    // Preview (dry-run) — how many rows a cutoff would archive, no changes
    // ====================================================================
    public Map<String, Object> preview(String table, String beforeDate) {
        String dateCol = require(table);
        Long count = jdbc.queryForObject(
                "SELECT COUNT_BIG(*) FROM [" + table + "] WHERE [" + dateCol + "] < ?", Long.class, beforeDate);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("table", table);
        m.put("dateColumn", dateCol);
        m.put("beforeDate", beforeDate);
        m.put("rowsToArchive", count);
        m.put("archiveTable", table + "_archive");
        return m;
    }

    // ====================================================================
    // Run — atomic per-batch copy+delete into <table>_archive
    // ====================================================================
    public Map<String, Object> run(String table, String beforeDate) {
        String dateCol = require(table);
        List<Map<String, Object>> cols = jdbc.queryForList(
                "SELECT c.name AS name, c.is_identity AS isId FROM sys.columns c "
                + "WHERE c.object_id = OBJECT_ID(?) AND c.is_computed = 0 ORDER BY c.column_id", table);
        if (cols.isEmpty()) throw new IllegalStateException("No columns found for " + table);

        StringBuilder colList = new StringBuilder();       // [a], [b]
        StringBuilder selectInto = new StringBuilder();     // [a], [idcol]+0 AS [idcol]
        StringBuilder outputList = new StringBuilder();     // DELETED.[a], DELETED.[b]
        for (Map<String, Object> c : cols) {
            String name = (String) c.get("name");
            boolean isId = ((Number) c.get("isId")).intValue() == 1;
            if (colList.length() > 0) { colList.append(", "); selectInto.append(", "); outputList.append(", "); }
            colList.append('[').append(name).append(']');
            selectInto.append(isId ? "[" + name + "]+0 AS [" + name + "]" : "[" + name + "]");
            outputList.append("DELETED.[").append(name).append(']');
        }

        String archive = table + "_archive";
        // Create the archive copy once, with identity stripped so ids insert freely.
        jdbc.execute("IF OBJECT_ID('" + archive + "') IS NULL "
                + "SELECT " + selectInto + " INTO [" + archive + "] FROM [" + table + "] WHERE 1=0");

        String del = "DELETE TOP (" + BATCH + ") src OUTPUT " + outputList
                + " INTO [" + archive + "] (" + colList + ") FROM [" + table + "] src "
                + "WHERE src.[" + dateCol + "] < ?";

        long total = 0;
        int batches = 0;
        int rows;
        do {
            rows = jdbc.update(del, beforeDate);
            total += rows;
            batches++;
        } while (rows == BATCH && batches < MAX_BATCHES);

        log.info("Archived {} rows from {} -> {} (before {}, {} batches)", total, table, archive, beforeDate, batches);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("table", table);
        m.put("archiveTable", archive);
        m.put("beforeDate", beforeDate);
        m.put("archivedRows", total);
        m.put("hitGuard", batches >= MAX_BATCHES);
        return m;
    }

    // ====================================================================
    private String require(String table) {
        String dateCol = WHITELIST.get(table);
        if (dateCol == null) {
            throw new IllegalArgumentException("Table '" + table + "' is not in the archive whitelist.");
        }
        return dateCol;
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject(
                "SELECT CASE WHEN OBJECT_ID(?) IS NULL THEN 0 ELSE 1 END", Integer.class, table);
        return n != null && n == 1;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
