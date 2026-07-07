package com.lear.MGCMS.payload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class RapportExcess {

    private String lotFrs;
    private Double excess;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime minDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime maxDate;

    public String getLotFrs() {
        return lotFrs;
    }

    public void setLotFrs(String lotFrs) {
        this.lotFrs = lotFrs;
    }

    public Double getExcess() {
        return excess;
    }

    public void setExcess(Double excess) {
        this.excess = excess;
    }

    public LocalDateTime getMinDate() {
        return minDate;
    }

    public void setMinDate(LocalDateTime minDate) {
        this.minDate = minDate;
    }

    public LocalDateTime getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(LocalDateTime maxDate) {
        this.maxDate = maxDate;
    }
}
