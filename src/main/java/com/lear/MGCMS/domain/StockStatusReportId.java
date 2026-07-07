package com.lear.MGCMS.domain;

import javax.persistence.Id;
import java.io.Serializable;
import java.util.Objects;

public class StockStatusReportId implements Serializable {
    private String itemNumber;
    private String location;
    private String ref;
    private Double qtyOnHand;


    public StockStatusReportId(String itemNumber, String location, String ref, Double qtyOnHand) {
        super();
        this.itemNumber = itemNumber;
        this.location = location;
        this.ref = ref;
        this.qtyOnHand = qtyOnHand;
    }

    public StockStatusReportId() {
        super();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockStatusReportId accountId = (StockStatusReportId) o;
        return itemNumber.equals(accountId.itemNumber) &&
                location.equals(accountId.location) &&
                ref.equals(accountId.ref) &&
                qtyOnHand.equals(accountId.qtyOnHand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemNumber, location, ref, qtyOnHand);
    }

}
