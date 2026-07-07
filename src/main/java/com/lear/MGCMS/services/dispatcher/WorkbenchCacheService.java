package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import com.lear.MGCMS.domain.ScanRouleau;

import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.services.NonImportedChargeService;
import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.scheduling.ShiftClock;

/**
 * In-memory cache for Process Workbench data with incremental refresh support.
 *
 * <p>Eliminates repeated SQL queries when multiple users poll
 * {@code /api/workbench/data}. The cache is refreshed by a scheduled
 * background thread; HTTP requests serve the cached copy instantly.</p>
 *
 * <p>Refresh frequency adapts to engine state:</p>
 * <ul>
 *   <li>Engine running (WARMING/IMPROVING): every 5 s</li>
 *   <li>Engine idle/stopped: every 15 s</li>
 * </ul>
 *
 * <p>Incremental refresh only reloads series for new, removed, or changed
 * sequences. Stock racks and box status are refreshed with independent TTLs.
 * A full rebuild is forced every 10 incremental refreshes.</p>
 */
@Service
public class WorkbenchCacheService {

    private static final Logger log = LoggerFactory.getLogger(WorkbenchCacheService.class);
    private static final int SQL_BATCH_SIZE = 1000;
    private static final int STOCK_RACKS_TTL_SECONDS = 300;
    private static final int FORCE_FULL_REFRESH_EVERY = 10;

    @Autowired private DispatcherProperties dispatcherProperties;
    @Autowired private LiveChargeService liveChargeService;
    @Autowired private ZoneLoadService zoneLoadService;
    @Autowired private OrdonnancementService ordonnancementService;
    @Autowired private ContinuousDispatchOptimizerService optimizerService;
    @Autowired private ShiftClock shiftClock;
    @Autowired private CuttingRequestRepository cuttingRequestRepository;
    @Autowired private CuttingRequestSerieDataRepository serieDataRepository;
    @Autowired private ScanRouleauRepository scanRouleauRepository;
    @Autowired private NonImportedChargeService nonImportedChargeService;
    @Autowired private SerieStatusDateValidator serieStatusDateValidator;
    @Autowired private TableFeedRankingService tableFeedRankingService;
    @Autowired(required = false) private WorkbenchSequenceFocusService sequenceFocusService;

    /** Cache key = "date|shift" → cached response map. */
    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
    /** Last refresh timestamp per key. */
    private final Map<String, LocalDateTime> lastRefresh = new ConcurrentHashMap<>();
    /** Per-key metadata for incremental caching. */
    private final Map<String, CacheMetadata> metadata = new ConcurrentHashMap<>();
    /** Tracks the engine state at the last cache build for transition detection. */
    private volatile EngineState lastCachedEngineState = null;
    /** Throttle sequenceStatus auto-correction to once per 5 minutes. */
    private volatile long lastSequenceStatusCorrectionMs = 0;
    private static final long SEQUENCE_STATUS_CORRECTION_INTERVAL_MS = 5 * 60 * 1000;
    private volatile long lastProductionNormalizationMs = 0;
    private static final long PRODUCTION_NORMALIZATION_INTERVAL_MS = 60 * 1000;
    private volatile long lastGroundUpEngineReloadMs = 0;
    private static final long GROUND_UP_ENGINE_RELOAD_INTERVAL_MS = 10 * 60 * 1000;
    /** Keys currently being rebuilt — prevents multiple threads from rebuilding the same key simultaneously. */
    private final Set<String> rebuildInProgress = ConcurrentHashMap.newKeySet();
    /** Prevents idle workbench polling from repeatedly rebuilding the engine snapshot. */
    private final Map<String, Long> lastEnginePreloadMsByKey = new ConcurrentHashMap<>();
    private final Set<String> enginePreloadInProgress = ConcurrentHashMap.newKeySet();
    private static final long ENGINE_PRELOAD_INTERVAL_MS = 60 * 1000;

    // ------------------------------------------------------------------ metadata holder

    /**
     * Holds incremental cache state for a single (date, shift) key.
     */
    private static class CacheMetadata {
        LocalDateTime lastFullRefresh;
        LocalDateTime lastIncrementalRefresh;
        final Set<String> cachedActiveSequences = ConcurrentHashMap.newKeySet();
        final Map<String, List<Map<String, Object>>> cachedSeriesMap = new ConcurrentHashMap<>();
        List<Map<String, Object>> cachedStockRacks;
        LocalDateTime stockRacksLoadedAt;
        int fullRefreshCount = 0;
    }

