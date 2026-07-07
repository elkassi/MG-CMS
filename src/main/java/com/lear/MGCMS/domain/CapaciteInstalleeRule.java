package com.lear.MGCMS.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Interval-based default/override rules for {@link CapaciteInstallee} values.
 *
 * <p>{@code CapaciteInstallee} itself is left unchanged, so the dispatcher and
 * every other consumer are unaffected. These rules only feed the resolution
 * fallback in {@code CapaciteInstalleeService.getEffective}:</p>
 * <ol>
 *   <li>an explicit {@code CapaciteInstallee} row for the exact (date, shift,
 *       groupe) always wins;</li>
 *   <li>otherwise the best-matching rule here is applied;</li>
 *   <li>otherwise the legacy null-date/null-shift default row;</li>
 *   <li>otherwise hardcoded defaults.</li>
 * </ol>
 *
 * <p>A rule applies to a date when the date falls in {@code [dateDebut, dateFin]}
 * (null {@code dateDebut} = since forever, null {@code dateFin} = until forever)
 * AND the optional {@code dayOfWeek} / {@code shiftNumber} / {@code groupe}
 * match (null = matches anything). Among matching rules the most specific (most
 * non-null conditions) wins, and values are layered field-by-field: a
 * "Friday + shift 2 -&gt; tempsTotalParMachine 440" rule overrides only that
 * field while inheriting {@code capaciteInstallee}/{@code efficienceTarget}
 * from a broader baseline rule.</p>
 */
@Entity
@Table(name = "capacite_installee_rule")
public class CapaciteInstalleeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Interval start (inclusive); null = applies since forever. */
    @Column(name = "date_debut")
    private LocalDate dateDebut;

    /** Interval end (inclusive); null = applies until forever. */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /** 1=Monday .. 7=Sunday ({@link java.time.DayOfWeek#getValue()}); null = any day. */
    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    /** 1/2/3; null = any shift. */
    @Column(name = "shift_number")
    private Integer shiftNumber;

    /** "Coupe"/"Laser"; null = any group. */
    @Column(name = "groupe", length = 50)
    private String groupe;

    /** Installed machine count; null = inherit from a broader rule / default. */
    @Column(name = "capacite_installee")
    private Integer capaciteInstallee;

    /** Opening time per machine (minutes); null = inherit. */
    @Column(name = "temps_total_par_machine")
    private Double tempsTotalParMachine;

    /** Group efficiency target (%); kept for dispatcher compatibility; null = inherit. */
    @Column(name = "efficience_target")
    private Double efficienceTarget;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CapaciteInstalleeRule() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getShiftNumber() {
        return shiftNumber;
    }

    public void setShiftNumber(Integer shiftNumber) {
        this.shiftNumber = shiftNumber;
    }

    public String getGroupe() {
        return groupe;
    }

    public void setGroupe(String groupe) {
        this.groupe = groupe;
    }

    public Integer getCapaciteInstallee() {
        return capaciteInstallee;
    }

    public void setCapaciteInstallee(Integer capaciteInstallee) {
        this.capaciteInstallee = capaciteInstallee;
    }

    public Double getTempsTotalParMachine() {
        return tempsTotalParMachine;
    }

    public void setTempsTotalParMachine(Double tempsTotalParMachine) {
        this.tempsTotalParMachine = tempsTotalParMachine;
    }

    public Double getEfficienceTarget() {
        return efficienceTarget;
    }

    public void setEfficienceTarget(Double efficienceTarget) {
        this.efficienceTarget = efficienceTarget;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
