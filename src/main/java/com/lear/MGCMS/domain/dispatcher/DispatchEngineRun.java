package com.lear.MGCMS.domain.dispatcher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * One execution of the continuous dispatch optimizer.
 * Backed by table {@code dispatch_engine_run} (Flyway {@code V13_07}).
 */
@Entity
@Table(name = "dispatch_engine_run")
public class DispatchEngineRun {

    public enum Mode {
        CONTINUOUS,
        FIXED_DURATION,
        ALTERNATING
    }

    public enum FinalState {
        STOPPED,
        ABORTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 32, nullable = false)
    private Mode mode;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "started_by", length = 50, nullable = false)
    private String startedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_state", length = 32, nullable = false)
    private FinalState finalState;

    @Column(name = "iterations", nullable = false)
    private Integer iterations = 0;

    @Column(name = "improvements", nullable = false)
    private Integer improvements = 0;

    @Column(name = "initial_spread", precision = 6, scale = 2)
    private BigDecimal initialSpread;

    @Column(name = "final_spread", precision = 6, scale = 2)
    private BigDecimal finalSpread;

    @Column(name = "notes", length = 512)
    private String notes;

    public DispatchEngineRun() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }

    public String getStartedBy() { return startedBy; }
    public void setStartedBy(String startedBy) { this.startedBy = startedBy; }

    public FinalState getFinalState() { return finalState; }
    public void setFinalState(FinalState finalState) { this.finalState = finalState; }

    public Integer getIterations() { return iterations; }
    public void setIterations(Integer iterations) { this.iterations = iterations; }

    public Integer getImprovements() { return improvements; }
    public void setImprovements(Integer improvements) { this.improvements = improvements; }

    public BigDecimal getInitialSpread() { return initialSpread; }
    public void setInitialSpread(BigDecimal initialSpread) { this.initialSpread = initialSpread; }

    public BigDecimal getFinalSpread() { return finalSpread; }
    public void setFinalSpread(BigDecimal finalSpread) { this.finalSpread = finalSpread; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
