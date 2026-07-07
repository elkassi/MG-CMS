package com.lear.MGCMS.domain.dispatcher;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * One zone suggestion produced by the engine for a specific sequence.
 * Backed by table {@code dispatch_engine_run_suggestion} (Flyway {@code V13_07}).
 * Composite PK = (run_id, sequence).
 */
@Entity
@Table(name = "dispatch_engine_run_suggestion")
public class DispatchEngineRunSuggestion {

    @Embeddable
    public static class Pk implements Serializable {
        @Column(name = "run_id")
        private Long runId;

        @Column(name = "sequence", length = 64)
        private String sequence;

        public Pk() {}

        public Pk(Long runId, String sequence) {
            this.runId = runId;
            this.sequence = sequence;
        }

        public Long getRunId() { return runId; }
        public void setRunId(Long runId) { this.runId = runId; }

        public String getSequence() { return sequence; }
        public void setSequence(String sequence) { this.sequence = sequence; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk)) return false;
            Pk pk = (Pk) o;
            return Objects.equals(runId, pk.runId)
                    && Objects.equals(sequence, pk.sequence);
        }

        @Override
        public int hashCode() {
            return Objects.hash(runId, sequence);
        }
    }

    @EmbeddedId
    private Pk id;

    @Column(name = "suggested_zone", length = 64, nullable = false)
    private String suggestedZone;

    @Column(name = "previous_zone", length = 64)
    private String previousZone;

    public DispatchEngineRunSuggestion() {}

    public Pk getId() { return id; }
    public void setId(Pk id) { this.id = id; }

    public String getSuggestedZone() { return suggestedZone; }
    public void setSuggestedZone(String suggestedZone) { this.suggestedZone = suggestedZone; }

    public String getPreviousZone() { return previousZone; }
    public void setPreviousZone(String previousZone) { this.previousZone = previousZone; }
}
