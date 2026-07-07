package com.lear.cms.domain;

import javax.persistence.*;
/*
     [ID_Timing_Model]
      ,[ID_PlanCoupe_Timing_Model]
      ,[ID_Spreading_Timing_Model]
      ,[ItemNumber_Timing_Model]
      ,[Description_ItemNumber_Timing_Model]
      ,[Placement_Timing_Model]
      ,[TypeItem_Timing_Model]
      ,[Perimeter_Timing_Model]
      ,[Qty_Plan_Timing_Model]
      ,[Qty_Per_Layer_Timing_Model]
      ,[Max_Plie_Timing_Model]
      ,[Machine_Timing_Model]
      ,[Layers_Timing_Model]
      ,[Longueur_Placement_Timing_Model]
      ,[Seuil_Longueur_Timing_Model]
      ,[Longueur_Matelas_Timing_Model]
      ,[Speed_m_min_Timing_Model]
      ,[Drilling_Misc_Timing_Model]
      ,[Prep_time_Min_Timing_Model]
      ,[Cutting_time_Stopper_Perlayer_Timing_Model]
      ,[Spread_time_PerLayer_m_min_Timing_Model]
      ,[Cutting_time_Timing_Model]
      ,[Spreading_Timing_Model]
      ,[Real_Cutting_time_Timing_Model]
      ,[Validated_Cutting_time_Timing_Model]
     */
@Entity
@Table(name = "Timing_Model")
public class TimingModel {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Timing_Model") private Long idTimingModel;
    @Column(name = "ID_PlanCoupe_Timing_Model") private Long idPlanCoupeTimingModel;
    @Column(name = "ID_Spreading_Timing_Model") private Long idSpreadingTimingModel;
    @Column(name = "ItemNumber_Timing_Model") private String itemNumberTimingModel;
    @Column(name = "Description_ItemNumber_Timing_Model") private String descriptionItemNumberTimingModel;
    @Column(name = "Placement_Timing_Model") private String placementTimingModel;
    @Column(name = "TypeItem_Timing_Model") private String typeItemTimingModel;
    @Column(name = "Perimeter_Timing_Model") private Double perimeterTimingModel;
    @Column(name = "Qty_Plan_Timing_Model") private Integer qtyPlanTimingModel;
    @Column(name = "Qty_Per_Layer_Timing_Model") private Integer qtyPerLayerTimingModel;
    @Column(name = "Max_Plie_Timing_Model") private Integer maxPlieTimingModel;
    @Column(name = "Machine_Timing_Model") private String machineTimingModel;
    @Column(name = "Layers_Timing_Model") private Integer layersTimingModel;
    @Column(name = "Longueur_Placement_Timing_Model") private Double longueurPlacementTimingModel;
    @Column(name = "Seuil_Longueur_Timing_Model") private Double seuilLongueurTimingModel;
    @Column(name = "Longueur_Matelas_Timing_Model") private Double longueurMatelasTimingModel;
    @Column(name = "Speed_m_min_Timing_Model") private Double speedMMinTimingModel;
    @Column(name = "Drilling_Misc_Timing_Model") private Double drillingMiscTimingModel;
    @Column(name = "Prep_time_Min_Timing_Model") private Double prepTimeMinTimingModel;
    @Column(name = "Cutting_time_Stopper_Perlayer_Timing_Model") private Double cuttingTimeStopperPerlayerTimingModel;
    @Column(name = "Spread_time_PerLayer_m_min_Timing_Model") private Double spreadTimePerLayerMMinTimingModel;
    //      ,[Cutting_time_Timing_Model]
    //      ,[Spreading_Timing_Model]
    @Transient
    @Column(name = "Cutting_time_Timing_Model") private Double cuttingTimeTimingModel;
    @Transient
    @Column(name = "Spreading_Timing_Model") private Double spreadingTimingModel;
    @Column(name = "Real_Cutting_time_Timing_Model") private Double realCuttingtimeTimingModel;
    @Column(name = "Validated_Cutting_time_Timing_Model") private Double validatedCuttingtimeTimingModel;

    public Double getCuttingTimeTimingModel() {
        return cuttingTimeTimingModel;
    }

    public void setCuttingTimeTimingModel(Double cuttingTimeTimingModel) {
        this.cuttingTimeTimingModel = cuttingTimeTimingModel;
    }

    public Double getSpreadingTimingModel() {
        return spreadingTimingModel;
    }

    public void setSpreadingTimingModel(Double spreadingTimingModel) {
        this.spreadingTimingModel = spreadingTimingModel;
    }

    public Double getRealCuttingtimeTimingModel() {
        return realCuttingtimeTimingModel;
    }

    public void setRealCuttingtimeTimingModel(Double realCuttingtimeTimingModel) {
        this.realCuttingtimeTimingModel = realCuttingtimeTimingModel;
    }

    public Double getValidatedCuttingtimeTimingModel() {
        return validatedCuttingtimeTimingModel;
    }

    public void setValidatedCuttingtimeTimingModel(Double validatedCuttingtimeTimingModel) {
        this.validatedCuttingtimeTimingModel = validatedCuttingtimeTimingModel;
    }

    public Long getIdTimingModel() {
        return idTimingModel;
    }

    public void setIdTimingModel(Long idTimingModel) {
        this.idTimingModel = idTimingModel;
    }

