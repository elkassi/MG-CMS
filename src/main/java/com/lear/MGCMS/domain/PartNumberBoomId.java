package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class PartNumberBoomId implements Serializable {

	private String partNumber;

	private String partNumberMaterial;

	public PartNumberBoomId(String partNumber, String partNumberMaterial) {
		super();
		this.partNumber = partNumber;
		this.partNumberMaterial = partNumberMaterial;
	}

	public PartNumberBoomId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartNumberBoomId accountId = (PartNumberBoomId) o;
        return partNumber.equals(accountId.partNumber) &&
        		partNumberMaterial.equals(accountId.partNumberMaterial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partNumber, partNumberMaterial);
    }
	
}
