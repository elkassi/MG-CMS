package com.lear.MGCMS.services.dispatcher;

import com.lear.MGCMS.domain.ScanRouleau;
import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.logistics.AllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Per-snapshot material availability checker.
 *
 * <p>Status is <b>advisory</b>, not a hard constraint. The engine uses it to
 * (a) flag sequences for the logistics team and (b) bias zone selection
 * slightly. {@link MaterialStatus#NOT_IN_ZONE} no longer blocks dispatch —
 * the engine can still propose a zone with no in-rack material, the cost
 * just goes up by a small tunable weight.</p>
 *
 * <p>Source of truth: {@link ScanRouleau} (physical rolls scanned into a
 * zone's rack locations). External stock APIs are not consulted — per the
 * Option C scope, material is for alerting logistics, not gating dispatch.</p>
 *
 * <p>Results are cached per snapshot so the same {@code (zone, refTissus)}
 * pair is not re-queried within an engine iteration.</p>
 */
@Service
public class MaterialAvailabilityChecker {

    public enum MaterialStatus {
        /** Roll for this fabric reference is on a rack in the target zone. */
        AVAILABLE_IN_ZONE,
        /**
         * No roll for this fabric reference is on a rack in the target zone.
         * Advisory only — the engine may still dispatch here. Logistics
         * should be alerted to transfer or replenish.
         */
        NOT_IN_ZONE
    }

    @Autowired
    private ScanRouleauRepository scanRouleauRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private SerieRouleauTempService serieRouleauTempService;

    /** Snapshot-scoped cache: zoneNom → sorted-refTissus-key → result map. */
    private final Map<String, Map<String, Map<String, MaterialStatus>>> snapshotCache = new HashMap<>();

    /** Clear at the start of every engine snapshot. */
    public void clearSnapshotCache() {
        snapshotCache.clear();
    }

    /**
     * Check material availability for a set of fabric references in a zone.
     *
     * @param refTissus fabric references (e.g. part-number materials)
     * @param zoneNom   target zone name
     * @return map refTissu → status (only {@code AVAILABLE_IN_ZONE} or
     *         {@code NOT_IN_ZONE})
     */
    public Map<String, MaterialStatus> check(Set<String> refTissus, String zoneNom) {
        if (refTissus == null || refTissus.isEmpty() || zoneNom == null || zoneNom.isBlank()) {
            return Collections.emptyMap();
        }

        String cacheKey = refTissus.stream()
                .map(MaterialAvailabilityChecker::normalizeMaterial)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.joining(","));
        Map<String, Map<String, MaterialStatus>> zoneCache = snapshotCache.computeIfAbsent(zoneNom, k -> new HashMap<>());
        Map<String, MaterialStatus> cached = zoneCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Zone zone = zoneRepository.findById(zoneNom).orElse(null);
        if (zone == null) {
            Map<String, MaterialStatus> allMissing = refTissus.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(r -> r, r -> MaterialStatus.NOT_IN_ZONE));
            zoneCache.put(cacheKey, allMissing);
            return allMissing;
        }

        List<String> locations = parseLocations(zone.getRollLocations());
        Map<String, Double> metersByMaterial = new HashMap<>();
        Map<String, String> materialBySerial = new HashMap<>();
        if (!locations.isEmpty()) {
            List<ScanRouleau> rolls = scanRouleauRepository.findByLocations(locations);
            for (ScanRouleau roll : rolls) {
                String rt = normalizeMaterial(roll.getReftissu());
                if (rt == null) continue;
                double meters = meters(roll);
                if (meters <= 0) continue;
                metersByMaterial.merge(rt, meters, Double::sum);
                if (roll.getSerialId() != null) {
                    materialBySerial.put(roll.getSerialId().trim(), rt);
                }
            }
        }
        subtractReservations(metersByMaterial, zoneNom);
        subtractOnTable(metersByMaterial, materialBySerial);

        Map<String, MaterialStatus> result = new HashMap<>();
        for (String rt : refTissus) {
            if (rt == null) continue;
            String key = normalizeMaterial(rt);
            if (key == null) continue;
            result.put(key,
                    metersByMaterial.getOrDefault(key, 0.0) > 0.0
                            ? MaterialStatus.AVAILABLE_IN_ZONE
                            : MaterialStatus.NOT_IN_ZONE);
        }

        zoneCache.put(cacheKey, result);
        return result;
    }

    /**
     * Convenience: count how many fabric references are NOT on a rack in
     * the target zone. Used by the engine cost function as a soft penalty.
     */
    public int countNotInZone(Set<String> refTissus, String zoneNom) {
        if (refTissus == null || refTissus.isEmpty()) return 0;
        Map<String, MaterialStatus> statuses = check(refTissus, zoneNom);
        int n = 0;
        for (MaterialStatus s : statuses.values()) {
            if (s == MaterialStatus.NOT_IN_ZONE) n++;
        }
        return n;
    }

    private List<String> parseLocations(String rollLocations) {
        if (rollLocations == null || rollLocations.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rollLocations.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String normalizeMaterial(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (v.startsWith("P") && v.length() > 1) {
            v = v.substring(1);
        }
        return v.toUpperCase(Locale.ROOT);
    }

    private double meters(ScanRouleau roll) {
        if (roll == null) return 0.0;
        if (roll.getMetrage() != null) return Math.max(0.0, roll.getMetrage());
        if (roll.getQuantite() == null) return 0.0;
        try {
            return Math.max(0.0, Double.parseDouble(roll.getQuantite().trim()));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void subtractReservations(Map<String, Double> metersByMaterial, String zoneNom) {
        Map<String, Double> reserved = allocationService.reservedMetersByMaterialZone();
        if (reserved == null || reserved.isEmpty()) return;
        for (Map.Entry<String, Double> entry : reserved.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            int sep = key.lastIndexOf('|');
            if (sep <= 0 || sep >= key.length() - 1) continue;
            String material = normalizeMaterial(key.substring(0, sep));
            String zone = key.substring(sep + 1);
            double meters = entry.getValue() != null ? entry.getValue() : 0.0;
            if (material != null && zoneNom.equalsIgnoreCase(zone) && meters > 0) {
                metersByMaterial.put(material, Math.max(0.0, metersByMaterial.getOrDefault(material, 0.0) - meters));
            }
        }
    }

    private void subtractOnTable(Map<String, Double> metersByMaterial, Map<String, String> materialBySerial) {
        List<SerieRouleauTemp> onTable = serieRouleauTempService.getAll();
        if (onTable == null || onTable.isEmpty()) return;
        for (SerieRouleauTemp roll : onTable) {
            if (roll == null || roll.getIdRouleau() == null) continue;
            String material = materialBySerial.get(roll.getIdRouleau().trim());
            if (material == null) continue;
            double rest = roll.getEstimationRest() != null ? roll.getEstimationRest() : 0.0;
            if (rest <= 0) continue;
            metersByMaterial.put(material, Math.max(0.0, metersByMaterial.getOrDefault(material, 0.0) - rest));
        }
    }
}
