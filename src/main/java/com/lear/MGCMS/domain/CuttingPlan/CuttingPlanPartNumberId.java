package com.lear.MGCMS.domain.CuttingPlan;

import java.io.Serializable;
import java.util.Objects;

public class CuttingPlanPartNumberId implements Serializable {

	private Long cuttingPlan;

	private String partNumber;

	public CuttingPlanPartNumberId(Long cuttingPlan, String partNumber) {
		super();
		this.cuttingPlan = cuttingPlan;
		this.partNumber = partNumber;
	}

	public CuttingPlanPartNumberId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingPlanPartNumberId accountId = (CuttingPlanPartNumberId) o;
        return cuttingPlan.equals(accountId.cuttingPlan) &&
        		partNumber.equals(accountId.partNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuttingPlan, partNumber);
    }

	
}
