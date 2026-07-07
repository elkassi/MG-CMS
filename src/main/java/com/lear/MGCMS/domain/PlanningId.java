package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Id;

public class PlanningId implements Serializable {
	
	private LocalDate planningDate; 
	private String shift;
	private String partNumber;
	public PlanningId(LocalDate planningDate, String shift, String partNumber) {
		super();
		this.planningDate = planningDate;
		this.shift = shift;
		this.partNumber = partNumber;
	}
	
	public PlanningId() {
		super();
	}

	public LocalDate getPlanningDate() {
		return planningDate;
	}
	public void setPlanningDate(LocalDate planningDate) {
		this.planningDate = planningDate;
	}
	public String getShift() {
		return shift;
	}
	public void setShift(String shift) {
		this.shift = shift;
	}
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanningId obj = (PlanningId) o;
        return Objects.equals(planningDate, obj.getPlanningDate()) &&
        		Objects.equals(shift, obj.getShift()) &&
        		Objects.equals(partNumber, obj.getPartNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(planningDate, shift, partNumber);
    }
	

}
