package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class CoupeMachineHistoryId implements Serializable {
	
	private String machine;

	private String fileReport;
	
	private Integer ind;


	public CoupeMachineHistoryId(String machine, String fileReport, Integer ind) {
		super();
		this.machine = machine;
		this.fileReport = fileReport;
		this.ind = ind;
	}

	public CoupeMachineHistoryId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoupeMachineHistoryId obj = (CoupeMachineHistoryId) o;
        return machine.equals(obj.machine) &&
        		fileReport.equals(obj.fileReport) && 
        		ind == obj.ind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(machine, fileReport, ind);
    }


}
