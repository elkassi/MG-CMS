package com.lear.MGCMS.services;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * SystemHealthService — read-only diagnostics for the ROLE_ADMIN System Health page.
 *
 * Three things the floor IT actually needs when "the app is slow":
 *   1. Are the 7 databases reachable + how big are they (which table is bloating)?
 *   2. What is the SQL Server doing — the expensive queries and WHO is running them.
 *   3. Is the MG-CMS host itself the bottleneck — CPU / RAM / disk (C:, F:) / network.
 *
 * The last-hour history is an in-memory ring buffer captured every 5 min.
 * ponytail: in-memory, lost on restart — enough for "what happened the last hour".
 * Upgrade path: persist snapshots to a table if cross-restart history is ever needed.
 */
@Service
public class SystemHealthService {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** name -> JdbcTemplate, in display order. The first (primary) also serves the instance-wide DMVs. */
    private final Map<String, JdbcTemplate> dbs = new LinkedHashMap<>();
    private final SystemInfo systemInfo = new SystemInfo();

    /** Ring buffer of the last ~2h of light snapshots (5-min cadence => 24 entries). */
    private final Deque<Map<String, Object>> history = new ArrayDeque<>();
    private static final int HISTORY_MAX = 24;

    public SystemHealthService(
            @Qualifier("dataSource") DataSource primary,
            @Qualifier("ctcDataSource") DataSource ctc,
            @Qualifier("cmsDataSource") DataSource cms,
            @Qualifier("plsDataSource") DataSource pls,
            @Qualifier("spliceDataSource") DataSource splice,
            @Qualifier("learpokayokeDataSource") DataSource learpokayoke,
            @Qualifier("imsDataSource") DataSource ims) {
        dbs.put("primary (LEAR_CMS)", new JdbcTemplate(primary));
        dbs.put("ctc (plt_viewer)", new JdbcTemplate(ctc));
        dbs.put("cms (qualite)", new JdbcTemplate(cms));
        dbs.put("pls (MG_PLS)", new JdbcTemplate(pls));
        dbs.put("splice", new JdbcTemplate(splice));
        dbs.put("learpokayoke", new JdbcTemplate(learpokayoke));
        dbs.put("ims (IMS_NEWAPP)", new JdbcTemplate(ims));
    }

    private JdbcTemplate primary() {
        return dbs.values().iterator().next();
    }

