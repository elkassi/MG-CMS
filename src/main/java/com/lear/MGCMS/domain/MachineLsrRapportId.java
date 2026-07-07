package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


public class MachineLsrRapportId implements Serializable {
	
	private String machine;
	private String label;
	private LocalDateTime date;

	public MachineLsrRapportId(String machine, String label, LocalDateTime date) {
		super();
		this.machine = machine;
		this.label = label;
		this.date = date;
	}

	public MachineLsrRapportId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MachineLsrRapportId obj = (MachineLsrRapportId) o;
        return machine.equals(obj.machine) &&
        		label.equals(obj.label) && 
        		date.equals(obj.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machine, label, date);
    }

	
}
