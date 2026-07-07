package com.lear.MGCMS.domain.dto;

public class RouleauSummaryDto {
    private String rollId;
    private String serialId;
    private String itemNumber;
    private Double qtyMeters;
    private String r100Location;
    private String locationType; // "In stock", "Not in stock", "In use"
    private String lot;
    private String emplacement;

    public RouleauSummaryDto() {
    }

    public RouleauSummaryDto(String rollId, String serialId, String itemNumber, Double qtyMeters, String r100Location, String locationType, String lot, String emplacement) {
        this.rollId = rollId;
        this.serialId = serialId;
        this.itemNumber = itemNumber;
        this.qtyMeters = qtyMeters;
        this.r100Location = r100Location;
        this.locationType = locationType;
        this.lot = lot;
        this.emplacement = emplacement;
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

    public Double getQtyMeters() {
        return qtyMeters;
    }

    public void setQtyMeters(Double qtyMeters) {
        this.qtyMeters = qtyMeters;
    }

    public String getR100Location() {
        return r100Location;
    }

    public void setR100Location(String r100Location) {
        this.r100Location = r100Location;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public String getEmplacement() {
        return emplacement;
    }

    public void setEmplacement(String emplacement) {
        this.emplacement = emplacement;
    }
}
