package com.lear.MGCMS.domain.CuttingPlan;

import java.io.Serializable;
import java.util.Objects;

public class CuttingPlanMaterialPlacementInfoId implements Serializable{
	
	private String cuttingPlanMaterial;
	private Long cuttingPlan;
	private String placement;

	public CuttingPlanMaterialPlacementInfoId(String cuttingPlanMaterial, Long cuttingPlan, String placement) {
		super();
		this.cuttingPlanMaterial = cuttingPlanMaterial;
		this.cuttingPlan = cuttingPlan;
		this.placement = placement;
	}

	public CuttingPlanMaterialPlacementInfoId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingPlanMaterialPlacementInfoId accountId = (CuttingPlanMaterialPlacementInfoId) o;
        return cuttingPlanMaterial.equals(accountId.cuttingPlanMaterial) &&
        		cuttingPlan.equals(accountId.cuttingPlan) &&
        		placement.equals(accountId.placement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuttingPlanMaterial, cuttingPlan ,placement);
    }

}
