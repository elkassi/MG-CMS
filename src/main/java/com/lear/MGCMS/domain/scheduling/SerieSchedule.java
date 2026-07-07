package com.lear.MGCMS.domain.scheduling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.ProductionTable;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to store individual serie scheduling information
 * Tracks spreading and cutting schedule, machine assignments, and progress
 */
@Entity
@Table(name = "SerieSchedule")
public class SerieSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serieId;
    private String sequenceId;

    // Spreading schedule
    private String spreadingTable;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime spreadingStartEstimated;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime spreadingEndEstimated;
    private Integer spreadingTimeMinutes;

    // Cutting schedule
    @ManyToOne
    private ProductionTable cuttingMachine;
    private String cuttingMachineName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cuttingStartEstimated;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cuttingEndEstimated;
    private Integer cuttingTimeMinutes;

    // Material info
    private String partNumberMaterial;
    private Double longueur;
    private Integer nbrCouche;

    // Status
    private String spreadingStatus; // WAITING, IN_PROGRESS, COMPLETE
    private String cuttingStatus; // WAITING, IN_PROGRESS, COMPLETE

    // Scheduling metadata
    private Integer schedulingOrder; // Order in which this should be processed
    private String assignmentReason; // Why this machine was assigned

    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public SerieSchedule() {
        this.createdAt = LocalDateTime.now();
        this.spreadingStatus = "WAITING";
        this.cuttingStatus = "WAITING";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSerieId() {
        return serieId;
    }

    public void setSerieId(String serieId) {
        this.serieId = serieId;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getSpreadingTable() {
        return spreadingTable;
    }

    public void setSpreadingTable(String spreadingTable) {
        this.spreadingTable = spreadingTable;
    }

    public LocalDateTime getSpreadingStartEstimated() {
        return spreadingStartEstimated;
    }

    public void setSpreadingStartEstimated(LocalDateTime spreadingStartEstimated) {
        this.spreadingStartEstimated = spreadingStartEstimated;
    }

    public LocalDateTime getSpreadingEndEstimated() {
        return spreadingEndEstimated;
    }

    public void setSpreadingEndEstimated(LocalDateTime spreadingEndEstimated) {
        this.spreadingEndEstimated = spreadingEndEstimated;
    }

    public Integer getSpreadingTimeMinutes() {
        return spreadingTimeMinutes;
    }

    public void setSpreadingTimeMinutes(Integer spreadingTimeMinutes) {
        this.spreadingTimeMinutes = spreadingTimeMinutes;
    }

    public ProductionTable getCuttingMachine() {
        return cuttingMachine;
    }

    public void setCuttingMachine(ProductionTable cuttingMachine) {
        this.cuttingMachine = cuttingMachine;
    }

    public String getCuttingMachineName() {
        return cuttingMachineName;
    }

    public void setCuttingMachineName(String cuttingMachineName) {
        this.cuttingMachineName = cuttingMachineName;
    }

    public LocalDateTime getCuttingStartEstimated() {
        return cuttingStartEstimated;
    }

    public void setCuttingStartEstimated(LocalDateTime cuttingStartEstimated) {
        this.cuttingStartEstimated = cuttingStartEstimated;
    }

    public LocalDateTime getCuttingEndEstimated() {
        return cuttingEndEstimated;
    }

    public void setCuttingEndEstimated(LocalDateTime cuttingEndEstimated) {
        this.cuttingEndEstimated = cuttingEndEstimated;
    }

    public Integer getCuttingTimeMinutes() {
        return cuttingTimeMinutes;
    }

    public void setCuttingTimeMinutes(Integer cuttingTimeMinutes) {
        this.cuttingTimeMinutes = cuttingTimeMinutes;
    }

    public String getPartNumberMaterial() {
        return partNumberMaterial;
    }

    public void setPartNumberMaterial(String partNumberMaterial) {
        this.partNumberMaterial = partNumberMaterial;
    }

    public Double getLongueur() {
        return longueur;
    }

    public void setLongueur(Double longueur) {
        this.longueur = longueur;
    }

    public Integer getNbrCouche() {
        return nbrCouche;
    }

    public void setNbrCouche(Integer nbrCouche) {
        this.nbrCouche = nbrCouche;
    }

    public String getSpreadingStatus() {
        return spreadingStatus;
    }

    public void setSpreadingStatus(String spreadingStatus) {
        this.spreadingStatus = spreadingStatus;
    }

    public String getCuttingStatus() {
        return cuttingStatus;
    }

    public void setCuttingStatus(String cuttingStatus) {
        this.cuttingStatus = cuttingStatus;
    }

    public Integer getSchedulingOrder() {
        return schedulingOrder;
    }

    public void setSchedulingOrder(Integer schedulingOrder) {
        this.schedulingOrder = schedulingOrder;
    }

    public String getAssignmentReason() {
        return assignmentReason;
    }

    public void setAssignmentReason(String assignmentReason) {
        this.assignmentReason = assignmentReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