    // ------------------------------------------------------------------ public API

    /**
     * Returns cached workbench data for the given (date, shift).
     * If the cache is empty or stale, falls back to a synchronous refresh.
     * Always injects a fresh engine state.
     */
    public Map<String, Object> getData(LocalDate date, int shift) {
        String key = key(date, shift);
        Map<String, Object> data = cache.get(key);

        if (data == null) {
            log.info("Cache miss for {} — performing full refresh", key);
            data = refresh(date, shift);
        } else if (isStale(key) || engineStateChanged()) {
            log.debug("Cache stale for {} — trying incremental refresh", key);
            try {
                data = refreshIncremental(date, shift);
            } catch (Exception e) {
                log.warn("Incremental refresh failed for {}, falling back to cached data", key, e);
                lastRefresh.put(key, LocalDateTime.now());
            }
        } else {
            log.debug("Cache hit for {}", key);
        }

        // Always inject fresh engine state — never cached.
        Map<String, Object> engineState = buildEngineState();
        data.put("engineState", engineState);
        if (sequenceFocusService != null) {
            sequenceFocusService.updateEngineState(data.get("sequenceFocus"), engineState);
        }

        return data;
    }

    /**
     * Force-refresh the cache for a specific (date, shift) — full rebuild.
     */
    public Map<String, Object> refresh(LocalDate date, int shift) {
        String key = key(date, shift);
        if (!rebuildInProgress.add(key)) {
            Map<String, Object> stale = cache.get(key);
            if (stale != null) return stale;
            // Another thread is rebuilding and cache is empty — wait up to 30s
            int waited = 0;
            while (rebuildInProgress.contains(key) && waited < 300) {
                try {
                    Thread.sleep(100);
                    waited++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                stale = cache.get(key);
                if (stale != null) return stale;
            }
            stale = cache.get(key);
            if (stale != null) return stale;
        }
        try {
            Map<String, Object> data = buildData(date, shift);
            cache.put(key, data);
            LocalDateTime now = LocalDateTime.now();
            lastRefresh.put(key, now);

            // Reset and populate metadata
            CacheMetadata meta = new CacheMetadata();
            meta.lastFullRefresh = now;
            meta.lastIncrementalRefresh = now;
            meta.fullRefreshCount = 0;

            @SuppressWarnings("unchecked")
            List<String> activeSeqs = (List<String>) data.get("activeSequences");
            if (activeSeqs != null) {
                meta.cachedActiveSequences.addAll(activeSeqs);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> seriesList = (List<Map<String, Object>>) data.get("series");
            if (seriesList != null) {
                for (Map<String, Object> s : seriesList) {
                    String seq = (String) s.get("sequence");
                    if (seq != null) {
                        meta.cachedSeriesMap.computeIfAbsent(seq, k -> new ArrayList<>()).add(s);
                    }
                }
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stockRacks = (List<Map<String, Object>>) data.get("stockRacks");
            meta.cachedStockRacks = stockRacks;
            meta.stockRacksLoadedAt = now;

            metadata.put(key, meta);
            log.info("Full cache refresh completed for {}", key);
            return data;
        } catch (Exception e) {
            log.error("Cache refresh failed for {}|{}", date, shift, e);
            lastRefresh.put(key, LocalDateTime.now().minusSeconds(20));
            Map<String, Object> fallback = cache.get(key);
            return fallback != null ? fallback : buildErrorResponse(date, shift, e);
        } finally {
            rebuildInProgress.remove(key);
        }
    }

    /**
     * Force a full reload for the given (date, shift), resetting all incremental
     * counters and metadata.
     */
    public synchronized Map<String, Object> reloadAll(LocalDate date, int shift) {
        String key = key(date, shift);
        log.info("Forced full reload for {}", key);
        Map<String, Integer> corrections = runProductionNormalization(true);
        ordonnancementService.invalidateTimelineCache();
        metadata.remove(key);
        cache.remove(key);
        lastRefresh.remove(key);
        Map<String, Object> data = refresh(date, shift);
        Map<String, Object> engineState = buildEngineState();
        data.put("engineState", engineState);
        if (sequenceFocusService != null) {
            sequenceFocusService.updateEngineState(data.get("sequenceFocus"), engineState);
        }
        data.put("_corrections", corrections);
        return data;
    }

    /**
     * Incremental refresh: only reloads what changed since the last refresh.
     *
     * <p>CRITICAL: when the engine is running (WARMING/IMPROVING) we MUST rebuild
     * liveCharge and zoneLoad on every poll because the engine's bestAssignment
     * changes continuously even when no DB rows changed. Skipping this makes the
     * UI appear frozen — the engine is working but the user sees stale proposals.</p>
     */
    public Map<String, Object> refreshIncremental(LocalDate date, int shift) {
        String key = key(date, shift);
        long startMs = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        CacheMetadata meta = metadata.computeIfAbsent(key, k -> new CacheMetadata());
        EngineState engineState = optimizerService.getState();
        boolean engineRunning = engineState == EngineState.WARMING || engineState == EngineState.IMPROVING;

        // 1. Load active sequences (always fast)
        long t1 = System.currentTimeMillis();
        Set<String> newActiveSequences = new HashSet<>();
        for (Object[] r : cuttingRequestRepository.findActiveDueOnOrBeforeLight(date, String.valueOf(shift))) {
            String seq = (String) r[0];
            if (seq != null) {
                newActiveSequences.add(seq);
            }
        }
        long seqLoadMs = System.currentTimeMillis() - t1;

        // 2. Compare with cached active sequences
        Set<String> oldActiveSequences = new HashSet<>(meta.cachedActiveSequences);

        Set<String> addedSequences = new HashSet<>(newActiveSequences);
        addedSequences.removeAll(oldActiveSequences);

        Set<String> removedSequences = new HashSet<>(oldActiveSequences);
        removedSequences.removeAll(newActiveSequences);

        Set<String> changedSequences = new HashSet<>();
        if (meta.lastIncrementalRefresh != null && !oldActiveSequences.isEmpty()) {
            long t2 = System.currentTimeMillis();
            long changedCount = serieDataRepository.countChangedSince(meta.lastIncrementalRefresh);
            if (changedCount > 0) {
                List<String> changedSeqList = serieDataRepository.findSequencesWithChangesSince(meta.lastIncrementalRefresh);
                for (String seq : changedSeqList) {
                    if (oldActiveSequences.contains(seq) && newActiveSequences.contains(seq)) {
                        changedSequences.add(seq);
                    }
                }
            }
            log.debug("Change detection took {} ms — changedCount={}, changedSequences={}",
                    System.currentTimeMillis() - t2, changedCount, changedSequences.size());
        }

        boolean sequencesChanged = !addedSequences.isEmpty()
                || !removedSequences.isEmpty()
                || !changedSequences.isEmpty();
        boolean cacheEmpty = cache.get(key) == null;
        boolean forceFullRebuild = meta.lastFullRefresh == null
                || meta.fullRefreshCount >= FORCE_FULL_REFRESH_EVERY;

        // When engine is running we MUST rebuild liveCharge/zoneLoad because
        // the engine's bestAssignment evolves every iteration. Otherwise the UI
        // shows frozen proposals and the user thinks dispatching is broken.
        boolean needDerivedDataRebuild = sequencesChanged || forceFullRebuild || cacheEmpty || engineRunning;

        // 3. Update cached series map
        long t3 = System.currentTimeMillis();
        for (String seq : removedSequences) {
            meta.cachedSeriesMap.remove(seq);
        }

        if (!addedSequences.isEmpty()) {
            List<String> batch = new ArrayList<>(addedSequences);
            for (int i = 0; i < batch.size(); i += SQL_BATCH_SIZE) {
                List<String> chunk = batch.subList(i, Math.min(i + SQL_BATCH_SIZE, batch.size()));
                meta.cachedSeriesMap.putAll(loadSeriesBatch(chunk));
            }
        }

        if (!changedSequences.isEmpty()) {
            List<String> batch = new ArrayList<>(changedSequences);
            for (int i = 0; i < batch.size(); i += SQL_BATCH_SIZE) {
                List<String> chunk = batch.subList(i, Math.min(i + SQL_BATCH_SIZE, batch.size()));
                meta.cachedSeriesMap.putAll(loadSeriesBatch(chunk));
            }
        }
        long seriesUpdateMs = System.currentTimeMillis() - t3;

        meta.cachedActiveSequences.clear();
        meta.cachedActiveSequences.addAll(newActiveSequences);

        // 4. TTL checks for stock racks and boxes
        boolean needStockRacks = meta.cachedStockRacks == null
                || meta.stockRacksLoadedAt == null
                || meta.stockRacksLoadedAt.plusSeconds(STOCK_RACKS_TTL_SECONDS).isBefore(now);

        // 5. Short-circuit ONLY when engine is idle and nothing changed
        if (!needDerivedDataRebuild && !needStockRacks && cache.containsKey(key)) {
            long totalMs = System.currentTimeMillis() - startMs;
            log.info("CACHE FAST-PATH for {} — {} ms (engine={}, seqLoad={} ms, seriesUpdate={} ms)",
                    key, totalMs, engineState, seqLoadMs, seriesUpdateMs);
            meta.lastIncrementalRefresh = now;
            meta.fullRefreshCount++;
            lastRefresh.put(key, now);
            return cache.get(key);
        }

        // If another thread is already rebuilding this key, return stale data immediately
        if (!rebuildInProgress.add(key)) {
            Map<String, Object> stale = cache.get(key);
            if (stale != null) {
                log.debug("Returning stale cache for {} while rebuild in progress", key);
                return stale;
            }
        }

        try {
        // 6. Build response map
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("date", date.toString());
        response.put("shift", shift);

        if (needDerivedDataRebuild) {
            long t4 = System.currentTimeMillis();
            response.put("liveCharge", liveChargeService.compute());
            long liveChargeMs = System.currentTimeMillis() - t4;

            long t5 = System.currentTimeMillis();
            response.put("zoneLoad", zoneLoadService.computeMatrix(date, shift));
            long zoneLoadMs = System.currentTimeMillis() - t5;

            long t6 = System.currentTimeMillis();
            response.put("gantt", ordonnancementService.getTimelineData(12, 12, Collections.emptyList()));
            long ganttMs = System.currentTimeMillis() - t6;

            long t7 = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            Map<String, Object> ganttForShiftComp = (Map<String, Object>) response.get("gantt");
            response.put("shiftCompletion", ordonnancementService.computeShiftCompletion(ganttForShiftComp));
            long shiftCompMs = System.currentTimeMillis() - t7;

            long t8 = System.currentTimeMillis();
            response.put("nonImportedCharge", nonImportedChargeService.computeCharge(date, String.valueOf(shift)));
            long nonImpMs = System.currentTimeMillis() - t8;

            long t9 = System.currentTimeMillis();
            response.put("tableFeed", tableFeedRankingService.compute(date, shift, 3));
            long tableFeedMs = System.currentTimeMillis() - t9;

            log.info("DERIVED-DATA REBUILD for {} — liveCharge={} ms, zoneLoad={} ms, gantt={} ms, shiftCompletion={} ms, nonImported={} ms, tableFeed={} ms, engineRunning={}",
                    key, liveChargeMs, zoneLoadMs, ganttMs, shiftCompMs, nonImpMs, tableFeedMs, engineRunning);

            meta.lastFullRefresh = now;
            meta.fullRefreshCount = 0;
        } else {
            Map<String, Object> oldData = cache.get(key);
            if (oldData != null) {
                response.put("liveCharge", oldData.get("liveCharge"));
                response.put("zoneLoad", oldData.get("zoneLoad"));
                response.put("gantt", oldData.get("gantt"));
                response.put("shiftCompletion", oldData.get("shiftCompletion"));
                response.put("nonImportedCharge", oldData.get("nonImportedCharge"));
                response.put("tableFeed", oldData.get("tableFeed"));
            }
            meta.fullRefreshCount++;
        }

        // 7. Series: flatten cachedSeriesMap
        List<Map<String, Object>> allSeries = new ArrayList<>();
        for (List<Map<String, Object>> seqSeries : meta.cachedSeriesMap.values()) {
            allSeries.addAll(seqSeries);
        }
        response.put("activeSequences", new ArrayList<>(meta.cachedActiveSequences));
        response.put("series", allSeries);

        // 8. Stock racks
        if (needStockRacks) {
            long t9 = System.currentTimeMillis();
            meta.cachedStockRacks = loadStockRacks();
            meta.stockRacksLoadedAt = now;
            log.debug("Stock racks reload took {} ms", System.currentTimeMillis() - t9);
        }
        response.put("stockRacks", meta.cachedStockRacks);

        if (sequenceFocusService != null && response.get("liveCharge") instanceof LiveChargeDto) {
            long t10 = System.currentTimeMillis();
            response.put("sequenceFocus", sequenceFocusService.build(
                    date, shift, (LiveChargeDto) response.get("liveCharge"),
                    meta.cachedStockRacks, engineState));
            log.debug("Sequence focus rebuild took {} ms", System.currentTimeMillis() - t10);
        } else {
            Map<String, Object> oldData = cache.get(key);
            if (oldData != null) {
                response.put("sequenceFocus", oldData.get("sequenceFocus"));
            }
        }

        response.put("_cachedAt", now.toString());

        cache.put(key, response);
        meta.lastIncrementalRefresh = now;
        lastRefresh.put(key, now);

        long totalMs = System.currentTimeMillis() - startMs;
        log.info("CACHE REBUILD for {} — {} ms (engine={}, addedSeq={}, removedSeq={}, changedSeq={}, stockReload={}, seqLoad={} ms, seriesUpdate={} ms)",
                key, totalMs, engineState, addedSequences.size(), removedSequences.size(),
                changedSequences.size(), needStockRacks, seqLoadMs, seriesUpdateMs);

        return response;
        } finally {
            rebuildInProgress.remove(key);
        }
    }

    /**
     * If the engine is idle or stopped, preloads a snapshot so the next
     * optimisation run starts faster.
     */
    public void preloadEngineIfIdle(LocalDate date, int shift) {
        EngineState es = optimizerService.getState();
        if (es == EngineState.IDLE || es == EngineState.STOPPED) {
            String key = key(date, shift);
            long nowMs = System.currentTimeMillis();
            Long last = lastEnginePreloadMsByKey.get(key);
            if (last != null && nowMs - last < ENGINE_PRELOAD_INTERVAL_MS) {
                return;
            }
            if (!enginePreloadInProgress.add(key)) {
                return;
            }
            log.debug("Preloading engine snapshot for {}", key);
            try {
                lastEnginePreloadMsByKey.put(key, nowMs);
                optimizerService.preloadSnapshot(date, shift);
            } catch (Exception e) {
                log.warn("Engine preloading failed for {}", key, e);
            } finally {
                enginePreloadInProgress.remove(key);
            }
        }
    }

    /**
     * Invalidate cached entry for a (date, shift).
     */
    public void invalidate(LocalDate date, int shift) {
        String key = key(date, shift);
        cache.remove(key);
        lastRefresh.remove(key);
        metadata.remove(key);
    }

    public void invalidateAll() {
        cache.clear();
        lastRefresh.clear();
        metadata.clear();
    }

    /** Background refresh — triggered by @Scheduled. */
    @Scheduled(fixedDelay = 10000)
    public void scheduledRefresh() {
        if (!dispatcherProperties.isEnabled()) {
            return;
        }

        // Throttled sequenceStatus auto-correction (once per 5 min)
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastProductionNormalizationMs > PRODUCTION_NORMALIZATION_INTERVAL_MS) {
            try {
                runProductionNormalization(false);
            } catch (Exception e) {
                log.warn("Scheduled production pass-through normalization failed, will retry later", e);
            } finally {
                lastProductionNormalizationMs = nowMs;
            }
        }
        if (nowMs - lastSequenceStatusCorrectionMs > SEQUENCE_STATUS_CORRECTION_INTERVAL_MS) {
            try {
                int corrected = serieStatusDateValidator.autoCorrectSequenceStatuses();
                if (corrected > 0) {
                    log.info("Scheduled sequenceStatus auto-correction: {} sequences marked COMPLETED", corrected);
                }
                lastSequenceStatusCorrectionMs = nowMs;
            } catch (Exception e) {
                log.warn("Scheduled sequenceStatus auto-correction failed, will retry later", e);
            }
        }
        if (nowMs - lastGroundUpEngineReloadMs > GROUND_UP_ENGINE_RELOAD_INTERVAL_MS) {
            try {
                Map<String, Object> engineReload = optimizerService.reloadActiveSnapshotFromGroundTruth();
                log.info("Scheduled 10-minute engine ground-up reload: {}", engineReload.get("status"));
            } catch (Exception e) {
                log.warn("Scheduled engine ground-up reload failed", e);
            } finally {
                lastGroundUpEngineReloadMs = nowMs;
            }
        }

        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        refreshIfNeeded(slot.date, slot.shift);
    }

    // ------------------------------------------------------------------ internals

    private void refreshIfNeeded(LocalDate date, int shift) {
        String key = key(date, shift);
        if (isStale(key)) {
            try {
                refreshIncremental(date, shift);
            } catch (Exception e) {
                log.warn("Scheduled incremental refresh failed for {}, falling back to full refresh", key, e);
                refresh(date, shift);
            }
        }
    }

    private Map<String, Integer> runProductionNormalization(boolean reloadEngine) {
        Map<String, Integer> corrections = serieStatusDateValidator.normalizeProductionProgress();
        int total = 0;
        for (Integer v : corrections.values()) {
            if (v != null) total += v;
        }
        if (total > 0) {
            log.info("Production progress normalized: {}", corrections);
            cache.clear();
            metadata.clear();
            lastRefresh.clear();
            ordonnancementService.invalidateTimelineCache();
            if (reloadEngine) {
                optimizerService.reloadActiveSnapshotFromGroundTruth();
            } else {
                optimizerService.requestRebuild();
            }
        } else if (reloadEngine) {
            optimizerService.reloadActiveSnapshotFromGroundTruth();
        }
        return corrections;
    }

    private boolean isStale(String key) {
        LocalDateTime last = lastRefresh.get(key);
        if (last == null) {
            return true;
        }
        EngineState es = optimizerService.getState();
        boolean engineRunning = es == EngineState.WARMING || es == EngineState.IMPROVING;
        int ttlSeconds = engineRunning ? 5 : 60;
        return last.plusSeconds(ttlSeconds).isBefore(LocalDateTime.now());
    }

    private boolean engineStateChanged() {
        EngineState current = optimizerService.getState();
        boolean changed = lastCachedEngineState == null || current != lastCachedEngineState;
        lastCachedEngineState = current;
        return changed;
    }

    private String key(LocalDate date, int shift) {
        return date.toString() + "|" + shift;
    }

    private Map<String, List<Map<String, Object>>> loadSeriesBatch(List<String> batch) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (Object[] row : serieDataRepository.findSeriesBySequencesLightProjection(batch)) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("serie", row[0]);
            s.put("sequence", row[1]);
            s.put("machine", row[2]);
            s.put("tempsDeCoupe", row[3]);
            s.put("nbrCouche", row[4]);
            s.put("placement", row[5]);
            String seq = (String) row[1];
            if (seq != null) {
                result.computeIfAbsent(seq, k -> new ArrayList<>()).add(s);
            }
        }
        return result;
    }

