package com.lear.MGCMS.domain.CuttingPlan.data;

import java.util.Objects;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanPartNumberId;

public class CuttingPlanPartNumberDataId {
	
	private Long cuttingPlan;

	private String partNumber;

	public CuttingPlanPartNumberDataId(Long cuttingPlan, String partNumber) {
		super();
		this.cuttingPlan = cuttingPlan;
		this.partNumber = partNumber;
	}

	public CuttingPlanPartNumberDataId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingPlanPartNumberDataId accountId = (CuttingPlanPartNumberDataId) o;
        return cuttingPlan.equals(accountId.cuttingPlan) &&
        		partNumber.equals(accountId.partNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuttingPlan, partNumber);
    }


}
