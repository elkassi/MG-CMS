package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class ReftissuMarginId implements Serializable{

	private Integer intervalId;

	private String partNumberMaterialConfig;

	public ReftissuMarginId(Integer intervalId, String partNumberMaterialConfig) {
		super();
		this.intervalId = intervalId;
		this.partNumberMaterialConfig = partNumberMaterialConfig;
	}

	public ReftissuMarginId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReftissuMarginId accountId = (ReftissuMarginId) o;
        return intervalId.equals(accountId.intervalId) &&
        		partNumberMaterialConfig.equals(accountId.partNumberMaterialConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intervalId, partNumberMaterialConfig);
    }
	
}
