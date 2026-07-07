package com.lear.MGCMS.services.dispatcher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Computes the box-duration KPI from a {@link ScheduleSnapshot}.
 *
 * <p>Box-duration per sequence:
 * <pre>
 *   boxDuration(seq) = (max(plannedEnd_COUPE)
 *                      − min(plannedStart_MATELASSAGE)) / nbBoxes(seq)
 * </pre>
 * Both the numerator and the divisor are derived from data the optimizer
 * already has. The numerator is the schedule's "wall time" for the
 * sequence; the divisor is the box count from
 * {@code CuttingRequestBoxInfoRepository}. A sequence with zero or
 * unknown box count is reported as zero (excluded from aggregates).</p>
 *
 * <p>This service is stateless — call sites are responsible for passing
 * the snapshot + box counts they want measured. The engine caches the box
 * count map across iterations because it changes rarely.</p>
 */
@Service
public class BoxDurationCalculator {

    /**
     * One sequence's box-duration plus the raw span and box count that
     * produced it. Useful for surfacing in the UI / state snapshot.
     */
    public static final class Result {
        public final String sequenceId;
        public final long spanMinutes;
        public final int nbBoxes;
        public final double minutesPerBox;

        public Result(String sequenceId, long spanMinutes, int nbBoxes, double minutesPerBox) {
            this.sequenceId = sequenceId;
            this.spanMinutes = spanMinutes;
            this.nbBoxes = nbBoxes;
            this.minutesPerBox = minutesPerBox;
        }
    }

    /**
     * Aggregate box-duration metrics across all sequences in a snapshot.
     */
    public static final class Aggregate {
        public final int sequencesMeasured;
        public final double meanMinutesPerBox;
        public final double maxMinutesPerBox;
        public final String worstSequenceId;
        public final Map<String, Result> bySequence;

        public Aggregate(int sequencesMeasured, double meanMinutesPerBox, double maxMinutesPerBox,
                         String worstSequenceId, Map<String, Result> bySequence) {
            this.sequencesMeasured = sequencesMeasured;
            this.meanMinutesPerBox = meanMinutesPerBox;
            this.maxMinutesPerBox = maxMinutesPerBox;
            this.worstSequenceId = worstSequenceId;
            this.bySequence = bySequence;
        }

        public static Aggregate empty() {
            return new Aggregate(0, 0.0, 0.0, null, new HashMap<>());
        }
    }

    /**
     * Compute box-duration per sequence and the aggregate.
     *
     * @param snapshot     in-memory schedule
     * @param boxCountsBySequence    sequenceId → number of boxes
     * @return aggregate metrics; empty when snapshot has no slots
     */
    public Aggregate compute(ScheduleSnapshot snapshot, Map<String, Integer> boxCountsBySequence) {
        if (snapshot == null || snapshot.size() == 0) return Aggregate.empty();

        // Group slots by sequence in one pass — cheaper than calling
        // snapshot.slotsForSequence per sequence.
        Map<String, LocalDateTime> minMatelassageStart = new HashMap<>();
        Map<String, LocalDateTime> maxCoupeEnd = new HashMap<>();
        for (ScheduleSnapshot.PlannedSlot s : snapshot.copyOfSlots().values()) {
            String seq = s.getSequenceId();
            if (seq == null) continue;
            if (s.getPhase() == ScheduleSnapshot.Phase.MATELASSAGE && s.getPlannedStart() != null) {
                LocalDateTime cur = minMatelassageStart.get(seq);
                if (cur == null || s.getPlannedStart().isBefore(cur)) {
                    minMatelassageStart.put(seq, s.getPlannedStart());
                }
            }
            if (s.getPhase() == ScheduleSnapshot.Phase.COUPE && s.getPlannedEnd() != null) {
                LocalDateTime cur = maxCoupeEnd.get(seq);
                if (cur == null || s.getPlannedEnd().isAfter(cur)) {
                    maxCoupeEnd.put(seq, s.getPlannedEnd());
                }
            }
        }

        Map<String, Result> results = new HashMap<>();
        double totalMinutesPerBox = 0.0;
        double maxMpb = 0.0;
        String worst = null;
        int measured = 0;

        for (String seq : maxCoupeEnd.keySet()) {
            LocalDateTime end = maxCoupeEnd.get(seq);
            LocalDateTime start = minMatelassageStart.get(seq);
            // When matelassage is missing (only coupe in the schedule) fall back
            // to the coupe phase's earliest start. This keeps frozen sequences
            // measurable.
            if (start == null) {
                for (ScheduleSnapshot.PlannedSlot s : snapshot.slotsForSequence(seq)) {
                    if (s.getPhase() == ScheduleSnapshot.Phase.COUPE && s.getPlannedStart() != null) {
                        if (start == null || s.getPlannedStart().isBefore(start)) {
                            start = s.getPlannedStart();
                        }
                    }
                }
            }
            if (start == null) continue;

            int nbBoxes = boxCountsBySequence != null ? boxCountsBySequence.getOrDefault(seq, 0) : 0;
            if (nbBoxes <= 0) continue;

            long span = Math.max(0, Duration.between(start, end).toMinutes());
            double mpb = (double) span / nbBoxes;
            results.put(seq, new Result(seq, span, nbBoxes, mpb));
            totalMinutesPerBox += mpb;
            measured++;
            if (mpb > maxMpb) {
                maxMpb = mpb;
                worst = seq;
            }
        }

        double mean = measured > 0 ? totalMinutesPerBox / measured : 0.0;
        return new Aggregate(measured, mean, maxMpb, worst, results);
    }
}
