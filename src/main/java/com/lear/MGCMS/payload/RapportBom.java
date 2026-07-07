package com.lear.MGCMS.payload;

public class RapportBom {
    /*
    select Item_Parent, Component, [Quantity Per], Scrap
     */
    private String itemParent;
    private String component;
    private double quantityPer;
    private double scrap;

    public String getItemParent() {
        return itemParent;
    }

    public void setItemParent(String itemParent) {
        this.itemParent = itemParent;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public double getQuantityPer() {
        return quantityPer;
    }

    public void setQuantityPer(double quantityPer) {
        this.quantityPer = quantityPer;
    }

    public double getScrap() {
        return scrap;
    }

    public void setScrap(double scrap) {
        this.scrap = scrap;
    }
}
