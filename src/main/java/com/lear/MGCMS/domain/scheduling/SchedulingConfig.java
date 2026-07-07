package com.lear.MGCMS.domain.scheduling;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Persists the user's dispatch algorithm preferences per zone.
 * Each zone (or global default with zone=null) stores:
 *  - selected algorithm (SCG, SPT, LPT, EDF, CR, WSPT, ATC, MATERIAL_GROUP)
 *  - weight sliders for multi-objective scoring
 *  - preset name (Express, Balanced, Efficient, Custom)
 */
@Entity
@Table(name = "scheduling_config")
public class SchedulingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Zone code (nullable = global default) */
    @Column(name = "zone_code")
    private String zoneCode;

    /** Selected dispatch algorithm key */
    @Column(nullable = false, length = 30)
    private String algorithm = "SCG";

    /** Preset name: EXPRESS, BALANCED, EFFICIENT, CUSTOM */
    @Column(length = 30)
    private String preset = "BALANCED";

    // ─── Weight sliders (0–100) for multi-objective scoring ───

    /** Weight for box completion time minimization */
    @Column(nullable = false)
    private int weightBoxCompletion = 40;

    /** Weight for machine utilization maximization */
    @Column(nullable = false)
    private int weightMachineUtilization = 30;

    /** Weight for material changeover minimization */
    @Column(nullable = false)
    private int weightChangeover = 20;

    /** Weight for due date adherence */
    @Column(nullable = false)
    private int weightDueDate = 10;

    // ─── Auto-dispatch toggle ───

    /** If true, auto-dispatch applies assignments without manual approval */
    @Column(nullable = false)
    private boolean autoApply = false;

    /** Auto-dispatch interval in minutes (default 5) */
    @Column(nullable = false)
    private int autoDispatchIntervalMinutes = 5;

    // ─── Audit ───

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String updatedBy;

    @PrePersist
    @PreUpdate
    void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ───

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }

    public int getWeightBoxCompletion() { return weightBoxCompletion; }
    public void setWeightBoxCompletion(int w) { this.weightBoxCompletion = w; }

    public int getWeightMachineUtilization() { return weightMachineUtilization; }
    public void setWeightMachineUtilization(int w) { this.weightMachineUtilization = w; }

    public int getWeightChangeover() { return weightChangeover; }
    public void setWeightChangeover(int w) { this.weightChangeover = w; }

    public int getWeightDueDate() { return weightDueDate; }
    public void setWeightDueDate(int w) { this.weightDueDate = w; }

    public boolean isAutoApply() { return autoApply; }
    public void setAutoApply(boolean autoApply) { this.autoApply = autoApply; }

    public int getAutoDispatchIntervalMinutes() { return autoDispatchIntervalMinutes; }
    public void setAutoDispatchIntervalMinutes(int m) { this.autoDispatchIntervalMinutes = m; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // ─── Preset factories ───

    public static SchedulingConfig express() {
        SchedulingConfig c = new SchedulingConfig();
        c.algorithm = "SPT";
        c.preset = "EXPRESS";
        c.weightBoxCompletion = 60;
        c.weightMachineUtilization = 20;
        c.weightChangeover = 10;
        c.weightDueDate = 10;
        return c;
    }

    public static SchedulingConfig balanced() {
        SchedulingConfig c = new SchedulingConfig();
        c.algorithm = "SCG";
        c.preset = "BALANCED";
        c.weightBoxCompletion = 40;
        c.weightMachineUtilization = 30;
        c.weightChangeover = 20;
        c.weightDueDate = 10;
        return c;
    }

    public static SchedulingConfig efficient() {
        SchedulingConfig c = new SchedulingConfig();
        c.algorithm = "MATERIAL_GROUP";
        c.preset = "EFFICIENT";
        c.weightBoxCompletion = 25;
        c.weightMachineUtilization = 40;
        c.weightChangeover = 30;
        c.weightDueDate = 5;
        return c;
    }
}
