package com.lear.MGCMS.domain.CuttingPlan;

import java.io.Serializable;
import java.util.Objects;

public class CuttingPlanMaterialId implements Serializable {

	private Long cuttingPlan;

	private String partNumberMaterial;

	public CuttingPlanMaterialId(Long cuttingPlan, String PartNumberMaterial) {
		super();
		this.cuttingPlan = cuttingPlan;
		this.partNumberMaterial = PartNumberMaterial;
	}

	public CuttingPlanMaterialId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingPlanMaterialId accountId = (CuttingPlanMaterialId) o;
        return cuttingPlan.equals(accountId.cuttingPlan) &&
        		partNumberMaterial.equals(accountId.partNumberMaterial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuttingPlan, partNumberMaterial);
    }

	
}
