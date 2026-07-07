package com.lear.splice.domain;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "markers_only_code")
public class MarkersOnlyCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @NotNull(message = "Order code is required")
    @Column(name = "order_code")
    private String orderCode;
    @NotNull(message = "Fabric type is required")
    @Column(name = "fabric_type")
    private String fabricType;
    @NotNull(message = "Marker is required")
    @Column(name = "marker")
    private String marker;
    @NotNull(message = "Layers is required")
    @Column(name = "layers")
    private int layers;
    @NotNull(message = "Multiply is required")
    @Column(name = "multiply")
    private int multiply;
    @Column(name = "status")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getFabricType() {
        return fabricType;
    }

    public void setFabricType(String fabricType) {
        this.fabricType = fabricType;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public int getLayers() {
        return layers;
    }

    public void setLayers(int layers) {
        this.layers = layers;
    }

    public int getMultiply() {
        return multiply;
    }

    public void setMultiply(int multiply) {
        this.multiply = multiply;
    }
}
