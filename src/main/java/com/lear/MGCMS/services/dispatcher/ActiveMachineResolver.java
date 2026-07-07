package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.ShiftZoneConfirmationMachine;
import com.lear.MGCMS.repositories.EtatMachineHistoriqueRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.dispatcher.ShiftZoneConfirmationMachineRepository;

/**
 * Answers "which machines in this zone are scheduleable for this (date,
 * shift)?" against {@code ShiftZoneConfirmationMachine}.
 *
 * <p>Results are cached in-memory with a 30-second TTL to avoid hammering the
 * database on every cache refresh. The cache key is {@code date|shift|zone}.</p>
 *
 * <p>Returning an empty set is semantically significant: it means the chef
 * has not confirmed this zone for this shift yet, and the engine MUST
 * refuse to schedule into it. This is the admission gate.</p>
 *
 * <p>Pure read-only; no writes, no event publishing.</p>
 */
@Service
public class ActiveMachineResolver {

    private static final Logger log = LoggerFactory.getLogger(ActiveMachineResolver.class);
    private static final long CACHE_TTL_MS = 30_000;

    @Autowired
    private ShiftZoneConfirmationMachineRepository machineRepository;

    @Autowired
    private EtatMachineHistoriqueRepository etatMachineRepository;

    @Autowired
    private ProductionTableRepository productionTableRepository;

    /** Cache key = "date|shift|zone" → cached result. */
    private final Map<String, Set<String>> cache = new ConcurrentHashMap<>();
    /** Cache timestamp per key. */
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    /** Shared cache for EtatMachineHistorique fallback — keyed by dateTime. */
    private final Map<String, List<Object[]>> statusCache = new ConcurrentHashMap<>();
    private final Map<String, Long> statusCacheTimestamps = new ConcurrentHashMap<>();

    /** Shared cache for findMachinesWithTypeInZone fallback — keyed by zoneNom. */
    private final Map<String, List<Object[]>> machinesInZoneCache = new ConcurrentHashMap<>();
    private final Map<String, Long> machinesInZoneCacheTimestamps = new ConcurrentHashMap<>();

