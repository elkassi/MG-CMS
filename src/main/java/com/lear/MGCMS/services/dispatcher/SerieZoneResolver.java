package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ZoneRepository;

/**
 * Maps a serie's {@code machine} (machine-type NAME, e.g. {@code "Lectra"},
 * {@code "LASER-DXF"}) to the zone that should host it for a given
 * (date, shift).
 *
 * <p>The resolution waterfall is:
 * <ol>
 *   <li>Find every zone hosting at least one machine of the requested type
 *       (via {@link ProductionTableRepository#findZonesHostingMachineType}).</li>
 *   <li>Keep only active zones, preferring {@code STRICT} over {@code SHARED}
 *       — STRICT zones have dedicated machine types (FirstArticle = Lectra),
 *       SHARED zones are the spillover (Laser).</li>
 *   <li>For each candidate zone, delegate to {@link ActiveMachineResolver}:
 *       if the chef confirmed that zone for this (date, shift) AND at least
 *       one up-machine in the confirmation is of the requested type, accept
 *       the zone.</li>
 * </ol>
 *
 * <p>Returning {@link Optional#empty()} means the caller (typically
 * {@link SchedulableSerieFilter}) must record an {@code UnassignableSerie} —
 * pick the reason code based on which leg of the waterfall failed.</p>
 *
 * <p>Configuration lookups (zone definitions, machine-to-zone mappings) are
 * cached with a 30-second TTL to avoid N×M DB hits when the heatmap resolves
 * hundreds of series in quick succession.</p>
 */
@Service
public class SerieZoneResolver {

    private static final String MACHINE_TYPE_LASER = "LASER";
    private static final String MACHINE_TYPE_LASER_DXF = "LASER-DXF";
    private static final String MACHINE_TYPE_LASER_LSR = "LASER-LSR";
    private static final String MACHINE_TYPE_LECTRA = "Lectra";
    private static final String MACHINE_TYPE_LECTRA_IP6 = "Lectra IP6";

    private static final Logger log = LoggerFactory.getLogger(SerieZoneResolver.class);
    private static final long CACHE_TTL_MS = 30_000;

    @Autowired
    private ProductionTableRepository productionTableRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private ActiveMachineResolver activeMachineResolver;

    @Autowired
    private DispatcherProperties dispatcherProperties;

    @Autowired
    private ZoneLoadService zoneLoadService;

    // --- TTL caches for configuration lookups (change rarely) ---
    private final Map<String, List<String>> zonesByTypeCache = new ConcurrentHashMap<>();
    private final Map<String, Long> zonesByTypeTimestamps = new ConcurrentHashMap<>();

    private final Map<String, Optional<Zone>> zoneByNomCache = new ConcurrentHashMap<>();
    private final Map<String, Long> zoneByNomTimestamps = new ConcurrentHashMap<>();

    private final Map<String, List<Object[]>> machinesByZoneCache = new ConcurrentHashMap<>();
    private final Map<String, Long> machinesByZoneTimestamps = new ConcurrentHashMap<>();

    /**
     * Outcome of a resolution attempt — the zone (if any) plus the reason
     * nothing matched, so the caller doesn't have to re-derive it.
     */
    public static final class Resolution {
        private final Zone zone;
        private final FailureReason failureReason;

        private Resolution(Zone zone, FailureReason failureReason) {
            this.zone = zone;
            this.failureReason = failureReason;
        }

        public static Resolution accepted(Zone z) { return new Resolution(z, null); }
        public static Resolution rejected(FailureReason r) { return new Resolution(null, r); }

        public boolean isAccepted() { return zone != null; }
        public Zone getZone() { return zone; }
        public FailureReason getFailureReason() { return failureReason; }
    }

    /**
     * Why no zone accepted the serie. Maps 1:1 onto
     * {@link com.lear.MGCMS.domain.dispatcher.UnassignableSerie.ReasonCode}.
     */
    public enum FailureReason {
        /** No zone in the plant hosts this machine type — plant misconfig. */
        NO_ZONE_ACCEPTING_TYPE,
        /** Zones host this type but none confirmed for the (date, shift). */
        ALL_ZONES_CLOSED_FOR_SHIFT,
        /** Zone(s) confirmed, but no up-machine in them hosts this type. */
        NO_ACTIVE_MACHINE_IN_ZONE
    }

    /** Convenience overload for callers that only want the zone. */
    @Transactional(readOnly = true)
    public Optional<Zone> resolveZone(SerieDispatchInfo serie, LocalDate date, int shift) {
        Resolution r = resolve(serie, date, shift);
        return r.isAccepted() ? Optional.of(r.getZone()) : Optional.empty();
    }

