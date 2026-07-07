package com.lear.MGCMS.services.scheduling;

import com.lear.MGCMS.services.OrdonnancementService.SerieDTO;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Pluggable dispatch algorithm comparators for serie ordering.
 * Each algorithm produces a Comparator that sorts series according to a different strategy.
 *
 * Usage:
 *   List<SerieDTO> series = ...;
 *   series.sort(DispatchAlgorithms.get("SPT", context));
 */
public class DispatchAlgorithms {

    /** All supported algorithm keys */
    public static final List<String> AVAILABLE_ALGORITHMS = List.of(
            "SCG", "SPT", "LPT", "EDF", "CR", "WSPT", "ATC", "MATERIAL_GROUP"
    );

    /**
     * Returns a Comparator for the given algorithm key.
     *
     * @param algorithm one of AVAILABLE_ALGORITHMS
     * @param context   provides cutting time lookup + started sequences + weights
     * @return Comparator for sorting SerieDTO
     */
    public static Comparator<SerieDTO> get(String algorithm, DispatchContext context) {
        if (algorithm == null) algorithm = "SCG";
        switch (algorithm.toUpperCase()) {
            case "SPT":  return spt(context);
            case "LPT":  return lpt(context);
            case "EDF":  return edf(context);
            case "CR":   return cr(context);
            case "WSPT": return wspt(context);
            case "ATC":  return atc(context);
            case "MATERIAL_GROUP": return materialGroup(context);
            case "SCG":
            default:     return scg(context);
        }
    }

    // ─────────────────── ALGORITHM COMPARATORS ───────────────────