    public Long getIdPlanCoupeTimingModel() {
        return idPlanCoupeTimingModel;
    }

    public void setIdPlanCoupeTimingModel(Long idPlanCoupeTimingModel) {
        this.idPlanCoupeTimingModel = idPlanCoupeTimingModel;
    }

    public Long getIdSpreadingTimingModel() {
        return idSpreadingTimingModel;
    }

    public void setIdSpreadingTimingModel(Long idSpreadingTimingModel) {
        this.idSpreadingTimingModel = idSpreadingTimingModel;
    }

    public String getItemNumberTimingModel() {
        return itemNumberTimingModel;
    }

    public void setItemNumberTimingModel(String itemNumberTimingModel) {
        this.itemNumberTimingModel = itemNumberTimingModel;
    }

    public String getDescriptionItemNumberTimingModel() {
        return descriptionItemNumberTimingModel;
    }

    public void setDescriptionItemNumberTimingModel(String descriptionItemNumberTimingModel) {
        this.descriptionItemNumberTimingModel = descriptionItemNumberTimingModel;
    }

    public String getPlacementTimingModel() {
        return placementTimingModel;
    }

    public void setPlacementTimingModel(String placementTimingModel) {
        this.placementTimingModel = placementTimingModel;
    }

    public String getTypeItemTimingModel() {
        return typeItemTimingModel;
    }

    public void setTypeItemTimingModel(String typeItemTimingModel) {
        this.typeItemTimingModel = typeItemTimingModel;
    }

    public Double getPerimeterTimingModel() {
        return perimeterTimingModel;
    }

    public void setPerimeterTimingModel(Double perimeterTimingModel) {
        this.perimeterTimingModel = perimeterTimingModel;
    }

    public Integer getQtyPlanTimingModel() {
        return qtyPlanTimingModel;
    }

    public void setQtyPlanTimingModel(Integer qtyPlanTimingModel) {
        this.qtyPlanTimingModel = qtyPlanTimingModel;
    }

    public Integer getQtyPerLayerTimingModel() {
        return qtyPerLayerTimingModel;
    }

    public void setQtyPerLayerTimingModel(Integer qtyPerLayerTimingModel) {
        this.qtyPerLayerTimingModel = qtyPerLayerTimingModel;
    }

    public Integer getMaxPlieTimingModel() {
        return maxPlieTimingModel;
    }

    public void setMaxPlieTimingModel(Integer maxPlieTimingModel) {
        this.maxPlieTimingModel = maxPlieTimingModel;
    }

    public String getMachineTimingModel() {
        return machineTimingModel;
    }

    public void setMachineTimingModel(String machineTimingModel) {
        this.machineTimingModel = machineTimingModel;
    }

    public Integer getLayersTimingModel() {
        return layersTimingModel;
    }

    public void setLayersTimingModel(Integer layersTimingModel) {
        this.layersTimingModel = layersTimingModel;
    }

    public Double getLongueurPlacementTimingModel() {
        return longueurPlacementTimingModel;
    }

    public void setLongueurPlacementTimingModel(Double longueurPlacementTimingModel) {
        this.longueurPlacementTimingModel = longueurPlacementTimingModel;
    }

    public Double getSeuilLongueurTimingModel() {
        return seuilLongueurTimingModel;
    }

    public void setSeuilLongueurTimingModel(Double seuilLongueurTimingModel) {
        this.seuilLongueurTimingModel = seuilLongueurTimingModel;
    }

    public Double getLongueurMatelasTimingModel() {
        return longueurMatelasTimingModel;
    }

    public void setLongueurMatelasTimingModel(Double longueurMatelasTimingModel) {
        this.longueurMatelasTimingModel = longueurMatelasTimingModel;
    }

    public Double getSpeedMMinTimingModel() {
        return speedMMinTimingModel;
    }

    public void setSpeedMMinTimingModel(Double speedMMinTimingModel) {
        this.speedMMinTimingModel = speedMMinTimingModel;
    }

    public Double getDrillingMiscTimingModel() {
        return drillingMiscTimingModel;
    }

    public void setDrillingMiscTimingModel(Double drillingMiscTimingModel) {
        this.drillingMiscTimingModel = drillingMiscTimingModel;
    }

    public Double getPrepTimeMinTimingModel() {
        return prepTimeMinTimingModel;
    }

    public void setPrepTimeMinTimingModel(Double prepTimeMinTimingModel) {
        this.prepTimeMinTimingModel = prepTimeMinTimingModel;
    }

    public Double getCuttingTimeStopperPerlayerTimingModel() {
        return cuttingTimeStopperPerlayerTimingModel;
    }

    public void setCuttingTimeStopperPerlayerTimingModel(Double cuttingTimeStopperPerlayerTimingModel) {
        this.cuttingTimeStopperPerlayerTimingModel = cuttingTimeStopperPerlayerTimingModel;
    }

    public Double getSpreadTimePerLayerMMinTimingModel() {
        return spreadTimePerLayerMMinTimingModel;
    }

    public void setSpreadTimePerLayerMMinTimingModel(Double spreadTimePerLayerMMinTimingModel) {
        this.spreadTimePerLayerMMinTimingModel = spreadTimePerLayerMMinTimingModel;
    }

}