    private List<Map<String, Object>> loadStockRacks() {
        List<Map<String, Object>> stockRacks = new ArrayList<>();
        for (Object[] row : scanRouleauRepository.findAllLight()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("serialId", row[0]);
            m.put("reftissu", row[1]);
            m.put("quantite", row[2]);
            m.put("emplacement", row[3]);
            m.put("lot", row[4]);
            m.put("metrage", row[5]);
            stockRacks.add(m);
        }
        return stockRacks;
    }

    private Map<String, Object> buildData(LocalDate date, int shift) {
        // 0. Auto-correct sequence statuses based on WO completion (throttled)
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastSequenceStatusCorrectionMs > SEQUENCE_STATUS_CORRECTION_INTERVAL_MS) {
            try {
                int corrected = serieStatusDateValidator.autoCorrectSequenceStatuses();
                if (corrected > 0) {
                    log.info("SequenceStatus auto-correction completed: {} sequences marked COMPLETED", corrected);
                }
                lastSequenceStatusCorrectionMs = nowMs;
            } catch (Exception e) {
                log.warn("SequenceStatus auto-correction failed, will retry later", e);
            }
        }

        // 1. Core workbench data
        LiveChargeDto liveCharge = liveChargeService.compute();
        ZoneLoadDto zoneLoad = zoneLoadService.computeMatrix(date, shift);
        Map<String, Object> gantt = ordonnancementService.getTimelineData(12, 12, Collections.emptyList());
        Map<String, Object> shiftCompletion = ordonnancementService.computeShiftCompletion(gantt);

