package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class ReftissuMachineId implements Serializable {

	private String machineType;

	private String partNumberMaterialConfig;

	public ReftissuMachineId(String machineType, String partNumberMaterialConfig) {
		super();
		this.machineType = machineType;
		this.partNumberMaterialConfig = partNumberMaterialConfig;
	}

	public ReftissuMachineId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReftissuMachineId accountId = (ReftissuMachineId) o;
        return machineType.equals(accountId.machineType) &&
        		partNumberMaterialConfig.equals(accountId.partNumberMaterialConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineType, partNumberMaterialConfig);
    }

	
}
