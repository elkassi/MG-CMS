package com.lear.MGCMS.domain.CuttingPlan;

import java.io.Serializable;
import java.util.Objects;

public class CuttingPlanCombinationPartNumberId implements Serializable {

	private Long cuttingPlanCombination;

	private String partNumber;

	public CuttingPlanCombinationPartNumberId(Long cuttingPlanCombination, String partNumber) {
		super();
		this.cuttingPlanCombination = cuttingPlanCombination;
		this.partNumber = partNumber;
	}

	public CuttingPlanCombinationPartNumberId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingPlanCombinationPartNumberId accountId = (CuttingPlanCombinationPartNumberId) o;
        return cuttingPlanCombination.equals(accountId.cuttingPlanCombination) &&
        		partNumber.equals(accountId.partNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuttingPlanCombination, partNumber);
    }
}
