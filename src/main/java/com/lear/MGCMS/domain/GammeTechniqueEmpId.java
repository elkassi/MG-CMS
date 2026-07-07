package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class GammeTechniqueEmpId implements Serializable {

	private String gammeTechnique;

	private String panelNumber;

	public GammeTechniqueEmpId(String gammeTechnique, String panelNumber) {
		super();
		this.gammeTechnique = gammeTechnique;
		this.panelNumber = panelNumber;
	}

	public GammeTechniqueEmpId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GammeTechniqueEmpId accountId = (GammeTechniqueEmpId) o;
        return gammeTechnique.equals(accountId.gammeTechnique) &&
        		panelNumber.equals(accountId.panelNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gammeTechnique, panelNumber);
    }

	
}

