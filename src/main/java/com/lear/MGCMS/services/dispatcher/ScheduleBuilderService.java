package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Constructive heuristic — produces an initial {@link ScheduleSnapshot}
 * from the engine's per-snapshot serie list, zone-machine map, and
 * sequence-to-zone assignment.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Group series by their sequence's dispatched zone.</li>
 *   <li>Within each zone, group machines by machine type.</li>
 *   <li>Sort series by {@code dueDate}/{@code dueShift} ascending, then by
 *       sequence ID (sequence-affinity tiebreak — keeps a sequence's series
 *       together when due buckets match).</li>
 *   <li>For each serie, assign it to the least-loaded machine of its type
 *       in the zone. Compute its planned matelassage start as the chosen
 *       machine's matelassage-pointer, planned coupe start as
 *       {@code max(plannedMatelassageEnd, machine's coupe-pointer)}.
 *       Advance both pointers.</li>
 * </ol>
 *
 * <p>The output is a deterministic, feasible schedule with no machine
 * conflicts and matelassage→coupe precedence respected. The engine's SA
 * loop then refines it.</p>
 *
 * <p>This is the Level-2 + Level-3 starting point. Levels 2/3 SA moves
 * (Slices 3/4) operate on the snapshot produced here.</p>
 */
@Service
public class ScheduleBuilderService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleBuilderService.class);
    private static final double COEF_SPREADING_PER_METRE = 0.5;
    private static final double COEF_SETUP_TIME = 2.0;
    private static final double SMALL_LENGTH_METERS = 1.0;

    private enum LifecycleState {
        CUTTING,
        READY_TO_CUT,
        SPREADING,
        WAITING,
        OTHER
    }

    @Autowired
    private com.lear.MGCMS.services.scheduling.CuttingTimeCalculator cuttingTimeCalculator;

    /**
     * Serie-level input given to the builder. The optimizer's snapshot
     * passes one of these per non-Complete serie.
     */
    public static final class SerieInput {
        public final String serieId;
        public final String sequenceId;
        public final String machineType;
        public final Double cuttingMinutes;
        public final Double longueur;
        public final Integer nbrCouche;
        public final String partNumberMaterial;
        public final String statusCoupe;
        public final String statusMatelassage;
        public final LocalDate dueDate;
        public final String dueShift;
        /** Existing assignment (matelassage table) if any — preferred when feasible. */
        public final String existingMatelassageMachine;
        /** Existing assignment (coupe table) if any — preferred when feasible. */
        public final String existingCoupeMachine;
        /** True when the serie is already physically committed on the floor. */
        public final boolean frozen;
        /** Frozen-only: real start time for matelassage. Else null. */
        public final LocalDateTime actualMatelassageStart;
        public final LocalDateTime actualMatelassageEnd;
        public final LocalDateTime actualCoupeStart;
        public final LocalDateTime actualCoupeEnd;

        public SerieInput(String serieId, String sequenceId, String machineType,
                          Double cuttingMinutes, Double longueur, Integer nbrCouche,
                          String partNumberMaterial, LocalDate dueDate, String dueShift,
                          String existingMatelassageMachine, String existingCoupeMachine,
                          boolean frozen,
                          LocalDateTime actualMatelassageStart, LocalDateTime actualMatelassageEnd,
                          LocalDateTime actualCoupeStart, LocalDateTime actualCoupeEnd) {
            this(serieId, sequenceId, machineType, cuttingMinutes, longueur, nbrCouche,
                    partNumberMaterial, null, null, dueDate, dueShift,
                    existingMatelassageMachine, existingCoupeMachine, frozen,
                    actualMatelassageStart, actualMatelassageEnd,
                    actualCoupeStart, actualCoupeEnd);
        }

        public SerieInput(String serieId, String sequenceId, String machineType,
                          Double cuttingMinutes, Double longueur, Integer nbrCouche,
                          String partNumberMaterial, String statusCoupe, String statusMatelassage,
                          LocalDate dueDate, String dueShift,
                          String existingMatelassageMachine, String existingCoupeMachine,
                          boolean frozen,
                          LocalDateTime actualMatelassageStart, LocalDateTime actualMatelassageEnd,
                          LocalDateTime actualCoupeStart, LocalDateTime actualCoupeEnd) {
            this.serieId = serieId;
            this.sequenceId = sequenceId;
            this.machineType = machineType;
            this.cuttingMinutes = cuttingMinutes;
            this.longueur = longueur;
            this.nbrCouche = nbrCouche;
            this.partNumberMaterial = partNumberMaterial;
            this.statusCoupe = statusCoupe;
            this.statusMatelassage = statusMatelassage;
            this.dueDate = dueDate;
            this.dueShift = dueShift;
            this.existingMatelassageMachine = existingMatelassageMachine;
            this.existingCoupeMachine = existingCoupeMachine;
            this.frozen = frozen;
            this.actualMatelassageStart = actualMatelassageStart;
            this.actualMatelassageEnd = actualMatelassageEnd;
            this.actualCoupeStart = actualCoupeStart;
            this.actualCoupeEnd = actualCoupeEnd;
        }

        public boolean isMovableWaiting() {
            return ScheduleBuilderService.isMovableWaiting(this);
        }
    }

    /**
     * Build the snapshot.
     *
     * @param horizonStart        the "now" reference — planned starts begin at or after this.
     * @param series              all schedulable series (from the engine snapshot)
     * @param sequenceToZone      sequence → assigned zone (from the engine's bestAssignment)
     * @param machinesByZoneByType zone → machine type → set of machine names
     */
    public ScheduleSnapshot build(LocalDateTime horizonStart,
                                  List<SerieInput> series,
                                  Map<String, String> sequenceToZone,
                                  Map<String, Map<String, Set<String>>> machinesByZoneByType) {
        long startMs = System.currentTimeMillis();
        ScheduleSnapshot snap = ScheduleSnapshot.empty();
        if (series == null || series.isEmpty()) {
            return snap;
        }

        // Per-machine pointer of "next available" timestamp, per phase.
        // matelassageHead[machine] and coupeHead[machine]. We use the input
        // horizonStart as the floor for every pointer.
        Map<String, LocalDateTime> matelassageHead = new HashMap<>();
        Map<String, LocalDateTime> coupeHead = new HashMap<>();

        // Pass 1 — anchor committed series first. Their slots are read straight
        // from the actual timestamps; their machine pointers advance to the
        // actual end of work.
        for (SerieInput s : series) {
            String matM = s.existingMatelassageMachine;
            String cutM = s.existingCoupeMachine;
            if (matM != null && s.actualMatelassageStart != null) {
                LocalDateTime end = s.actualMatelassageEnd != null
                        ? s.actualMatelassageEnd
                        : s.actualMatelassageStart.plusMinutes((long) estimateSpreadingMinutes(s));
                snap.put(new ScheduleSnapshot.PlannedSlot(
                        s.serieId, s.sequenceId, ScheduleSnapshot.Phase.MATELASSAGE,
                        matM, sequenceToZone.get(s.sequenceId),
                        s.actualMatelassageStart, end));
                advanceHead(matelassageHead, matM, end);
            }
            if (cutM != null && s.actualCoupeStart != null) {
                LocalDateTime end = s.actualCoupeEnd != null
                        ? s.actualCoupeEnd
                        : s.actualCoupeStart.plusMinutes((long) Math.max(1, resolveCuttingMinutes(s)));
                snap.put(new ScheduleSnapshot.PlannedSlot(
                        s.serieId, s.sequenceId, ScheduleSnapshot.Phase.COUPE,
                        cutM, sequenceToZone.get(s.sequenceId),
                        s.actualCoupeStart, end));
                advanceHead(coupeHead, cutM, end);
            }
        }

        // Pass 2 — every serie that still needs a coupe slot. Only pure WAITING
        // rows are movable; READY_TO_CUT and SPREADING remain on their current
        // matelassage table and are only placed into the committed queue.
        Map<String, Boolean> openedSequence = new HashMap<>();
        Map<String, LocalDateTime> firstOpenAt = new HashMap<>();
        Map<String, Integer> pendingCountBySequence = new HashMap<>();
        for (SerieInput s : series) {
            boolean opened = lifecycle(s) != LifecycleState.WAITING || hasAnyActualTimestamp(s);
            if (opened && s.sequenceId != null) {
                openedSequence.put(s.sequenceId, true);
                LocalDateTime first = minNonNull(s.actualMatelassageStart, s.actualCoupeStart);
                if (first != null) {
                    LocalDateTime current = firstOpenAt.get(s.sequenceId);
                    if (current == null || first.isBefore(current)) {
                        firstOpenAt.put(s.sequenceId, first);
                    }
                }
            }
            if (needsCoupePlanning(s) && s.sequenceId != null) {
                pendingCountBySequence.merge(s.sequenceId, 1, Integer::sum);
            }
        }

        // Physical queue order:
        // CUTTING is anchored in pass 1, then READY_TO_CUT, then SPREADING,
        // then movable WAITING. WAITING rows are ordered for future work.
        List<SerieInput> pending = new ArrayList<>();
        for (SerieInput s : series) {
            if (needsCoupePlanning(s)) pending.add(s);
        }
        pending.sort(Comparator
                .comparingInt((SerieInput s) -> lifecycleOrder(lifecycle(s)))
                .thenComparing((SerieInput s) -> !openedSequence.getOrDefault(s.sequenceId, false))
                .thenComparing(s -> firstOpenAt.getOrDefault(s.sequenceId, LocalDateTime.MAX))
                .thenComparing((SerieInput s) -> s.dueDate != null ? s.dueDate : LocalDate.MAX)
                .thenComparingInt(s -> shiftOrder(s.dueShift))
                .thenComparingInt(ScheduleBuilderService::smallLengthRank)
                .thenComparing(ScheduleBuilderService::materialKey)
                .thenComparingDouble(ScheduleBuilderService::lengthForSort)
                .thenComparingInt(s -> pendingCountBySequence.getOrDefault(s.sequenceId, Integer.MAX_VALUE))
                .thenComparing(s -> s.sequenceId != null ? s.sequenceId : "")
                .thenComparing(s -> s.serieId != null ? s.serieId : ""));

        for (SerieInput s : pending) {
            String zone = sequenceToZone.get(s.sequenceId);
            if (zone == null) continue;
            Map<String, Set<String>> mbt = machinesByZoneByType.get(zone);
            if (mbt == null) continue;
            Set<String> machinesOfType = mbt.get(s.machineType);
            if (machinesOfType == null || machinesOfType.isEmpty()) continue;

            LifecycleState lifecycle = lifecycle(s);
            boolean movableWaiting = lifecycle == LifecycleState.WAITING;
            String chosenMachine = chooseMachine(machinesOfType,
                    s.existingMatelassageMachine, s.existingCoupeMachine,
                    matelassageHead, horizonStart, !movableWaiting);
            if (chosenMachine == null) continue;

            double spreadingMin = estimateSpreadingMinutes(s);
            double cuttingMin = resolveCuttingMinutes(s);

            LocalDateTime matStart = resolveMatStart(s, lifecycle, chosenMachine, matelassageHead, horizonStart);
            LocalDateTime matEnd = resolveMatEnd(s, lifecycle, matStart, spreadingMin, horizonStart);

            // Coupe head respects (a) machine's prior coupe slots, (b) the
            // matelassage end for this serie. Coupe machine often equals the
            // matelassage machine (Lectra), but can differ — fall back to the
            // existing assignment when valid.
            String coupeMachine = chosenMachine; // default to same machine
            if (!movableWaiting && s.existingCoupeMachine != null && machinesOfType.contains(s.existingCoupeMachine)) {
                coupeMachine = s.existingCoupeMachine;
            }
            LocalDateTime coupeStart = max(headOr(coupeHead, coupeMachine, horizonStart), matEnd);
            LocalDateTime coupeEnd = coupeStart.plusMinutes((long) Math.max(1, cuttingMin));

            if (shouldPlanMatelassageSlot(s, lifecycle)) {
                snap.put(new ScheduleSnapshot.PlannedSlot(
                        s.serieId, s.sequenceId, ScheduleSnapshot.Phase.MATELASSAGE,
                        chosenMachine, zone, matStart, matEnd));
                advanceHead(matelassageHead, chosenMachine, matEnd);
            }
            snap.put(new ScheduleSnapshot.PlannedSlot(
                    s.serieId, s.sequenceId, ScheduleSnapshot.Phase.COUPE,
                    coupeMachine, zone, coupeStart, coupeEnd));

            advanceHead(coupeHead, coupeMachine, coupeEnd);
        }

        if (log.isDebugEnabled()) {
            log.debug("ScheduleBuilderService.build() — {} series, {} slots, took {} ms",
                    series.size(), snap.size(), System.currentTimeMillis() - startMs);
        }
        return snap;
    }

    private static String chooseMachine(Set<String> machinesOfType,
                                        String existingMatelassage, String existingCoupe,
                                        Map<String, LocalDateTime> matelassageHead,
                                        LocalDateTime fallback,
                                        boolean honorExisting) {
        if (honorExisting && existingMatelassage != null && machinesOfType.contains(existingMatelassage)) {
            return existingMatelassage;
        }
        if (honorExisting && existingCoupe != null && machinesOfType.contains(existingCoupe)) {
            return existingCoupe;
        }
        String chosen = null;
        LocalDateTime min = LocalDateTime.MAX;
        for (String m : machinesOfType) {
            LocalDateTime h = matelassageHead.getOrDefault(m, fallback);
            if (h.isBefore(min)) {
                min = h;
                chosen = m;
            }
        }
        // Fallback: deterministic alpha pick if all heads equal fallback.
        if (chosen == null) {
            chosen = machinesOfType.stream().sorted().findFirst().orElse(null);
        }
        return chosen;
    }

    private static LocalDateTime headOr(Map<String, LocalDateTime> heads, String machine,
                                        LocalDateTime fallback) {
        LocalDateTime h = heads.get(machine);
        return h != null ? h : fallback;
    }

    private static void advanceHead(Map<String, LocalDateTime> heads, String machine,
                                    LocalDateTime newHead) {
        LocalDateTime cur = heads.get(machine);
        if (cur == null || newHead.isAfter(cur)) {
            heads.put(machine, newHead);
        }
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static LocalDateTime minNonNull(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private static boolean needsCoupePlanning(SerieInput s) {
        if (s == null || s.actualCoupeStart != null) return false;
        LifecycleState state = lifecycle(s);
        return state == LifecycleState.READY_TO_CUT
                || state == LifecycleState.SPREADING
                || state == LifecycleState.WAITING
                || state == LifecycleState.OTHER;
    }

    private static boolean shouldPlanMatelassageSlot(SerieInput s, LifecycleState state) {
        if (s.actualMatelassageStart != null) return false;
        return state == LifecycleState.WAITING || state == LifecycleState.SPREADING || state == LifecycleState.OTHER;
    }

    private static LocalDateTime resolveMatStart(SerieInput s,
                                                 LifecycleState state,
                                                 String chosenMachine,
                                                 Map<String, LocalDateTime> matelassageHead,
                                                 LocalDateTime horizonStart) {
        if (s.actualMatelassageStart != null) return s.actualMatelassageStart;
        if (state == LifecycleState.READY_TO_CUT) return horizonStart;
        return max(headOr(matelassageHead, chosenMachine, horizonStart), horizonStart);
    }

    private static LocalDateTime resolveMatEnd(SerieInput s,
                                               LifecycleState state,
                                               LocalDateTime matStart,
                                               double spreadingMin,
                                               LocalDateTime horizonStart) {
        if (s.actualMatelassageEnd != null) return s.actualMatelassageEnd;
        if (state == LifecycleState.READY_TO_CUT) return horizonStart;
        return matStart.plusMinutes((long) Math.max(1, spreadingMin));
    }

    private static boolean hasAnyActualTimestamp(SerieInput s) {
        return s.actualMatelassageStart != null
                || s.actualMatelassageEnd != null
                || s.actualCoupeStart != null
                || s.actualCoupeEnd != null;
    }

    private static LifecycleState lifecycle(SerieInput s) {
        if (s == null) return LifecycleState.OTHER;
        if (isStatus(s.statusCoupe, "In progress") || s.actualCoupeStart != null) {
            return LifecycleState.CUTTING;
        }
        if (isStatus(s.statusMatelassage, "Complete") || s.actualMatelassageEnd != null) {
            return LifecycleState.READY_TO_CUT;
        }
        if (isStatus(s.statusMatelassage, "In progress") || s.actualMatelassageStart != null) {
            return LifecycleState.SPREADING;
        }
        if (isMovableWaiting(s)) {
            return LifecycleState.WAITING;
        }
        return LifecycleState.OTHER;
    }

    private static boolean isMovableWaiting(SerieInput s) {
        return s != null
                && isWaiting(s.statusCoupe)
                && isWaiting(s.statusMatelassage)
                && s.actualMatelassageStart == null
                && s.actualMatelassageEnd == null
                && s.actualCoupeStart == null
                && s.actualCoupeEnd == null;
    }

    private static boolean isWaiting(String status) {
        return status == null || status.trim().isEmpty() || "Waiting".equalsIgnoreCase(status.trim());
    }

    private static boolean isStatus(String status, String expected) {
        return status != null && expected.equalsIgnoreCase(status.trim());
    }

    private static int lifecycleOrder(LifecycleState state) {
        if (state == LifecycleState.READY_TO_CUT) return 0;
        if (state == LifecycleState.SPREADING) return 1;
        if (state == LifecycleState.WAITING) return 2;
        return 3;
    }

    private static int smallLengthRank(SerieInput s) {
        return lengthForSort(s) <= SMALL_LENGTH_METERS ? 0 : 1;
    }

    private static String materialKey(SerieInput s) {
        return s.partNumberMaterial != null ? s.partNumberMaterial.trim() : "";
    }

    private static double lengthForSort(SerieInput s) {
        return s.longueur != null && s.longueur > 0 ? s.longueur : Double.MAX_VALUE;
    }

    private static double estimateSpreadingMinutes(SerieInput s) {
        double longueur = s.longueur != null ? s.longueur : 0;
        int couches = s.nbrCouche != null ? s.nbrCouche : 1;
        return longueur * couches * COEF_SPREADING_PER_METRE + COEF_SETUP_TIME;
    }

    private static double resolveCuttingMinutes(SerieInput s) {
        if (s.cuttingMinutes != null && s.cuttingMinutes > 0) return s.cuttingMinutes;
        // Fallback when the precomputed value isn't supplied.
        return Math.max(1.0, estimateSpreadingMinutes(s));
    }

    private static int shiftOrder(String dueShift) {
        if (dueShift == null || dueShift.trim().isEmpty()) return 99;
        try {
            int parsed = Integer.parseInt(dueShift.trim());
            return parsed >= 1 && parsed <= 3 ? parsed : 99;
        } catch (NumberFormatException ignored) {
            return 99;
        }
    }
}
