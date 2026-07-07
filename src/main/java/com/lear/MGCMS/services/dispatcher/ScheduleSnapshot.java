package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory schedule snapshot — keyed by {@code (serieId, phase)}.
 *
 * <p>One snapshot is held by the engine at a time. It is rebuilt from
 * scratch by {@link ScheduleBuilderService} on every engine snapshot
 * rebuild and incrementally mutated by move operators (Slices 3-4).
 * Defensive copies are made by accessor methods so the public
 * {@code /api/dispatcher/engine/schedule} endpoint can iterate safely
 * without holding the engine's lock.</p>
 *
 * <p>Phases: every serie has a matelassage phase and a coupe phase.
 * Precedence is matelassage → coupe, so a serie's planned coupe start
 * must be ≥ its planned matelassage end (the builder enforces this).</p>
 */
public final class ScheduleSnapshot {

    public enum Phase {
        MATELASSAGE,
        COUPE
    }

    /**
     * One planned slot in the schedule. Immutable from the engine's
     * perspective once a snapshot rebuild starts — move operators
     * produce new {@link PlannedSlot} instances rather than mutating.
     */
    public static final class PlannedSlot {
        private final String serieId;
        private final String sequenceId;
        private final Phase phase;
        private final String machineNom;
        private final String zoneNom;
        private final LocalDateTime plannedStart;
        private final LocalDateTime plannedEnd;

        public PlannedSlot(String serieId, String sequenceId, Phase phase,
                           String machineNom, String zoneNom,
                           LocalDateTime plannedStart, LocalDateTime plannedEnd) {
            this.serieId = serieId;
            this.sequenceId = sequenceId;
            this.phase = phase;
            this.machineNom = machineNom;
            this.zoneNom = zoneNom;
            this.plannedStart = plannedStart;
            this.plannedEnd = plannedEnd;
        }

        public String getSerieId()             { return serieId; }
        public String getSequenceId()          { return sequenceId; }
        public Phase getPhase()                { return phase; }
        public String getMachineNom()          { return machineNom; }
        public String getZoneNom()             { return zoneNom; }
        public LocalDateTime getPlannedStart() { return plannedStart; }
        public LocalDateTime getPlannedEnd()   { return plannedEnd; }

        /** Convenience: derived duration in minutes (0 when either timestamp is null). */
        public long getPlannedMinutes() {
            if (plannedStart == null || plannedEnd == null) return 0;
            return java.time.Duration.between(plannedStart, plannedEnd).toMinutes();
        }
    }

    /** Compound key for one entry in the schedule. */
    public static final class Key {
        public final String serieId;
        public final Phase phase;

        public Key(String serieId, Phase phase) {
            this.serieId = serieId;
            this.phase = phase;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return java.util.Objects.equals(serieId, k.serieId) && phase == k.phase;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(serieId, phase);
        }
    }

    private final Map<Key, PlannedSlot> slots = new LinkedHashMap<>();

    /** Replace any existing slot for {@code (serie, phase)}. */
    public void put(PlannedSlot slot) {
        slots.put(new Key(slot.serieId, slot.phase), slot);
    }

    public PlannedSlot get(String serieId, Phase phase) {
        return slots.get(new Key(serieId, phase));
    }

    public int size() {
        return slots.size();
    }

    /** Defensive copy of the underlying map — safe to iterate outside the engine lock. */
    public Map<Key, PlannedSlot> copyOfSlots() {
        return new LinkedHashMap<>(slots);
    }

    /** All planned slots for a given sequence, both phases, undefined order. */
    public List<PlannedSlot> slotsForSequence(String sequenceId) {
        return slots.values().stream()
                .filter(s -> java.util.Objects.equals(s.sequenceId, sequenceId))
                .collect(Collectors.toList());
    }

