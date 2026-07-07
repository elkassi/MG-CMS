package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterial;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacement;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumber;
import com.lear.MGCMS.domain.CuttingPlan.PartNumberCorrespendance;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestPartNumber;
import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerie;
import com.lear.MGCMS.services.CuttingPlan.PartNumberCorrespendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes the cut perimeter of each part number (D,6) in a placement, mirroring
 * the geometry parse the existing {@code /api/placementData/rapport} endpoint uses
 * (sum of {@code hypot} between consecutive X/Y points per M15 segment, divided by
 * 42), but grouped per part number instead of per whole placement.
 *
 * <p>The result is cached onto {@code CuttingPlanPartNumber.perimetre} and
 * {@code CuttingRequestPartNumber.perimetre}; the Plan de Charge part-number report
 * uses the perimeter share to split a plan's cutting time across its part numbers.</p>
 *
 * <p>Cut files are read from disk (same folder fallbacks + windows-1252 as the
 * placement endpoints). All parsing is defensive: a missing/unreadable file or a
 * malformed token yields a smaller (or empty) perimeter rather than an exception,
 * so it can never block a plan save.</p>
 */
@Service
public class PerimetreService {

    private static final double UNIT_DIVISOR = 42.0; // same convention as /rapport

    @Value("${lear.cutfilesFolder}")
    private String cutfilesFolder;
    @Value("${lear.cutfilesArchiveFolder}")
    private String cutfilesArchiveFolder;
    @Value("${lear.cutfilesAblLaserFolder}")
    private String cutfilesAblLaserFolder;

    @Autowired
    private PartNumberCorrespendanceService partNumberCorrespendanceService;

