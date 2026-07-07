package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit trail of changes on {@link ProgramCNC}: one row per creation / update / delete,
 * holding when, who, the operation, and a snapshot (ProgramCNC.toString()).
 */
@Entity
@Table(name = "ProgramCNCHistory")
public class ProgramCNCHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "operationDate")
    private LocalDateTime operationDate;

    @Column(name = "username")
    private String username;

    @Column(name = "operation")
    private String operation; // CREATION / UPDATE / DELETE

    @Column(name = "snapshot")
    private String snapshot; // ProgramCNC.toString()

    public ProgramCNCHistory() {}

    public ProgramCNCHistory(LocalDateTime operationDate, String username, String operation, String snapshot) {
        this.operationDate = operationDate;
        this.username = username;
        this.operation = operation;
        this.snapshot = snapshot;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getOperationDate() { return operationDate; }
    public void setOperationDate(LocalDateTime operationDate) { this.operationDate = operationDate; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getSnapshot() { return snapshot; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }
}