    /**
     * Full resolution returning both the zone and, when absent, the reason.
     */
    @Transactional(readOnly = true)
    public Resolution resolve(SerieDispatchInfo serie, LocalDate date, int shift) {
        if (serie == null || serie.getMachine() == null || date == null) {
            return Resolution.rejected(FailureReason.NO_ZONE_ACCEPTING_TYPE);
        }

        List<String> requestedTypes = requestedMachineTypes(serie.getMachine());
        List<String> candidateNames = findCandidateZones(requestedTypes);
        if (candidateNames == null || candidateNames.isEmpty()) {
            return Resolution.rejected(FailureReason.NO_ZONE_ACCEPTING_TYPE);
        }
        candidateNames = preferRequestedZone(candidateNames, serie.getPreferredZoneNom());

        ZoneLoadDto loadDto = null;
        if (zoneLoadService != null) {
            loadDto = zoneLoadService.computeMatrix(date, shift);
        }
        final Map<String, Double> zoneLoadByNom = new java.util.HashMap<>();
        if (loadDto != null && loadDto.getRows() != null) {
            for (ZoneLoadDto.ZoneLoadRowDto z : loadDto.getRows()) {
                if (z != null && z.getZoneNom() != null) {
                    zoneLoadByNom.put(z.getZoneNom(), z.getLoadPct());
                }
            }
        }

        // Load and sort: active first, STRICT before SHARED, deterministic by nom.
        List<Zone> strict = new ArrayList<>();
        List<Zone> shared = new ArrayList<>();
        for (String zoneNom : candidateNames) {
            Zone z = getZoneByNom(zoneNom);
            if (z == null || !z.isActive()) continue;
            Zone.Category cat = z.getCategory();
            if (cat == Zone.Category.SHARED) shared.add(z);
            else strict.add(z);
        }
        
        // Load-aware tiebreak: sort strict list by (isConfirmed DESC, loadPct ASC, nom ASC)
        strict.sort((z1, z2) -> {
            boolean c1 = activeMachineResolver.isZoneConfirmed(date, shift, z1.getNom());
            boolean c2 = activeMachineResolver.isZoneConfirmed(date, shift, z2.getNom());
            if (c1 != c2) return c1 ? -1 : 1;
            double l1 = zoneLoadByNom.getOrDefault(z1.getNom(), 0.0);
            double l2 = zoneLoadByNom.getOrDefault(z2.getNom(), 0.0);
            int cmp = Double.compare(l1, l2);
            if (cmp != 0) return cmp;
            return z1.getNom().compareTo(z2.getNom());
        });

        boolean sawAnyCandidate = !strict.isEmpty() || !shared.isEmpty();
        if (!sawAnyCandidate) {
            // Every candidate zone is inactive — treat as misconfig.
            return Resolution.rejected(FailureReason.NO_ZONE_ACCEPTING_TYPE);
        }

        // STRICT tier first, then SHARED fallback.
        boolean anyConfirmed = false;
        for (Zone z : strict) {
            Resolution r = tryZone(z, requestedTypes, date, shift);
            if (r.isAccepted()) return r;
            if (r.getFailureReason() == FailureReason.NO_ACTIVE_MACHINE_IN_ZONE) {
                anyConfirmed = true;
            }
        }
        for (Zone z : shared) {
            Resolution r = tryZone(z, requestedTypes, date, shift);
            if (r.isAccepted()) return r;
            if (r.getFailureReason() == FailureReason.NO_ACTIVE_MACHINE_IN_ZONE) {
                anyConfirmed = true;
            }
        }

        return Resolution.rejected(anyConfirmed
                ? FailureReason.NO_ACTIVE_MACHINE_IN_ZONE
                : FailureReason.ALL_ZONES_CLOSED_FOR_SHIFT);
    }

    /**
     * Checks one zone: is it confirmed for the shift, and does it have an
     * up-machine of the requested type?
     */
    private Resolution tryZone(Zone z, List<String> machineTypeNames, LocalDate date, int shift) {
        java.util.Set<String> up = activeMachineResolver.activeMachines(date, shift, z.getNom());
        List<Object[]> machinesInZone = getMachinesInZone(z.getNom());
        boolean hostsRequestedType = false;
        for (Object[] row : machinesInZone) {
            String machineNom = (String) row[0];
            String typeName   = (String) row[1];
            if (!matchesRequestedType(machineTypeNames, typeName)) {
                continue;
            }
            hostsRequestedType = true;
            if (up.contains(machineNom)) {
                return Resolution.accepted(z);
            }
        }

        if (up.isEmpty()) {
            if (dispatcherProperties.isAllowUnconfirmedZones() && hostsRequestedType) {
                return Resolution.accepted(z);
            }
            return Resolution.rejected(FailureReason.ALL_ZONES_CLOSED_FOR_SHIFT);
        }
        return Resolution.rejected(FailureReason.NO_ACTIVE_MACHINE_IN_ZONE);
    }

