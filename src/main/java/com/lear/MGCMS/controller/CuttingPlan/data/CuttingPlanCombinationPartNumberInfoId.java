package com.lear.MGCMS.controller.CuttingPlan.data;

import java.io.Serializable;
import java.util.Objects;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanCombinationPartNumberId;

public class CuttingPlanCombinationPartNumberInfoId  implements Serializable {

	private Long cuttingPlanCombination;

	private String partNumber;

	public CuttingPlanCombinationPartNumberInfoId(Long cuttingPlanCombination, String partNumber) {
		super();
		this.cuttingPlanCombination = cuttingPlanCombination;
		this.partNumber = partNumber;
	}

	public CuttingPlanCombinationPartNumberInfoId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingPlanCombinationPartNumberInfoId accountId = (CuttingPlanCombinationPartNumberInfoId) o;
        return cuttingPlanCombination.equals(accountId.cuttingPlanCombination) &&
        		partNumber.equals(accountId.partNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuttingPlanCombination, partNumber);
    }
}
