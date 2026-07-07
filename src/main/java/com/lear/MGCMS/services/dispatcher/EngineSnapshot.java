package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory snapshot of the dispatch optimizer's view of a (date, shift).
 * Mutable during the improving loop so the hot path never hits the database.
 */
public class EngineSnapshot {

    private final LocalDate date;
    private final int shift;
    private final Map<CellKey, CellLoad> baselineLoads;
    private final Map<CellKey, CellLoad> pendingLoads;
    private final List<SequenceCandidate> candidates;
    private double currentSpread;

    public EngineSnapshot(LocalDate date, int shift,
                          Map<CellKey, CellLoad> baselineLoads,
                          Map<CellKey, CellLoad> pendingLoads,
                          List<SequenceCandidate> candidates,
                          double currentSpread) {
        this.date = date;
        this.shift = shift;
        this.baselineLoads = baselineLoads != null ? baselineLoads : Collections.emptyMap();
        this.pendingLoads = pendingLoads != null ? pendingLoads : Collections.emptyMap();
        this.candidates = candidates != null ? candidates : Collections.emptyList();
        this.currentSpread = currentSpread;
    }

    public LocalDate getDate() { return date; }
    public int getShift() { return shift; }
    public Map<CellKey, CellLoad> getBaselineLoads() { return baselineLoads; }
    public Map<CellKey, CellLoad> getPendingLoads() { return pendingLoads; }
    public List<SequenceCandidate> getCandidates() { return candidates; }
    public double getCurrentSpread() { return currentSpread; }
    public void setCurrentSpread(double currentSpread) { this.currentSpread = currentSpread; }

    /** Composite key for a (machineType, zone) cell on the load matrix. */
    public static final class CellKey {
        private final String machineType;
        private final String zoneNom;

        public CellKey(String machineType, String zoneNom) {
            this.machineType = machineType;
            this.zoneNom = zoneNom;
        }

        public String getMachineType() { return machineType; }
        public String getZoneNom() { return zoneNom; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CellKey)) return false;
            CellKey c = (CellKey) o;
            return Objects.equals(machineType, c.machineType)
                    && Objects.equals(zoneNom, c.zoneNom);
        }

        @Override
        public int hashCode() {
            return Objects.hash(machineType, zoneNom);
        }
    }

    /** One cell's load state. Capacity is immutable after warming; plannedMinutes moves. */
    public static final class CellLoad {
        private double plannedMinutes;
        private final double capacityMinutes;

        public CellLoad(double capacityMinutes) {
            this.capacityMinutes = capacityMinutes;
        }

        public double getPlannedMinutes() { return plannedMinutes; }
        public void setPlannedMinutes(double plannedMinutes) { this.plannedMinutes = plannedMinutes; }
        public double getCapacityMinutes() { return capacityMinutes; }
    }

    /** One pending sequence that the engine is free to move between zones. */
    public static final class SequenceCandidate {
        private final String sequence;
        private final String primaryMachineType;
        private String currentZone;
        private final List<String> possibleZones;
        /** Per-machine-type cutting minutes when this sequence is placed in a zone. */
        private final Map<String, Double> loadByMachineType;

        public SequenceCandidate(String sequence, String primaryMachineType,
                                 String currentZone, List<String> possibleZones,
                                 Map<String, Double> loadByMachineType) {
            this.sequence = sequence;
            this.primaryMachineType = primaryMachineType;
            this.currentZone = currentZone;
            this.possibleZones = possibleZones != null ? possibleZones : Collections.emptyList();
            this.loadByMachineType = loadByMachineType != null ? loadByMachineType : Collections.emptyMap();
        }

        public String getSequence() { return sequence; }
        public String getPrimaryMachineType() { return primaryMachineType; }
        public String getCurrentZone() { return currentZone; }
        public void setCurrentZone(String currentZone) { this.currentZone = currentZone; }
        public List<String> getPossibleZones() { return possibleZones; }
        public Map<String, Double> getLoadByMachineType() { return loadByMachineType; }
    }
}
