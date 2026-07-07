package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class ReftissuPropertyId implements Serializable {
    private String reftissu;

    private String property;

    public ReftissuPropertyId(String reftissu, String property) {
        super();
        this.reftissu = reftissu;
        this.property = property;
    }

    public ReftissuPropertyId() {
        super();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReftissuPropertyId accountId = (ReftissuPropertyId) o;
        return reftissu.equals(accountId.reftissu) &&
                property.equals(accountId.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reftissu, property);
    }

}
