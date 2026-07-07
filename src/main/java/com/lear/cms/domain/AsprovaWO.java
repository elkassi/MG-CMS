package com.lear.cms.domain;

import javax.persistence.*;

@Entity
@Table(name = "Asprova_WO", schema = "dbo")
public class AsprovaWO {

    /*

   [ID_Table_Asprova_WO] int
      ,[ID_Order_Schedule] int
      ,[ID_ItemNumber_Asprova_WO] int
      ,[ItemNumber_Asprova_WO] varchar(500)
      ,[Marker_Group_ID_Asprova_WO] varchar(500)
      ,[ID_Asprova_Asprova_WO] varchar(500)
      ,[Qty_Asrpova_Asprova_WO] numeric(18, 5)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Table_Asprova_WO") private Integer idTableAsprovaWO;
    @Column(name = "ID_Order_Schedule") private Integer idOrderSchedule;
    @Column(name = "ID_ItemNumber_Asprova_WO") private Integer idItemNumberAsprovaWO;
    @Column(name = "ItemNumber_Asprova_WO") private String itemNumberAsprovaWO;
    @Column(name = "Marker_Group_ID_Asprova_WO") private String markerGroupIdAsprovaWO;
    @Column(name = "ID_Asprova_Asprova_WO") private String idAsprovaAsprovaWO;
    @Column(name = "Qty_Asrpova_Asprova_WO") private Double qtyAsrpovaAsprovaWO;

    public Integer getIdTableAsprovaWO() {
        return idTableAsprovaWO;
    }

    public void setIdTableAsprovaWO(Integer idTableAsprovaWO) {
        this.idTableAsprovaWO = idTableAsprovaWO;
    }

    public Integer getIdOrderSchedule() {
        return idOrderSchedule;
    }

    public void setIdOrderSchedule(Integer idOrderSchedule) {
        this.idOrderSchedule = idOrderSchedule;
    }

    public Integer getIdItemNumberAsprovaWO() {
        return idItemNumberAsprovaWO;
    }

    public void setIdItemNumberAsprovaWO(Integer idItemNumberAsprovaWO) {
        this.idItemNumberAsprovaWO = idItemNumberAsprovaWO;
    }

    public String getItemNumberAsprovaWO() {
        return itemNumberAsprovaWO;
    }

    public void setItemNumberAsprovaWO(String itemNumberAsprovaWO) {
        this.itemNumberAsprovaWO = itemNumberAsprovaWO;
    }

    public String getMarkerGroupIdAsprovaWO() {
        return markerGroupIdAsprovaWO;
    }

    public void setMarkerGroupIdAsprovaWO(String markerGroupIdAsprovaWO) {
        this.markerGroupIdAsprovaWO = markerGroupIdAsprovaWO;
    }

    public String getIdAsprovaAsprovaWO() {
        return idAsprovaAsprovaWO;
    }

    public void setIdAsprovaAsprovaWO(String idAsprovaAsprovaWO) {
        this.idAsprovaAsprovaWO = idAsprovaAsprovaWO;
    }

    public Double getQtyAsrpovaAsprovaWO() {
        return qtyAsrpovaAsprovaWO;
    }

    public void setQtyAsrpovaAsprovaWO(Double qtyAsrpovaAsprovaWO) {
        this.qtyAsrpovaAsprovaWO = qtyAsrpovaAsprovaWO;
    }
}