    private List<String> findCandidateZones(List<String> machineTypes) {
        for (String machineType : machineTypes) {
            List<String> found = getZonesHostingMachineType(machineType);
            if (found != null && !found.isEmpty()) {
                return found;
            }
        }
        return Collections.emptyList();
    }

    // ------------------------------------------------------------------
    // Cached configuration lookups
    // ------------------------------------------------------------------

    private List<String> getZonesHostingMachineType(String machineType) {
        Long cachedAt = zonesByTypeTimestamps.get(machineType);
        if (cachedAt != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            return zonesByTypeCache.get(machineType);
        }
        long start = System.currentTimeMillis();
        List<String> result = productionTableRepository.findZonesHostingMachineType(machineType);
        if (result == null) {
            result = Collections.emptyList();
        }
        zonesByTypeCache.put(machineType, result);
        zonesByTypeTimestamps.put(machineType, System.currentTimeMillis());
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 50) {
            log.warn("SerieZoneResolver cache miss findZonesHostingMachineType({}) took {} ms", machineType, elapsed);
        } else {
            log.debug("SerieZoneResolver cache miss findZonesHostingMachineType({}) took {} ms", machineType, elapsed);
        }
        return result;
    }

    private Zone getZoneByNom(String zoneNom) {
        Long cachedAt = zoneByNomTimestamps.get(zoneNom);
        if (cachedAt != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            Optional<Zone> opt = zoneByNomCache.get(zoneNom);
            return opt != null ? opt.orElse(null) : null;
        }
        Zone z = zoneRepository.findByObjId(zoneNom);
        zoneByNomCache.put(zoneNom, Optional.ofNullable(z));
        zoneByNomTimestamps.put(zoneNom, System.currentTimeMillis());
        return z;
    }

    private List<Object[]> getMachinesInZone(String zoneNom) {
        Long cachedAt = machinesByZoneTimestamps.get(zoneNom);
        if (cachedAt != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            return machinesByZoneCache.get(zoneNom);
        }
        List<Object[]> result = productionTableRepository.findMachinesWithTypeInZone(zoneNom);
        if (result == null) {
            result = Collections.emptyList();
        }
        machinesByZoneCache.put(zoneNom, result);
        machinesByZoneTimestamps.put(zoneNom, System.currentTimeMillis());
        return result;
    }

    // ------------------------------------------------------------------
    // Static helpers
    // ------------------------------------------------------------------

    private static List<String> requestedMachineTypes(String machineTypeName) {
        if (machineTypeName == null) return Collections.emptyList();
        String trimmed = machineTypeName.trim();
        if (trimmed.isEmpty()) return Collections.emptyList();

        Set<String> requested = new LinkedHashSet<>();
        requested.add(trimmed);
        if (MACHINE_TYPE_LASER.equalsIgnoreCase(trimmed)
                || MACHINE_TYPE_LASER_LSR.equalsIgnoreCase(trimmed)) {
            requested.add(MACHINE_TYPE_LASER_DXF);
        }
        return new ArrayList<>(requested);
    }

    private static boolean matchesRequestedType(List<String> requestedTypes, String actualType) {
        if (requestedTypes == null || requestedTypes.isEmpty() || actualType == null) {
            return false;
        }
        for (String requested : requestedTypes) {
            if (actualType.equalsIgnoreCase(requested)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> preferRequestedZone(List<String> candidateNames, String preferredZoneNom) {
        if (candidateNames == null || candidateNames.isEmpty()
                || preferredZoneNom == null || preferredZoneNom.trim().isEmpty()) {
            return candidateNames;
        }

        List<String> reordered = new ArrayList<>(candidateNames.size());
        for (String candidate : candidateNames) {
            if (preferredZoneNom.equalsIgnoreCase(candidate)) {
                reordered.add(candidate);
                break;
            }
        }
        for (String candidate : candidateNames) {
            boolean alreadyAdded = false;
            for (String added : reordered) {
                if (added.equalsIgnoreCase(candidate)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                reordered.add(candidate);
            }
        }
        return reordered;
    }
}
