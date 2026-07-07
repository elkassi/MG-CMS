package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator;

/**
 * V2 ordering strategy that targets the box-duration KPI.
 *
 * <p>Core insight: for a sequence (box) the bottleneck is the serie with
 * the longest cutting time, because {@code max dateFinCoupe} determines
 * the numerator of {@code boxDur}.  By putting the longest-cutting series
 * first on every machine we pull the bottleneck leftwards, shortening the
 * overall box span.  Matelassage→coupe precedence is respected because
 * ready-to-cut and waiting series are still handled in separate phases.</p>
 *
 * <p>LASER-DXF is treated differently: spreading and cutting happen in
 * parallel on the same machine, so the effective processing time is
 * {@code max(spread, cut)} rather than {@code spread + cut}.  This means
 * a LASER-DXF serie with a very long cut (many layers) blocks the machine
 * for longer — reinforcing the longest-cut-first heuristic.</p>
 */
@Component
public class BoxDurOptimizedOrderingStrategy implements SeriesOrderingStrategy {

    private static final double COEF_SPREADING_PER_METRE = 0.5;
    private static final double COEF_SETUP_TIME = 2.0;

    @Autowired
    private CuttingTimeCalculator cuttingTimeCalculator;

    @Override
    public void sortReadyToCut(List<OrdonnancementService.SerieDTO> series, Context ctx) {
        Map<OrdonnancementService.SerieDTO, Double> cutMinutes = resolveCuttingTimes(series, ctx);
        series.sort(Comparator
                .comparingDouble((OrdonnancementService.SerieDTO s) ->
                        -cutMinutes.getOrDefault(s, 0.0))
                .thenComparing(s -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparingDouble(s -> -(s.completionRatio != null ? s.completionRatio : 0.0))
                .thenComparing(s -> s.dateFinMatelassage != null
                        ? s.dateFinMatelassage : LocalDateTime.MAX)
                .thenComparing(s -> s.sequence != null ? s.sequence : "")
                .thenComparing(s -> s.serie != null ? s.serie : ""));
    }

    @Override
    public void sortWaiting(List<OrdonnancementService.SerieDTO> series, Context ctx) {
        Map<OrdonnancementService.SerieDTO, Double> cutMinutes = resolveCuttingTimes(series, ctx);
        Map<OrdonnancementService.SerieDTO, Double> totalMinutes = new HashMap<>();
        for (OrdonnancementService.SerieDTO s : series) {
            double spread = estimateSpreadingTime(s);
            double cut = cutMinutes.getOrDefault(s, 0.0);
            boolean isLaser = ctx.laserDxfMachines != null
                    && s.tableCoupe != null
                    && ctx.laserDxfMachines.contains(s.tableCoupe);
            double total = isLaser ? Math.max(spread, cut) : spread + cut;
            totalMinutes.put(s, total);
        }
        series.sort(Comparator
                .comparingDouble((OrdonnancementService.SerieDTO s) ->
                        -totalMinutes.getOrDefault(s, 0.0))
                .thenComparing(s -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparingDouble(s -> -(s.completionRatio != null ? s.completionRatio : 0.0))
                .thenComparing(s -> s.planningDate != null ? s.planningDate : LocalDate.MAX)
                .thenComparing(s -> s.sequence != null ? s.sequence : "")
                .thenComparing(s -> s.serie != null ? s.serie : ""));
    }

    /**
     * Resolve cutting minutes for every serie in the list, using the same
     * priority waterfall as {@code OrdonnancementService}:
     * TimingModel (validated) &gt; tempsDeCoupe &gt; dimension fallback.
     */
    private Map<OrdonnancementService.SerieDTO, Double> resolveCuttingTimes(
            List<OrdonnancementService.SerieDTO> series, Context ctx) {
        Map<OrdonnancementService.SerieDTO, Double> out = new HashMap<>();
        for (OrdonnancementService.SerieDTO s : series) {
            out.put(s, resolveOne(s, ctx));
        }
        return out;
    }

    private double resolveOne(OrdonnancementService.SerieDTO s, Context ctx) {
        Map<String, CuttingTimeCalculator.TimingRow> tim;
        if (s.placement != null && ctx.cuttingTimeMap != null) {
            Double v = ctx.cuttingTimeMap.get(s.placement);
            if (v != null) {
                tim = Collections.singletonMap(
                        s.placement, new CuttingTimeCalculator.TimingRow(v, null));
            } else {
                tim = Collections.emptyMap();
            }
        } else {
            tim = Collections.emptyMap();
        }

        String machineType = "Lectra";
        if (s.tableCoupe != null) {
            if (ctx.laserDxfMachines != null && ctx.laserDxfMachines.contains(s.tableCoupe)) {
                machineType = "LASER-DXF";
            } else if (ctx.gerberMachines != null && ctx.gerberMachines.contains(s.tableCoupe)) {
                machineType = "Gerber";
            }
        }

        double resolved = cuttingTimeCalculator.resolveMinutes(
                s.placement, s.tempsDeCoupe, s.nbrCouche, machineType, tim);
        return resolved > 0 ? resolved : 1.0;
    }

    private double estimateSpreadingTime(OrdonnancementService.SerieDTO s) {
        double longueur = s.longueur != null ? s.longueur : 0;
        int nbrCouche = s.nbrCouche != null ? s.nbrCouche : 1;
        return (longueur * nbrCouche * COEF_SPREADING_PER_METRE) + COEF_SETUP_TIME;
    }
}
