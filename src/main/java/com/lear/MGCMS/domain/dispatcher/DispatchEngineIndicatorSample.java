package com.lear.MGCMS.domain.dispatcher;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * One sample of the optimizer's objective value taken during the improving loop.
 * Backed by table {@code dispatch_engine_indicator_sample} (Flyway {@code V13_07}).
 * Composite PK = (run_id, iteration).
 */
@Entity
@Table(name = "dispatch_engine_indicator_sample")
public class DispatchEngineIndicatorSample {

    @Embeddable
    public static class Pk implements Serializable {
        @Column(name = "run_id")
        private Long runId;

        @Column(name = "iteration")
        private Integer iteration;

        public Pk() {}

        public Pk(Long runId, Integer iteration) {
            this.runId = runId;
            this.iteration = iteration;
        }

        public Long getRunId() { return runId; }
        public void setRunId(Long runId) { this.runId = runId; }

        public Integer getIteration() { return iteration; }
        public void setIteration(Integer iteration) { this.iteration = iteration; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk)) return false;
            Pk pk = (Pk) o;
            return Objects.equals(runId, pk.runId)
                    && Objects.equals(iteration, pk.iteration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(runId, iteration);
        }
    }

    @EmbeddedId
    private Pk id;

    @Column(name = "sample_at", nullable = false)
    private LocalDateTime sampleAt;

    @Column(name = "spread_pct", precision = 6, scale = 2, nullable = false)
    private BigDecimal spreadPct;

    @Column(name = "max_load_pct", precision = 6, scale = 2, nullable = false)
    private BigDecimal maxLoadPct;

    @Column(name = "min_load_pct", precision = 6, scale = 2, nullable = false)
    private BigDecimal minLoadPct;

    @Column(name = "accepted", nullable = false)
    private boolean accepted = false;

    public DispatchEngineIndicatorSample() {}

    public Pk getId() { return id; }
    public void setId(Pk id) { this.id = id; }

    public LocalDateTime getSampleAt() { return sampleAt; }
    public void setSampleAt(LocalDateTime sampleAt) { this.sampleAt = sampleAt; }

    public BigDecimal getSpreadPct() { return spreadPct; }
    public void setSpreadPct(BigDecimal spreadPct) { this.spreadPct = spreadPct; }

    public BigDecimal getMaxLoadPct() { return maxLoadPct; }
    public void setMaxLoadPct(BigDecimal maxLoadPct) { this.maxLoadPct = maxLoadPct; }

    public BigDecimal getMinLoadPct() { return minLoadPct; }
    public void setMinLoadPct(BigDecimal minLoadPct) { this.minLoadPct = minLoadPct; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}