    // ====================================================================
    // Public: full live status (what the /status endpoint returns)
    // ====================================================================
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("timestamp", LocalDateTime.now().format(TS));
        out.put("server", serverMetrics());
        out.put("databases", databases());
        out.put("expensiveQueries", expensiveQueries());
        out.put("activeQueries", activeQueries());
        out.put("historyPoints", history.size());
        return out;
    }

    // ====================================================================
    // 1. Server host metrics (OSHI)
    // ====================================================================
    public Map<String, Object> serverMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        OperatingSystem os = systemInfo.getOperatingSystem();

        // --- CPU + network rate over one 500ms window ---
        try {
            CentralProcessor cpu = hal.getProcessor();
            long[] net0 = netTotals(hal);
            double load = cpu.getSystemCpuLoad(500); // blocks 500ms, returns 0..1
            long[] net1 = netTotals(hal);
            Map<String, Object> cpuMap = new LinkedHashMap<>();
            cpuMap.put("usedPct", round(load * 100, 1));
            cpuMap.put("physicalCores", cpu.getPhysicalProcessorCount());
            cpuMap.put("logicalCores", cpu.getLogicalProcessorCount());
            cpuMap.put("name", cpu.getProcessorIdentifier().getName().trim());
            m.put("cpu", cpuMap);

            Map<String, Object> netMap = new LinkedHashMap<>();
            netMap.put("rxKbps", round((net1[0] - net0[0]) * 8.0 / 1000.0 / 0.5, 1));
            netMap.put("txKbps", round((net1[1] - net0[1]) * 8.0 / 1000.0 / 0.5, 1));
            m.put("network", netMap);
        } catch (Exception e) {
            m.put("cpu", err(e));
        }

        // --- Memory ---
        try {
            GlobalMemory mem = hal.getMemory();
            long total = mem.getTotal();
            long avail = mem.getAvailable();
            long used = total - avail;
            Map<String, Object> memMap = new LinkedHashMap<>();
            memMap.put("usedMb", bytesToMb(used));
            memMap.put("totalMb", bytesToMb(total));
            memMap.put("usedPct", round(100.0 * used / total, 1));
            m.put("memory", memMap);
        } catch (Exception e) {
            m.put("memory", err(e));
        }

        // --- JVM heap (the MG-CMS process itself) ---
        try {
            Runtime rt = Runtime.getRuntime();
            Map<String, Object> jvm = new LinkedHashMap<>();
            jvm.put("heapUsedMb", bytesToMb(rt.totalMemory() - rt.freeMemory()));
            jvm.put("heapMaxMb", bytesToMb(rt.maxMemory()));
            m.put("jvm", jvm);
        } catch (Exception e) {
            m.put("jvm", err(e));
        }

        // --- Disks / volumes (C:, F:, ...) — used over total, what's remaining ---
        try {
            List<Map<String, Object>> disks = new ArrayList<>();
            for (OSFileStore fs : os.getFileSystem().getFileStores()) {
                long total = fs.getTotalSpace();
                if (total <= 0) continue;
                long usable = fs.getUsableSpace();
                long used = total - usable;
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("mount", fs.getMount());
                d.put("name", fs.getName());
                d.put("totalGb", bytesToGb(total));
                d.put("usedGb", bytesToGb(used));
                d.put("freeGb", bytesToGb(usable));
                d.put("usedPct", round(100.0 * used / total, 1));
                disks.add(d);
            }
            m.put("disks", disks);
        } catch (Exception e) {
            m.put("disks", err(e));
        }

        // --- Top processes by memory (what's eating the host) ---
        try {
            List<Map<String, Object>> procs = new ArrayList<>();
            int self = os.getProcessId();
            for (OSProcess p : os.getProcesses(OperatingSystem.ProcessFiltering.ALL_PROCESSES,
                    OperatingSystem.ProcessSorting.RSS_DESC, 8)) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("pid", p.getProcessID());
                pm.put("name", p.getName());
                pm.put("memMb", bytesToMb(p.getResidentSetSize()));
                pm.put("isMgcms", p.getProcessID() == self);
                procs.add(pm);
            }
            m.put("topProcesses", procs);
        } catch (Exception e) {
            m.put("topProcesses", err(e));
        }

        return m;
    }

    private long[] netTotals(HardwareAbstractionLayer hal) {
        long rx = 0, tx = 0;
        for (NetworkIF nif : hal.getNetworkIFs()) {
            nif.updateAttributes();
            rx += nif.getBytesRecv();
            tx += nif.getBytesSent();
        }
        return new long[] { rx, tx };
    }

    // ====================================================================
    // 2. Per-database probes (connectivity, size, biggest tables)
    // ====================================================================
    public List<Map<String, Object>> databases() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, JdbcTemplate> e : dbs.entrySet()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("name", e.getKey());
            JdbcTemplate jt = e.getValue();
            try {
                jt.queryForObject("SELECT 1", Integer.class);
                d.put("online", true);
                d.put("sizeMb", jt.queryForObject(
                        "SELECT CAST(SUM(size) * 8.0 / 1024 AS DECIMAL(12,1)) FROM sys.database_files", Double.class));
                d.put("biggestTables", jt.queryForList(
                        "SELECT TOP 5 t.name AS tbl, SUM(p.row_count) AS [rows], "
                        + "SUM(p.reserved_page_count) * 8 / 1024 AS mb "
                        + "FROM sys.dm_db_partition_stats p JOIN sys.tables t ON t.object_id = p.object_id "
                        + "WHERE p.index_id IN (0,1) GROUP BY t.name ORDER BY mb DESC"));
            } catch (Exception ex) {
                d.put("online", false);
                d.put("error", rootMsg(ex));
            }
            out.add(d);
        }
        return out;
    }

    // ====================================================================
    // 3. Expensive queries (instance-wide, since last restart) + who runs them
    // ====================================================================
    public List<Map<String, Object>> expensiveQueries() {
        try {
            return primary().queryForList(
                "SELECT TOP 15 DB_NAME(qt.dbid) AS db, qs.execution_count AS execCount, "
                + "qs.total_worker_time/1000 AS totalCpuMs, "
                + "(qs.total_worker_time/qs.execution_count)/1000 AS avgCpuMs, "
                + "qs.total_logical_reads AS totalReads, "
                + "qs.total_logical_reads/qs.execution_count AS avgReads, "
                + "(qs.total_elapsed_time/qs.execution_count)/1000 AS avgElapsedMs, "
                + "LTRIM(SUBSTRING(qt.text, (qs.statement_start_offset/2)+1, "
                + "((CASE qs.statement_end_offset WHEN -1 THEN DATALENGTH(qt.text) "
                + "ELSE qs.statement_end_offset END - qs.statement_start_offset)/2)+1)) AS queryText "
                + "FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) qt "
                + "ORDER BY qs.total_worker_time DESC");
        } catch (Exception ex) {
            return List.of(err(ex));
        }
    }

    /** Currently-running user queries WITH the login that owns each — what you can act on now. */
    public List<Map<String, Object>> activeQueries() {
        try {
            return primary().queryForList(
                "SELECT TOP 20 r.session_id AS sessionId, s.login_name AS login, s.host_name AS host, "
                + "DB_NAME(r.database_id) AS db, r.status, r.cpu_time AS cpuMs, "
                + "r.total_elapsed_time AS elapsedMs, r.reads, r.wait_type AS waitType, "
                + "LTRIM(SUBSTRING(t.text, (r.statement_start_offset/2)+1, "
                + "((CASE r.statement_end_offset WHEN -1 THEN DATALENGTH(t.text) "
                + "ELSE r.statement_end_offset END - r.statement_start_offset)/2)+1)) AS queryText "
                + "FROM sys.dm_exec_requests r JOIN sys.dm_exec_sessions s ON r.session_id = s.session_id "
                + "CROSS APPLY sys.dm_exec_sql_text(r.sql_handle) t "
                + "WHERE r.session_id <> @@SPID AND s.is_user_process = 1 "
                + "ORDER BY r.cpu_time DESC");
        } catch (Exception ex) {
            return List.of(err(ex));
        }
    }

    // ====================================================================
    // History capture (every 5 min) + downloadable last-hour report
    // ====================================================================
    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void captureSnapshot() {
        try {
            Map<String, Object> server = serverMetrics();
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("at", LocalDateTime.now().format(TS));
            snap.put("cpuPct", deep(server, "cpu", "usedPct"));
            snap.put("memUsedPct", deep(server, "memory", "usedPct"));
            snap.put("memUsedMb", deep(server, "memory", "usedMb"));
            snap.put("netRxKbps", deep(server, "network", "rxKbps"));
            snap.put("netTxKbps", deep(server, "network", "txKbps"));
            snap.put("disks", server.get("disks"));
            snap.put("activeQueryCount", activeQueries().size());
            synchronized (history) {
                history.addLast(snap);
                while (history.size() > HISTORY_MAX) history.removeFirst();
            }
        } catch (Exception e) {
            log.warn("System health snapshot failed: {}", e.getMessage());
        }
    }

    public String reportFileName() {
        return "mgcms-health-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".md";
    }

    /** Markdown report = last {minutes} of history + a fresh live snapshot. Hand this back to support. */
    public byte[] report(int minutes) {
        int points = Math.max(1, Math.min(HISTORY_MAX, minutes / 5));
        StringBuilder sb = new StringBuilder();
        sb.append("# MG-CMS System Health Report\n\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(TS)).append("  \n");
        sb.append("Window: last ").append(minutes).append(" min (").append(points).append(" snapshots)\n\n");

        Map<String, Object> live = status();

        sb.append("## Server (live)\n\n");
        Map<String, Object> server = cast(live.get("server"));
        sb.append("```json\n").append(json(server)).append("\n```\n\n");

        sb.append("## Databases\n\n| DB | Online | Size MB | Biggest table |\n|---|---|---|---|\n");
        for (Object o : (List<?>) live.get("databases")) {
            Map<String, Object> d = cast(o);
            Object big = "";
            Object bt = d.get("biggestTables");
            if (bt instanceof List && !((List<?>) bt).isEmpty()) {
                Map<String, Object> t = cast(((List<?>) bt).get(0));
                big = t.get("tbl") + " (" + t.get("mb") + " MB)";
            }
            sb.append("| ").append(d.get("name")).append(" | ").append(d.get("online"))
              .append(" | ").append(d.getOrDefault("sizeMb", d.getOrDefault("error", "")))
              .append(" | ").append(big).append(" |\n");
        }

        sb.append("\n## Expensive queries (instance, since restart)\n\n");
        sb.append("```json\n").append(json(live.get("expensiveQueries"))).append("\n```\n\n");

        sb.append("## Active queries now (with login)\n\n");
        sb.append("```json\n").append(json(live.get("activeQueries"))).append("\n```\n\n");

        sb.append("## History (last ").append(minutes).append(" min)\n\n");
        sb.append("| Time | CPU % | Mem % | Net Rx Kbps | Net Tx Kbps | Active Q |\n|---|---|---|---|---|---|\n");
        List<Map<String, Object>> recent;
        synchronized (history) {
            recent = new ArrayList<>(history);
        }
        int from = Math.max(0, recent.size() - points);
        for (int i = from; i < recent.size(); i++) {
            Map<String, Object> s = recent.get(i);
            sb.append("| ").append(s.get("at")).append(" | ").append(s.get("cpuPct"))
              .append(" | ").append(s.get("memUsedPct")).append(" | ").append(s.get("netRxKbps"))
              .append(" | ").append(s.get("netTxKbps")).append(" | ").append(s.get("activeQueryCount")).append(" |\n");
        }
        if (recent.isEmpty()) sb.append("| (no snapshots captured yet — app started < 5 min ago) |\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ====================================================================
    // helpers
    // ====================================================================
    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    private static Object deep(Map<String, Object> m, String k1, String k2) {
        Object inner = m.get(k1);
        return inner instanceof Map ? ((Map<?, ?>) inner).get(k2) : null;
    }

    private static Map<String, Object> err(Exception e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", rootMsg(e));
        return m;
    }

    private static String rootMsg(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null) r = r.getCause();
        return r.getClass().getSimpleName() + ": " + r.getMessage();
    }

    private static double round(double v, int d) {
        double f = Math.pow(10, d);
        return Math.round(v * f) / f;
    }

    private static double bytesToMb(long b) {
        return round(b / 1024.0 / 1024.0, 1);
    }

    private static double bytesToGb(long b) {
        return round(b / 1024.0 / 1024.0 / 1024.0, 1);
    }

    /** Minimal JSON for the report — avoids a hard Jackson dependency in this layer. */
    private static String json(Object o) {
        StringBuilder sb = new StringBuilder();
        writeJson(o, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeJson(Object o, StringBuilder sb) {
        if (o == null) { sb.append("null"); return; }
        if (o instanceof Map) {
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append('"').append(e.getKey()).append("\": ");
                writeJson(e.getValue(), sb);
            }
            sb.append("}");
        } else if (o instanceof List) {
            sb.append("[");
            boolean first = true;
            for (Object e : (List<Object>) o) {
                if (!first) sb.append(", ");
                first = false;
                writeJson(e, sb);
            }
            sb.append("]");
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
        } else {
            sb.append('"').append(String.valueOf(o).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")).append('"');
        }
    }
}