    /**
     * SCG — Sequence Compaction Greedy (current default).
     * Priority: started sequences first → due date → sequence name → serie name.
     */
    public static Comparator<SerieDTO> scg(DispatchContext ctx) {
        return Comparator
                .comparing((SerieDTO s) -> ctx.startedSequences.contains(s.sequence) ? 0 : 1)
                .thenComparing(s -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparing(s -> s.sequence != null ? s.sequence : "")
                .thenComparing(s -> s.serie != null ? s.serie : "");
    }

    /**
     * SPT — Shortest Processing Time.
     * Favors series with shortest total processing time (spreading + cutting).
     * Maximizes throughput (number of series completed per hour).
     */
    public static Comparator<SerieDTO> spt(DispatchContext ctx) {
        return Comparator
                .comparing((SerieDTO s) -> ctx.startedSequences.contains(s.sequence) ? 0 : 1)
                .thenComparingDouble(s -> totalProcessingTime(s, ctx));
    }

    /**
     * LPT — Longest Processing Time.
     * Favors series with longest total processing time first.
     * Good for load balancing across machines (prevents long jobs at the end).
     */
    public static Comparator<SerieDTO> lpt(DispatchContext ctx) {
        return Comparator
                .comparing((SerieDTO s) -> ctx.startedSequences.contains(s.sequence) ? 0 : 1)
                .thenComparing(Comparator.comparingDouble((SerieDTO s) -> totalProcessingTime(s, ctx)).reversed());
    }

    /**
     * EDF — Earliest Due Date First.
     * Favors series whose sequence has the earliest due date + shift.
     * Minimizes late deliveries.
     */
    public static Comparator<SerieDTO> edf(DispatchContext ctx) {
        return Comparator
                .comparing((SerieDTO s) -> ctx.startedSequences.contains(s.sequence) ? 0 : 1)
                .thenComparing(s -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparingDouble(s -> totalProcessingTime(s, ctx));
    }

    /**
     * CR — Critical Ratio.
     * CR = time remaining until due date / total processing time remaining.
     * Lower CR = more urgent. CR < 1 means the serie will be late if not started now.
     */
    public static Comparator<SerieDTO> cr(DispatchContext ctx) {
        return Comparator
                .comparing((SerieDTO s) -> ctx.startedSequences.contains(s.sequence) ? 0 : 1)
                .thenComparingDouble(s -> criticalRatio(s, ctx));
    }

    /**
     * WSPT — Weighted Shortest Processing Time.
     * score = weight / processing_time. Higher score = scheduled first.
     * Weight = priority multiplier from context (sequence priority).
     */
    public static Comparator<SerieDTO> wspt(DispatchContext ctx) {
        return Comparator
                .comparing((SerieDTO s) -> ctx.startedSequences.contains(s.sequence) ? 0 : 1)
                .thenComparing(Comparator.comparingDouble((SerieDTO s) -> wsptScore(s, ctx)).reversed());
    }

    /**
     * ATC — Apparent Tardiness Cost.
     * Combines due-date urgency with processing time efficiency.
     * score = (weight / pTime) * exp(-max(0, dueSlack) / (k * avgPTime))
     * Higher score = scheduled first.
     */
    public static Comparator<SerieDTO> atc(DispatchContext ctx) {
        double avgPTime = ctx.averageProcessingTime > 0 ? ctx.averageProcessingTime : 30.0;
        return Comparator
                .comparing((SerieDTO s) -> ctx.startedSequences.contains(s.sequence) ? 0 : 1)
                .thenComparing(Comparator.comparingDouble((SerieDTO s) -> atcScore(s, ctx, avgPTime)).reversed());
    }

    /**
     * MATERIAL_GROUP — Material Changeover Minimization.
     * Groups series by partNumberMaterial, then within each group sorts by sequence affinity + due date.
     * Minimizes material changeover count on each machine.
     */
    public static Comparator<SerieDTO> materialGroup(DispatchContext ctx) {
        return Comparator
                .comparing((SerieDTO s) -> ctx.startedSequences.contains(s.sequence) ? 0 : 1)
                .thenComparing(s -> s.partNumberMaterial != null ? s.partNumberMaterial : "")
                .thenComparing(s -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> s.dueShift != null ? s.dueShift : Integer.MAX_VALUE)
                .thenComparing(s -> s.sequence != null ? s.sequence : "");
    }

    // ─────────────────── SCORING HELPERS ───────────────────

    /**
     * Total processing time in minutes (spreading + cutting).
     */
    private static double totalProcessingTime(SerieDTO s, DispatchContext ctx) {
        double spreadTime = estimateSpreadingTime(s, ctx);
        double cuttingTime = ctx.getCuttingTime(s);
        return spreadTime + cuttingTime;
    }

    /**
     * Estimate spreading time: (longueur × nbrCouche × coef) + setup.
     */
    private static double estimateSpreadingTime(SerieDTO s, DispatchContext ctx) {
        double longueur = s.longueur != null ? s.longueur : 0;
        int nbrCouche = s.nbrCouche != null ? s.nbrCouche : 1;
        return (longueur * nbrCouche * ctx.coefSpreadingPerMetre) + ctx.coefSetupTime;
    }

    /**
     * Critical Ratio: timeRemaining / processingTimeRemaining.
     * Lower = more urgent. Values < 1.0 mean the job cannot finish on time.
     */
    private static double criticalRatio(SerieDTO s, DispatchContext ctx) {
        if (s.dueDate == null) return Double.MAX_VALUE; // no due date → lowest priority
        LocalDateTime now = ctx.now;
        LocalDateTime dueDateTime = s.dueDate.atTime(ctx.shiftEndHour, ctx.shiftEndMinute);
        double minutesRemaining = Duration.between(now, dueDateTime).toMinutes();
        double pTime = totalProcessingTime(s, ctx);
        if (pTime <= 0) return Double.MAX_VALUE;
        return minutesRemaining / pTime;
    }

    /**
     * WSPT score: weight / processingTime. Higher = do first.
     */
    private static double wsptScore(SerieDTO s, DispatchContext ctx) {
        double pTime = totalProcessingTime(s, ctx);
        if (pTime <= 0) return Double.MAX_VALUE;
        double weight = ctx.getSequenceWeight(s.sequence);
        return weight / pTime;
    }

    /**
     * ATC score: (weight / pTime) * exp(-max(0, slack) / (k * avgPTime)).
     * k is a lookahead parameter (default 2.0).
     */
    private static double atcScore(SerieDTO s, DispatchContext ctx, double avgPTime) {
        double pTime = totalProcessingTime(s, ctx);
        if (pTime <= 0) return Double.MAX_VALUE;
        double weight = ctx.getSequenceWeight(s.sequence);

        double slack = 0;
        if (s.dueDate != null) {
            LocalDateTime dueDateTime = s.dueDate.atTime(ctx.shiftEndHour, ctx.shiftEndMinute);
            slack = Duration.between(ctx.now, dueDateTime).toMinutes() - pTime;
        }

        double k = 2.0; // lookahead parameter
        double exponent = -Math.max(0, slack) / (k * avgPTime);
        return (weight / pTime) * Math.exp(exponent);
    }

    // ─────────────────── CONTEXT ───────────────────

    /**
     * Context object passed to algorithms — provides data they need for scoring.
     */
    public static class DispatchContext {
        /** Sequences that already have in-progress series */
        public Set<String> startedSequences = new HashSet<>();

        /** Cutting time lookup: placement → minutes */
        public Map<String, Double> cuttingTimeMap = new HashMap<>();

        /** Sequence priority weights (higher = more important). Default 1.0. */
        public Map<String, Double> sequenceWeights = new HashMap<>();

        /** Spreading coefficient: minutes per metre per layer */
        public double coefSpreadingPerMetre = 0.5;

        /** Setup time in minutes (material changeover) */
        public double coefSetupTime = 2.0;

        /** Average processing time across all pending series (for ATC normalization) */
        public double averageProcessingTime = 30.0;

        /** Current time for urgency calculations */
        public LocalDateTime now = LocalDateTime.now();

        /** Shift end time (hour, minute) for due-date calculations */
        public int shiftEndHour = 21;
        public int shiftEndMinute = 45;

        /**
         * Get cutting time for a serie from cuttingTimeMap (lookup by placement).
         * Falls back to estimated time from dimensions if placement not found.
         */
        public double getCuttingTime(SerieDTO s) {
            if (s.placement != null && cuttingTimeMap.containsKey(s.placement)) {
                return cuttingTimeMap.get(s.placement);
            }
            // Fallback: rough estimate from tempsDeCoupe if available
            if (s.tempsDeCoupe != null && s.tempsDeCoupe > 0) {
                return s.tempsDeCoupe;
            }
            // Last resort: estimate from dimensions
            double longueur = s.longueur != null ? s.longueur : 5.0;
            return longueur * 2.0; // ~2 min per metre as rough estimate
        }

        /**
         * Get priority weight for a sequence. Default 1.0.
         */
        public double getSequenceWeight(String sequence) {
            if (sequence == null) return 1.0;
            return sequenceWeights.getOrDefault(sequence, 1.0);
        }
    }

}
