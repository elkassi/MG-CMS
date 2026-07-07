package com.lear.MGCMS.domain.CuttingPlan.data;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Id;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterial;
import com.lear.MGCMS.domain.CuttingPlan.CuttingPlanMaterialPlacementId;

public class CuttingPlanMaterialPlacementDataId implements Serializable{
	
	private String placement;
	private Long cuttingPlan;
	private String partNumberMaterial;

	public CuttingPlanMaterialPlacementDataId(String placement, Long cuttingPlan, String partNumberMaterial) {
		super();
		this.placement = placement;
		this.cuttingPlan = cuttingPlan;
		this.partNumberMaterial = partNumberMaterial;
	}

	public CuttingPlanMaterialPlacementDataId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingPlanMaterialPlacementDataId accountId = (CuttingPlanMaterialPlacementDataId) o;
        return placement.equals(accountId.placement) &&
        		cuttingPlan.equals(accountId.cuttingPlan) &&
        		partNumberMaterial.equals(accountId.partNumberMaterial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placement, cuttingPlan, partNumberMaterial);
    }
}
