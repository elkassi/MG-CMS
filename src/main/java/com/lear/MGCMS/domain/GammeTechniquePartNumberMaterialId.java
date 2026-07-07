package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class GammeTechniquePartNumberMaterialId implements Serializable {

	private String gammeTechnique;

	private String partNumberMaterial;

	public GammeTechniquePartNumberMaterialId(String gammeTechnique, String PartNumberMaterial) {
		super();
		this.gammeTechnique = gammeTechnique;
		this.partNumberMaterial = PartNumberMaterial;
	}

	public GammeTechniquePartNumberMaterialId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GammeTechniquePartNumberMaterialId accountId = (GammeTechniquePartNumberMaterialId) o;
        return gammeTechnique.equals(accountId.gammeTechnique) &&
        		partNumberMaterial.equals(accountId.partNumberMaterial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gammeTechnique, partNumberMaterial);
    }

	
}