        // 2. Active sequences due in the requested shift or older
        List<String> activeSequences = new ArrayList<>();
        for (Object[] r : cuttingRequestRepository.findActiveDueOnOrBeforeLight(date, String.valueOf(shift))) {
            String seq = (String) r[0];
            if (seq != null) {
                activeSequences.add(seq);
            }
        }

        // 3. Series for active sequences (batched)
        List<Map<String, Object>> seriesList = new ArrayList<>();
        for (int i = 0; i < activeSequences.size(); i += SQL_BATCH_SIZE) {
            List<String> batch = activeSequences.subList(i, Math.min(i + SQL_BATCH_SIZE, activeSequences.size()));
            for (Object[] row : serieDataRepository.findSeriesBySequencesLightProjection(batch)) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("serie", row[0]);
                s.put("sequence", row[1]);
                s.put("machine", row[2]);
                s.put("tempsDeCoupe", row[3]);
                s.put("nbrCouche", row[4]);
                s.put("placement", row[5]);
                seriesList.add(s);
            }
        }

        // 4. Stock availability — ScanRouleau (all racks, light projection)
        List<Map<String, Object>> stockRacks = loadStockRacks();

        // 5. Non-imported charge (Order_Schedule status = 'F')
        Map<String, Object> nonImportedCharge = nonImportedChargeService.computeCharge(date, String.valueOf(shift));
        TableFeedDto tableFeed = tableFeedRankingService.compute(date, shift, 3);

        // 6. Sequence focus — chef/logistics view built from the balanced
        // schedule + existing light stock projection.
        Map<String, Object> sequenceFocus = sequenceFocusService != null
                ? sequenceFocusService.build(date, shift, liveCharge, stockRacks, optimizerService.getState())
                : Collections.emptyMap();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date.toString());
        out.put("shift", shift);
        out.put("liveCharge", liveCharge);
        out.put("zoneLoad", zoneLoad);
        out.put("gantt", gantt);
        out.put("shiftCompletion", shiftCompletion);
        out.put("activeSequences", activeSequences);
        out.put("series", seriesList);
        out.put("stockRacks", stockRacks);
        out.put("nonImportedCharge", nonImportedCharge);
        out.put("tableFeed", tableFeed);
        out.put("sequenceFocus", sequenceFocus);
        out.put("_cachedAt", LocalDateTime.now().toString());
        return out;
    }

    private Map<String, Object> buildEngineState() {
        Map<String, Object> state = new LinkedHashMap<>();
        EngineState es = optimizerService.getState();
        state.put("state", es == null ? null : es.name());
        EngineMode mode = optimizerService.getMode();
        state.put("mode", mode == null ? null : mode.name());
        state.put("phase", optimizerService.getCurrentPhase() != null ? optimizerService.getCurrentPhase().name() : null);
        state.put("runId", optimizerService.getCurrentRunId());
        state.put("iteration", optimizerService.getIteration());
        state.put("spread", optimizerService.getCurrentSpread());
        state.put("rawSpread", optimizerService.getCurrentRawSpread());
        state.put("stdDev", optimizerService.getCurrentStdDev());
        state.put("median", optimizerService.getCurrentMedian());
        state.put("initialSpread", optimizerService.getInitialSpread());
        state.put("lastImprovement", optimizerService.getLastImprovement());
        state.put("bestSpread", optimizerService.getBestSpread());
        state.put("bestStdDev", optimizerService.getBestStdDev());
        state.put("bestMedian", optimizerService.getBestMedian());
        return state;
    }

    private Map<String, Object> buildErrorResponse(LocalDate date, int shift, Exception e) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date.toString());
        out.put("shift", shift);
        out.put("error", e.getMessage());
        out.put("liveCharge", null);
        out.put("zoneLoad", null);
        out.put("gantt", null);
        out.put("tableFeed", null);
        out.put("engineState", null);
        out.put("activeSequences", Collections.emptyList());
        out.put("series", Collections.emptyList());
        out.put("stockRacks", Collections.emptyList());
        out.put("nonImportedCharge", Collections.emptyMap());
        out.put("sequenceFocus", Collections.emptyMap());
        return out;
    }
}
