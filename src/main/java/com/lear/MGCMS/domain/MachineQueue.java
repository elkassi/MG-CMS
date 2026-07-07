package com.lear.MGCMS.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "machine_queue", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"machineNom", "queuePosition"})
})
public class MachineQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String machineNom;

    @Column(nullable = false)
    private Integer queuePosition; // 1, 2, or 3

    @Column(nullable = false, length = 100)
    private String serie;

    @Column(length = 100)
    private String sequenceId;

    @Column(length = 200)
    private String partNumberMaterial;

    private Double longueur;

    private Double estimatedCuttingTime;

    private LocalDateTime estimatedStartTime;

    private LocalDateTime estimatedEndTime;

    @Column(length = 100)
    private String assignedBy;

    private LocalDateTime assignedAt;

    /**
     * Monotonic version counter bumped by every {@code saveQueues()} commit
     * on this machine. Used by the operator kiosk to cheaply detect queue
     * changes without subscribing to WebSocket topics.
     *
     * <p>Managed by {@code OrdonnancementService} starting Phase 7 — NOT by
     * Hibernate's {@code @Version} optimistic-locking mechanism.</p>
     */
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = LocalDateTime.now();
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }

    public MachineQueue() {}

    public MachineQueue(String machineNom, Integer queuePosition, String serie,
                        String sequenceId, String partNumberMaterial, Double longueur,
                        Double estimatedCuttingTime) {
        this.machineNom = machineNom;
        this.queuePosition = queuePosition;
        this.serie = serie;
        this.sequenceId = sequenceId;
        this.partNumberMaterial = partNumberMaterial;
        this.longueur = longueur;
        this.estimatedCuttingTime = estimatedCuttingTime;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMachineNom() { return machineNom; }
    public void setMachineNom(String machineNom) { this.machineNom = machineNom; }

    public Integer getQueuePosition() { return queuePosition; }
    public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getSequenceId() { return sequenceId; }
    public void setSequenceId(String sequenceId) { this.sequenceId = sequenceId; }

    public String getPartNumberMaterial() { return partNumberMaterial; }
    public void setPartNumberMaterial(String partNumberMaterial) { this.partNumberMaterial = partNumberMaterial; }

    public Double getLongueur() { return longueur; }
    public void setLongueur(Double longueur) { this.longueur = longueur; }

    public Double getEstimatedCuttingTime() { return estimatedCuttingTime; }
    public void setEstimatedCuttingTime(Double estimatedCuttingTime) { this.estimatedCuttingTime = estimatedCuttingTime; }

    public LocalDateTime getEstimatedStartTime() { return estimatedStartTime; }
    public void setEstimatedStartTime(LocalDateTime estimatedStartTime) { this.estimatedStartTime = estimatedStartTime; }

    public LocalDateTime getEstimatedEndTime() { return estimatedEndTime; }
    public void setEstimatedEndTime(LocalDateTime estimatedEndTime) { this.estimatedEndTime = estimatedEndTime; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
