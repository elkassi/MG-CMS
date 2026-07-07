package com.lear.MGCMS.payload;

public class TimingPlacement {
    private String placementTimingModel;
    private Double validatedCuttingTimeTimingModel;
    private Double cuttingTimeTimingModel;
    private Double spreadingTimingModel;

    public String getPlacementTimingModel() {
        return placementTimingModel;
    }

    public void setPlacementTimingModel(String placementTimingModel) {
        this.placementTimingModel = placementTimingModel;
    }

    public Double getValidatedCuttingTimeTimingModel() {
        return validatedCuttingTimeTimingModel;
    }

    public void setValidatedCuttingTimeTimingModel(Double validatedCuttingTimeTimingModel) {
        this.validatedCuttingTimeTimingModel = validatedCuttingTimeTimingModel;
    }

    public Double getCuttingTimeTimingModel() {
        return cuttingTimeTimingModel;
    }

    public void setCuttingTimeTimingModel(Double cuttingTimeTimingModel) {
        this.cuttingTimeTimingModel = cuttingTimeTimingModel;
    }

    public Double getSpreadingTimingModel() {
        return spreadingTimingModel;
    }

    public void setSpreadingTimingModel(Double spreadingTimingModel) {
        this.spreadingTimingModel = spreadingTimingModel;
    }
}