    /** Read a cut file by placement name, trying the same folders as PlacementDataController. */
    private String[] readPlacementLines(String placement) {
        String[] candidates = {
                cutfilesFolder + placement,
                cutfilesAblLaserFolder + placement,
                cutfilesFolder + "IP6\\" + placement,
                cutfilesFolder + "Archive\\" + placement,
                cutfilesArchiveFolder + placement
        };
        for (String path : candidates) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path), "windows-1252"))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
                if (!lines.isEmpty()) {
                    return lines.toArray(new String[0]);
                }
            } catch (IOException ignored) {
                // try next candidate folder
            }
        }
        return null;
    }

    /**
     * Perimeter per piece part number (D,6) for one placement, in the /rapport unit
     * (segment-length sum / 42). Returns an empty map if the file is missing/unreadable.
     */
    public Map<String, Double> perimetrePerPartNumber(String placement) {
        Map<String, Double> result = new HashMap<>();
        if (placement == null || placement.trim().isEmpty()) {
            return result;
        }
        String[] lines = readPlacementLines(placement.trim());
        if (lines == null || lines.length == 0) {
            return result;
        }
        String geom = lines[0];
        int n1 = geom.indexOf("*N1");
        int q = geom.indexOf("*Q");
        if (n1 < 0 || q < 0 || q <= n1) {
            return result;
        }

        // 1. perimeter per piece index, from the geometry line
        Map<Integer, Double> perByPiece = new HashMap<>();
        String pointsData = geom.substring(n1 + 1, q); // starts at "N1*..."
        for (String chunkRaw : pointsData.split("\\*N")) {
            String chunk = chunkRaw;
            if (!chunk.isEmpty() && (chunk.charAt(0) == 'N' || chunk.charAt(0) == 'n')) {
                chunk = chunk.substring(1); // first chunk "N1*..." -> "1*..."
            }
            int k = 0;
            while (k < chunk.length() && Character.isDigit(chunk.charAt(k))) {
                k++;
            }
            if (k == 0) {
                continue;
            }
            int pieceIndex;
            try {
                pieceIndex = Integer.parseInt(chunk.substring(0, k));
            } catch (NumberFormatException e) {
                continue;
            }
            double per = 0.0;
            for (String segment : chunk.split("\\*M15\\*")) {
                Integer lastX = null, lastY = null;
                for (String elem : segment.split("\\*")) {
                    if (!elem.startsWith("X") || !elem.contains("Y")) {
                        continue;
                    }
                    try {
                        String[] xy = elem.replace("X", "").split("Y");
                        int x = Integer.parseInt(xy[0]);
                        int y = Integer.parseInt(xy[1]);
                        if (lastX != null) {
                            per += Math.hypot(x - lastX, y - lastY);
                        }
                        lastX = x;
                        lastY = y;
                    } catch (Exception ignored) {
                        // skip malformed point
                    }
                }
            }
            perByPiece.merge(pieceIndex, per, Double::sum);
        }

        // 2. piece index -> D,6 part number, from the metadata lines
        Map<Integer, String> pnByPiece = new HashMap<>();
        Integer cur = null;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("L,")) {
                try {
                    cur = Integer.parseInt(line.split(",")[1].trim());
                } catch (Exception e) {
                    cur = null;
                }
            } else if (line.startsWith("D,6,") && cur != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    pnByPiece.put(cur, parts[2].trim());
                }
            }
        }

        // 3. group perimeter by part number
        for (Map.Entry<Integer, Double> e : perByPiece.entrySet()) {
            String pn = pnByPiece.get(e.getKey());
            if (pn == null || pn.isEmpty()) {
                continue;
            }
            result.merge(pn, e.getValue() / UNIT_DIVISOR, Double::sum);
        }
        return result;
    }

    /** Resolve a D,6 part number to one of the entity part numbers (direct, then correspondence). */
    private String resolveEntityPn(String d6, Set<String> entityPns, String placement) {
        if (entityPns.contains(d6)) {
            return d6;
        }
        for (String pn : entityPns) {
            PartNumberCorrespendance c = partNumberCorrespendanceService
                    .findByPartNumberAndPartNumberCorrespondanceAndPlacement(d6, pn, placement);
            if (c == null) {
                c = partNumberCorrespendanceService
                        .findByPartNumberAndPartNumberCorrespondanceAndPlacementNull(d6, pn);
            }
            if (c != null) {
                return pn;
            }
        }
        return null;
    }

    /** Sum perimeter per entity part number across a set of placements. */
    private Map<String, Double> aggregate(Collection<String> placements, Set<String> entityPns) {
        Map<String, Double> perByPn = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (String placementRaw : placements) {
            if (placementRaw == null) {
                continue;
            }
            String placement = placementRaw.trim();
            if (placement.isEmpty() || !seen.add(placement)) {
                continue;
            }
            Map<String, Double> perByD6 = perimetrePerPartNumber(placement);
            for (Map.Entry<String, Double> e : perByD6.entrySet()) {
                String target = resolveEntityPn(e.getKey(), entityPns, placement);
                if (target != null) {
                    perByPn.merge(target, e.getValue(), Double::sum);
                }
            }
        }
        return perByPn;
    }

    /**
     * Compute and set {@code perimetre} on the plan's part numbers (in-memory; the
     * caller persists). Reads the plan's placements from its materials. Never throws.
     */
    public void applyToPlan(CuttingPlan plan) {
        try {
            if (plan == null || plan.getCuttingPlanPartNumbers() == null
                    || plan.getCuttingPlanPartNumbers().isEmpty()) {
                return;
            }
            Set<String> pns = new HashSet<>();
            for (CuttingPlanPartNumber p : plan.getCuttingPlanPartNumbers()) {
                pns.add(p.getPartNumber());
            }
            List<String> placements = new ArrayList<>();
            if (plan.getCuttingPlanMaterials() != null) {
                for (CuttingPlanMaterial m : plan.getCuttingPlanMaterials()) {
                    if (m.getCuttingPlanMaterialPlacement() == null) {
                        continue;
                    }
                    for (CuttingPlanMaterialPlacement mp : m.getCuttingPlanMaterialPlacement()) {
                        placements.add(mp.getPlacement());
                    }
                }
            }
            Map<String, Double> per = aggregate(placements, pns);
            for (CuttingPlanPartNumber p : plan.getCuttingPlanPartNumbers()) {
                p.setPerimetre(per.getOrDefault(p.getPartNumber(), 0.0));
            }
        } catch (Exception ignored) {
            // perimeter is best-effort; never block a plan save
        }
    }

    /**
     * Compute and set {@code perimetre} on the request's part numbers when any is null
     * (lazy cache; the caller must be transactional so the managed entities flush).
     * Reads the placements from the request's series. Never throws.
     */
    public void ensureRequestPerimetre(CuttingRequest req) {
        try {
            if (req == null || req.getCuttingRequestPartNumbers() == null
                    || req.getCuttingRequestPartNumbers().isEmpty()) {
                return;
            }
            boolean anyNull = false;
            Set<String> pns = new HashSet<>();
            for (CuttingRequestPartNumber p : req.getCuttingRequestPartNumbers()) {
                pns.add(p.getPartNumber());
                if (p.getPerimetre() == null) {
                    anyNull = true;
                }
            }
            if (!anyNull) {
                return;
            }
            List<String> placements = new ArrayList<>();
            if (req.getCuttingRequestSeries() != null) {
                for (CuttingRequestSerie s : req.getCuttingRequestSeries()) {
                    placements.add(s.getPlacement());
                }
            }
            Map<String, Double> per = aggregate(placements, pns);
            if (per.isEmpty()) {
                // A transient cut-file read failure (missing/locked/unreadable file)
                // yields an empty map. Do NOT persist 0.0 in that case — a cached 0.0
                // is non-null and would poison the cache forever (anyNull stays false,
                // so it is never re-attempted). Leaving perimetre null lets the next
                // report access retry once the file is readable again.
                return;
            }
            for (CuttingRequestPartNumber p : req.getCuttingRequestPartNumbers()) {
                Double val = per.get(p.getPartNumber());
                // Only cache a genuinely resolved perimeter. Part numbers that did not
                // resolve this pass stay null and are retried on the next access.
                if (val != null && val > 0) {
                    p.setPerimetre(val);
                }
            }
        } catch (Exception ignored) {
            // best-effort cache; report still renders with whatever is available
        }
    }
}
