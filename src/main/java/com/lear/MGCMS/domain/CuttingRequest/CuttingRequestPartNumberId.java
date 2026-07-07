package com.lear.MGCMS.domain.CuttingRequest;

import java.io.Serializable;
import java.util.Objects;

public class CuttingRequestPartNumberId implements Serializable {

	private String cuttingRequest;

	private String partNumber;


	public CuttingRequestPartNumberId(String cuttingRequest, String partNumber) {
		super();
		this.cuttingRequest = cuttingRequest;
		this.partNumber = partNumber;
	}

	public CuttingRequestPartNumberId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuttingRequestPartNumberId obj = (CuttingRequestPartNumberId) o;
        return cuttingRequest.equals(obj.cuttingRequest) &&
        		partNumber.equals(obj.partNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuttingRequest, partNumber);
    }
	
}
