package com.lear.MGCMS.domain.CuttingPlan;

import java.io.Serializable;
import java.util.Objects;

public class CuttingPlanMaterialPlacementId implements Serializable{
	
	private CuttingPlanMaterial cuttingPlanMaterial;
	
	private String placement;

	public CuttingPlanMaterialPlacementId(CuttingPlanMaterial cuttingPlanMaterial, String placement) {
		super();
		this.cuttingPlanMaterial = cuttingPlanMaterial;
		this.placement = placement;
	}

	public CuttingPlanMaterialPlacementId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingPlanMaterialPlacementId accountId = (CuttingPlanMaterialPlacementId) o;
        return cuttingPlanMaterial.getPartNumberMaterial().equals(accountId.cuttingPlanMaterial.getPartNumberMaterial()) &&
        		cuttingPlanMaterial.getCuttingPlan().getId().equals(accountId.cuttingPlanMaterial.getCuttingPlan().getId()) &&
        		placement.equals(accountId.placement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuttingPlanMaterial.getPartNumberMaterial(), cuttingPlanMaterial.getCuttingPlan().getId(),placement);
    }

}
