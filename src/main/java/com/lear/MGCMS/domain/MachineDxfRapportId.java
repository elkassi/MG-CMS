package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class MachineDxfRapportId implements Serializable {

    private Integer processID;

    private String machineName;

    public MachineDxfRapportId(Integer processID, String machineName) {
        super();
        this.processID = processID;
        this.machineName = machineName;
    }

    public MachineDxfRapportId() {
        super();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MachineDxfRapportId accountId = (MachineDxfRapportId) o;
        return processID.equals(accountId.processID) &&
                machineName.equals(accountId.machineName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processID, machineName);
    }

}
