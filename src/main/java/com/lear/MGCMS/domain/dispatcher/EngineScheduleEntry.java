package com.lear.MGCMS.domain.dispatcher;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * One planned slot in the engine's current best schedule.
 *
 * <p>Two rows per serie: one for {@link Phase#MATELASSAGE} and one for
 * {@link Phase#COUPE}. The engine replaces the table on every snapshot
 * write — we hold the latest "best" only, not a history. Historical runs
 * live in {@code dispatch_engine_run_suggestion}.</p>
 *
 * <p>Backed by Flyway migration {@code V15_01}.</p>
 */
@Entity
@Table(name = "engine_schedule_entry")
public class EngineScheduleEntry {

    public enum Phase {
        MATELASSAGE,
        COUPE
    }

    @EmbeddedId
    private Pk id;

    @Column(name = "machine_nom", length = 64)
    private String machineNom;

    @Column(name = "sequence_id", length = 64, nullable = false)
    private String sequenceId;

    @Column(name = "zone_nom", length = 64)
    private String zoneNom;

    @Column(name = "planned_start")
    private LocalDateTime plannedStart;

    @Column(name = "planned_end")
    private LocalDateTime plannedEnd;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "planned_at", nullable = false)
    private LocalDateTime plannedAt;

    public EngineScheduleEntry() {}

    public EngineScheduleEntry(String serieId, Phase phase, String machineNom, String sequenceId,
                               String zoneNom, LocalDateTime plannedStart, LocalDateTime plannedEnd,
                               Long runId, LocalDateTime plannedAt) {
        this.id = new Pk(serieId, phase);
        this.machineNom = machineNom;
        this.sequenceId = sequenceId;
        this.zoneNom = zoneNom;
        this.plannedStart = plannedStart;
        this.plannedEnd = plannedEnd;
        this.runId = runId;
        this.plannedAt = plannedAt;
    }

    public Pk getId() { return id; }
    public void setId(Pk id) { this.id = id; }

    public String getMachineNom() { return machineNom; }
    public void setMachineNom(String v) { this.machineNom = v; }

    public String getSequenceId() { return sequenceId; }
    public void setSequenceId(String v) { this.sequenceId = v; }

    public String getZoneNom() { return zoneNom; }
    public void setZoneNom(String v) { this.zoneNom = v; }

    public LocalDateTime getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDateTime v) { this.plannedStart = v; }

    public LocalDateTime getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDateTime v) { this.plannedEnd = v; }

    public Long getRunId() { return runId; }
    public void setRunId(Long v) { this.runId = v; }

    public LocalDateTime getPlannedAt() { return plannedAt; }
    public void setPlannedAt(LocalDateTime v) { this.plannedAt = v; }

    @Embeddable
    public static class Pk implements Serializable {
        @Column(name = "serie_id", length = 64, nullable = false)
        private String serieId;

        @Enumerated(EnumType.STRING)
        @Column(name = "phase", length = 16, nullable = false)
        private Phase phase;

        public Pk() {}

        public Pk(String serieId, Phase phase) {
            this.serieId = serieId;
            this.phase = phase;
        }

        public String getSerieId() { return serieId; }
        public void setSerieId(String v) { this.serieId = v; }

        public Phase getPhase() { return phase; }
        public void setPhase(Phase v) { this.phase = v; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk)) return false;
            Pk pk = (Pk) o;
            return Objects.equals(serieId, pk.serieId) && phase == pk.phase;
        }

        @Override
        public int hashCode() {
            return Objects.hash(serieId, phase);
        }
    }
}
