package com.lear.ctc.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sequence_details")
public class SequenceDetails {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
//SELECT   TOP (200) id, sequence, serial_number, marker, material_part_number, created_at, updated_at
    @Column(name = "sequence")
    private String sequence;
    @Column(name = "serial_number")
    private String serialNumber;
    @Column(name = "marker")
    private String marker;
    @Column(name = "material_part_number")
    private String materialPartNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public String getMaterialPartNumber() {
        return materialPartNumber;
    }

    public void setMaterialPartNumber(String materialPartNumber) {
        this.materialPartNumber = materialPartNumber;
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
