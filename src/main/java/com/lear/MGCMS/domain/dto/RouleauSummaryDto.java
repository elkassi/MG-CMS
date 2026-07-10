package com.lear.MGCMS.domain.dto;

public class RouleauSummaryDto {
    private String rollId;
    private String serialId;
    private String itemNumber;
    private Double quantity;
    private String status; // "Blocked", "Consommé", "In stock", "In production"
    private String lot;
    private String location;
    private Boolean isFullyConsumed; // keeping this just in case, though status="Consommé" covers it

    public RouleauSummaryDto() {
    }

    public RouleauSummaryDto(String rollId, String serialId, String itemNumber, Double quantity, String status, String lot, String location) {
        this.rollId = rollId;
        this.serialId = serialId;
        this.itemNumber = itemNumber;
        this.quantity = quantity;
        this.status = status;
        this.lot = lot;
        this.location = location;
    }

    public String getRollId() {
        return rollId;
    }

    public void setRollId(String rollId) {
        this.rollId = rollId;
    }

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(String serialId) {
        this.serialId = serialId;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getIsFullyConsumed() {
        return isFullyConsumed;
    }

    public void setIsFullyConsumed(Boolean isFullyConsumed) {
        this.isFullyConsumed = isFullyConsumed;
    }
}
