package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class CoupePerformanceId implements Serializable {
    private String machine;
    private String placement;
    private LocalDateTime dateDebut;

    public CoupePerformanceId(String machine, String placement, LocalDateTime dateDebut) {
        super();
        this.machine = machine;
        this.placement = placement;
        this.dateDebut = dateDebut;
    }

    public CoupePerformanceId() {
        super();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoupePerformanceId obj = (CoupePerformanceId) o;
        return machine.equals(obj.machine) &&
                placement.equals(obj.placement) &&
                dateDebut.equals(obj.dateDebut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machine, placement, dateDebut);
    }


}