    /** Ordered list (planned-start asc) for one machine, both phases mixed. */
    public List<PlannedSlot> slotsForMachine(String machineNom) {
        return slots.values().stream()
                .filter(s -> java.util.Objects.equals(s.machineNom, machineNom))
                .sorted(Comparator.comparing(PlannedSlot::getPlannedStart,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /** Defensive copy grouped by machine — used by debug endpoint and the public API. */
    public Map<String, List<PlannedSlot>> groupByMachine() {
        Map<String, List<PlannedSlot>> out = new LinkedHashMap<>();
        for (PlannedSlot s : slots.values()) {
            if (s.machineNom == null) continue;
            out.computeIfAbsent(s.machineNom, k -> new java.util.ArrayList<>()).add(s);
        }
        for (List<PlannedSlot> list : out.values()) {
            list.sort(Comparator.comparing(PlannedSlot::getPlannedStart,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return out;
    }

    // ------------------------------------------------------------------ Slices 3/4 mutators
    //
    // The engine's SA loop owns these — they are NOT thread-safe with concurrent
    // readers because no copy is made. The optimizer mutates the snapshot inside
    // its single iteration thread; outside readers must use copyOfSlots().

    /**
     * Slice 3 — change the machine (and zone) of one slot in place. Returns the
     * old slot for undo, or null when the (serie, phase) is not in the schedule.
     * Timing fields are kept as-is on purpose: per-machine timing is the engine's
     * concern (it lives in cached aggregates) and a full timing recompute is
     * deferred to the next snapshot rebuild.
     */
    public PlannedSlot mutateSlotMachine(String serieId, Phase phase,
                                          String newMachineNom, String newZoneNom) {
        Key k = new Key(serieId, phase);
        PlannedSlot old = slots.get(k);
        if (old == null) return null;
        PlannedSlot replaced = new PlannedSlot(old.getSerieId(), old.getSequenceId(),
                old.getPhase(), newMachineNom, newZoneNom,
                old.getPlannedStart(), old.getPlannedEnd());
        slots.put(k, replaced);
        return old;
    }

    /**
     * Slice 4 — swap two adjacent slots in a machine's queue. The total
     * (start..end) span of the two slots is preserved, so downstream slots
     * are untouched. Returns true when applied.
     *
     * <p>Adjacent swap is the only reorder operator that does NOT ripple
     * timing changes to other slots, which makes it cheap and revertable.</p>
     */
    public boolean swapAdjacentInQueue(String machineNom, int lowerIdx) {
        List<PlannedSlot> queue = slotsForMachine(machineNom);
        if (queue.size() < 2 || lowerIdx < 0 || lowerIdx >= queue.size() - 1) return false;
        PlannedSlot a = queue.get(lowerIdx);
        PlannedSlot b = queue.get(lowerIdx + 1);
        if (a.getPlannedStart() == null || a.getPlannedEnd() == null
                || b.getPlannedStart() == null || b.getPlannedEnd() == null) return false;
        long durA = java.time.Duration.between(a.getPlannedStart(), a.getPlannedEnd()).toMinutes();
        long durB = java.time.Duration.between(b.getPlannedStart(), b.getPlannedEnd()).toMinutes();
        java.time.LocalDateTime origStart = a.getPlannedStart();
        java.time.LocalDateTime newBEnd = origStart.plusMinutes(durB);
        java.time.LocalDateTime newAEnd = newBEnd.plusMinutes(durA);
        PlannedSlot newA = new PlannedSlot(a.getSerieId(), a.getSequenceId(), a.getPhase(),
                a.getMachineNom(), a.getZoneNom(), newBEnd, newAEnd);
        PlannedSlot newB = new PlannedSlot(b.getSerieId(), b.getSequenceId(), b.getPhase(),
                b.getMachineNom(), b.getZoneNom(), origStart, newBEnd);
        slots.put(new Key(a.getSerieId(), a.getPhase()), newA);
        slots.put(new Key(b.getSerieId(), b.getPhase()), newB);
        return true;
    }

    /** Empty snapshot — used before the first build. */
    public static ScheduleSnapshot empty() {
        return new ScheduleSnapshot();
    }
}
