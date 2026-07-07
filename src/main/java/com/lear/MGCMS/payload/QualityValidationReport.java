package com.lear.MGCMS.payload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class QualityValidationReport {
    /*
    SELECT TOP (1000) qv.serie, qv.date, qv.machine, qv.reftissu, crs.dateDebutCoupe, crs.tableCoupe
  FROM [dbo].QualityValidationHistory as qv
  JOIN  [dbo].CuttingRequestSerie as crs on crs.serie = qv.serie
  where dateDebutCoupe >= '2024-12-21 12:05:50.8900000' and dateDebutCoupe <= '2024-12-21 12:15:50.8900000'
  order by crs.dateDebutCoupe desc
     */

    private String serie;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime date;
    private String machine;
    private String reftissu;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime dateDebutCoupe;
    private String tableCoupe;

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getReftissu() {
        return reftissu;
    }

    public void setReftissu(String reftissu) {
        this.reftissu = reftissu;
    }

    public LocalDateTime getDateDebutCoupe() {
        return dateDebutCoupe;
    }

    public void setDateDebutCoupe(LocalDateTime dateDebutCoupe) {
        this.dateDebutCoupe = dateDebutCoupe;
    }

    public String getTableCoupe() {
        return tableCoupe;
    }

    public void setTableCoupe(String tableCoupe) {
        this.tableCoupe = tableCoupe;
    }
}
