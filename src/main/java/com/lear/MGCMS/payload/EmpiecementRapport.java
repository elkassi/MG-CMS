package com.lear.MGCMS.payload;

import java.time.LocalDateTime;

public class EmpiecementRapport {

    private String folder;
    private String placement;
    private String reftissu;
    private String partnumber;
    private LocalDateTime date;
    private Long cuttingPlanId;
    private Integer drillCMS1;
    private Integer drillCMS2;


    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public String getReftissu() {
        return reftissu;
    }

    public void setReftissu(String reftissu) {
        this.reftissu = reftissu;
    }

    public String getPartnumber() {
        return partnumber;
    }

    public void setPartnumber(String partnumber) {
        this.partnumber = partnumber;
    }


    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getCuttingPlanId() {
        return cuttingPlanId;
    }

    public void setCuttingPlanId(Long cuttingPlanId) {
        this.cuttingPlanId = cuttingPlanId;
    }

    public Integer getDrillCMS1() {
        return drillCMS1;
    }

    public void setDrillCMS1(Integer drillCMS1) {
        this.drillCMS1 = drillCMS1;
    }

    public Integer getDrillCMS2() {
        return drillCMS2;
    }

    public void setDrillCMS2(Integer drillCMS2) {
        this.drillCMS2 = drillCMS2;
    }
}