    /**
     * Names of machines the chef has flagged {@code is_up = true} for the
     * triple. Returns an empty set — not null — when no confirmation exists
     * at all or when every machine is down.
     */
    @Transactional(readOnly = true)
    public Set<String> activeMachines(LocalDate date, int shift, String zoneNom) {
        if (date == null || zoneNom == null) return Set.of();

        String key = date.toString() + "|" + shift + "|" + zoneNom;
        Long cachedAt = cacheTimestamps.get(key);
        if (cachedAt != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            Set<String> hit = cache.get(key);
            return hit != null ? hit : Set.of();
        }

        long startMs = System.currentTimeMillis();
        List<ShiftZoneConfirmationMachine> rows =
                machineRepository.findUpMachinesForTriple(date, shift, zoneNom);
        Set<String> out = new LinkedHashSet<>();
        for (ShiftZoneConfirmationMachine row : rows) {
            if (row.getMachineNom() != null) out.add(row.getMachineNom());
        }
        if (!out.isEmpty()) {
            cache.put(key, Collections.unmodifiableSet(new LinkedHashSet<>(out)));
            cacheTimestamps.put(key, System.currentTimeMillis());
            log.debug("activeMachines cache miss for {} — {} ms, {} machines (chef-confirmed)", key, System.currentTimeMillis() - startMs, out.size());
            return out;
        }

        // Fallback: no chef confirmation for this zone/shift.
        // Use EtatMachineHistorique: machine is active if status is 'M' or has no status.
        LocalDateTime dateTime = date.atTime(12, 0); // midday = during shift

        String statusKey = dateTime.toString();
        Long statusCachedAt = statusCacheTimestamps.get(statusKey);
        List<Object[]> statusRows;
        if (statusCachedAt != null && System.currentTimeMillis() - statusCachedAt < CACHE_TTL_MS) {
            List<Object[]> hit = statusCache.get(statusKey);
            statusRows = hit != null ? hit : Collections.emptyList();
        } else {
            statusRows = etatMachineRepository.findAllCurrentStatuses(dateTime);
            statusCache.put(statusKey, statusRows);
            statusCacheTimestamps.put(statusKey, System.currentTimeMillis());
        }

        Map<String, String> statusByMachine = new HashMap<>(statusRows.size() * 2);
        for (Object[] sr : statusRows) {
            statusByMachine.put((String) sr[0], (String) sr[1]);
        }

        String zoneKey = zoneNom;
        Long machinesCachedAt = machinesInZoneCacheTimestamps.get(zoneKey);
        List<Object[]> machinesInZone;
        if (machinesCachedAt != null && System.currentTimeMillis() - machinesCachedAt < CACHE_TTL_MS) {
            List<Object[]> hit = machinesInZoneCache.get(zoneKey);
            machinesInZone = hit != null ? hit : Collections.emptyList();
        } else {
            machinesInZone = productionTableRepository.findMachinesWithTypeInZone(zoneNom);
            machinesInZoneCache.put(zoneKey, machinesInZone);
            machinesInZoneCacheTimestamps.put(zoneKey, System.currentTimeMillis());
        }

        for (Object[] mr : machinesInZone) {
            String machineNom = (String) mr[0];
            String codeEtat = statusByMachine.get(machineNom);
            if (codeEtat == null || "M".equalsIgnoreCase(codeEtat)) {
                out.add(machineNom);
            }
        }

        cache.put(key, Collections.unmodifiableSet(new LinkedHashSet<>(out)));
        cacheTimestamps.put(key, System.currentTimeMillis());
        long elapsedMs = System.currentTimeMillis() - startMs;
        if (elapsedMs > 100) {
            log.warn("activeMachines cache miss for {} — {} ms (fallback path)", key, elapsedMs);
        } else {
            log.debug("activeMachines cache miss for {} — {} ms, {} machines (fallback)", key, elapsedMs, out.size());
        }
        return out;
    }

    /**
     * Invalidate cached entries for a specific (date, shift). Called by
     * WorkbenchCacheService when a full reload happens so machine status
     * changes are picked up immediately.
     */
    public void invalidateCache(LocalDate date, int shift) {
        String prefix = date.toString() + "|" + shift + "|";
        int removed = 0;
        for (String key : new LinkedHashSet<>(cache.keySet())) {
            if (key.startsWith(prefix)) {
                cache.remove(key);
                cacheTimestamps.remove(key);
                removed++;
            }
        }
        // Also clear shared fallback caches so fresh data is picked up immediately.
        statusCache.clear();
        statusCacheTimestamps.clear();
        machinesInZoneCache.clear();
        machinesInZoneCacheTimestamps.clear();
        if (removed > 0) {
            log.debug("Invalidated {} activeMachines cache entries for {}|{}", removed, date, shift);
        }
    }

    /**
     * True iff the chef de zone has *explicitly* confirmed the zone for this
     * triple — i.e. there is at least one row in
     * {@code ShiftZoneConfirmationMachine} marked is_up. The M-only fallback
     * is NOT considered: this method's contract is "did the chef sign off",
     * not "could the engine schedule here". Used by admission gates and
     * the "non confirmée" banner in Process.
     */
    public boolean isZoneConfirmed(LocalDate date, int shift, String zoneNom) {
        if (date == null || zoneNom == null) return false;
        return !machineRepository.findUpMachinesForTriple(date, shift, zoneNom).isEmpty();
    }

    /**
     * True iff the zone has at least one machine the engine can schedule onto
     * — chef confirmation OR M-only fallback. This is the relaxed predicate;
     * use {@link #isZoneConfirmed} when you specifically need the chef gate.
     */
    public boolean hasActiveMachines(LocalDate date, int shift, String zoneNom) {
        return !activeMachines(date, shift, zoneNom).isEmpty();
    }
}
