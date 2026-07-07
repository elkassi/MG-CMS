package com.lear.splice.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "marker_log")
public class MarkerLog {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	@Column(name = "marker")
	private String marker;
	@Column(name = "fabric_type")
	private String fabricType;
	@Column(name = "marker_length_brut")
	private Double markerLengthBrut;
	@Column(name = "marker_length_net")
	private Double markerLengthNet;
	@Column(name = "fabric_type_defect_length")
	private Double fabricTypeDefectLength;
	@Column(name = "fabric_type_splice_length")
	private Double fabricTypeSpliceLength;
	@Column(name = "old_splice_length")
	private Integer oldSpliceLength;
	@Column(name = "splice_number")
	private Integer spliceNumber;
	@Column(name = "combined")
	private Integer combined;
	@Column(name = "number_of_layers_to_do")
	private Integer numberOfLayersToDo;
	@Column(name = "number_of_layers_done")
	private Integer numberOfLayersDone;
	@Column(name = "fabric_type_length")
	private Double fabricTypeLength;
	@Column(name = "state")
	private Integer state;
	@Column(name = "user_badge")
	private String userBadge;
	@Column(name = "user_name")
	private String userName;
	@Column(name = "station_number")
	private String stationNumber;
	@Column(name = "created_at")
	private LocalDateTime createdAt;
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	@Column(name = "sync")
	private Integer sync;
	@Column(name = "buffer")
	private Integer buffer;
	@Column(name = "order_code")
	private String orderCode;
	@Column(name = "end_of_roll_length")
	private Double endOfRollLength;
	@Column(name = "work_order_code")
	private String workOrderCode;
	@Column(name = "framing_marker")
	private String framingMarker;
	@Column(name = "fabric_type_good_part_length")
	private Integer fabricTypeGoodPartLength;
	@Column(name = "number_of_sets")
	private Integer numberOfSets;
	@Column(name = "number_of_invert_splice")
	private Integer numberOfInvertSplice;
	@Column(name = "material_roll_width")
	private Integer materialRollWidth;
	@Column(name = "marker_width_brut")
	private Integer markerWidthBrut;
	@Column(name = "fabric_type_splice_length_inversed")
	private Double fabricTypeSpliceLengthInversed;
	@Column(name = "longest_piece_length")
	private Integer longestPieceLength;
	@Column(name = "scanned_marker")
	private String scannedMarker;
	@Column(name = "mrk_drill1")
	private Integer mrkDrill1;
	@Column(name = "mrk_drill2")
	private Integer mrkDrill2;
	@Column(name = "start_margin_length")
	private Integer startMarginLength;
	@Column(name = "stop_margin_length")
	private Integer stopMarginLength;
	@Column(name = "initial_start_margin_length")
	private Integer initialStartMarginLength;
	@Column(name = "initial_stop_margin_length")
	private Integer initialStopMarginLength;
	@Column(name = "fabric_type_description")
	private String fabricTypeDescription;
	@Column(name = "fabric_type_category")
	private String fabricTypeCategory;
	@Column(name = "confirm_deny_cutting_badge")
	private String confirmDenyCuttingBadge;
	@Column(name = "confirm_deny_cutting_name")
	private String confirmDenyCuttingName;
	@Column(name = "manual_mode")
	private Integer manualMode;
	@Column(name = "standard_time_per_layer")
	private Integer standardTimePerLayer;
	@Column(name = "standard_time_per_roll")
	private Integer standardTimePerRoll;
	@Column(name = "break_time")
	private Integer breakTime;
	@Column(name = "critical_item_approved_by_badge")
	private String criticalItemApprovedByBadge;
	@Column(name = "critical_item_approved_by_name")
	private String criticalItemApprovedByName;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getMarker() {
		return marker;
	}
	public void setMarker(String marker) {
		this.marker = marker;
	}
	public String getFabricType() {
		return fabricType;
	}
	public void setFabricType(String fabricType) {
		this.fabricType = fabricType;
	}
	public Double getMarkerLengthBrut() {
		return markerLengthBrut;
	}
	public void setMarkerLengthBrut(Double markerLengthBrut) {
		this.markerLengthBrut = markerLengthBrut;
	}
	public Double getMarkerLengthNet() {
		return markerLengthNet;
	}
	public void setMarkerLengthNet(Double markerLengthNet) {
		this.markerLengthNet = markerLengthNet;
	}
	public Double getFabricTypeDefectLength() {
		return fabricTypeDefectLength;
	}
	public void setFabricTypeDefectLength(Double fabricTypeDefectLength) {
		this.fabricTypeDefectLength = fabricTypeDefectLength;
	}
	public Double getFabricTypeSpliceLength() {
		return fabricTypeSpliceLength;
	}
	public void setFabricTypeSpliceLength(Double fabricTypeSpliceLength) {
		this.fabricTypeSpliceLength = fabricTypeSpliceLength;
	}
	public Integer getOldSpliceLength() {
		return oldSpliceLength;
	}
	public void setOldSpliceLength(Integer oldSpliceLength) {
		this.oldSpliceLength = oldSpliceLength;
	}
	public Integer getSpliceNumber() {
		return spliceNumber;
	}
	public void setSpliceNumber(Integer spliceNumber) {
		this.spliceNumber = spliceNumber;
	}
	public Integer getCombined() {
		return combined;
	}
	public void setCombined(Integer combined) {
		this.combined = combined;
	}
	public Integer getNumberOfLayersToDo() {
		return numberOfLayersToDo;
	}
	public void setNumberOfLayersToDo(Integer numberOfLayersToDo) {
		this.numberOfLayersToDo = numberOfLayersToDo;
	}
	public Integer getNumberOfLayersDone() {
		return numberOfLayersDone;
	}
	public void setNumberOfLayersDone(Integer numberOfLayersDone) {
		this.numberOfLayersDone = numberOfLayersDone;
	}
	public Double getFabricTypeLength() {
		return fabricTypeLength;
	}
	public void setFabricTypeLength(Double fabricTypeLength) {
		this.fabricTypeLength = fabricTypeLength;
	}
	public Integer getState() {
		return state;
	}
	public void setState(Integer state) {
		this.state = state;
	}
	public String getUserBadge() {
		return userBadge;
	}
	public void setUserBadge(String userBadge) {
		this.userBadge = userBadge;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getStationNumber() {
		return stationNumber;
	}
	public void setStationNumber(String stationNumber) {
		this.stationNumber = stationNumber;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	public Integer getSync() {
		return sync;
	}
	public void setSync(Integer sync) {
		this.sync = sync;
	}
	public Integer getBuffer() {
		return buffer;
	}
	public void setBuffer(Integer buffer) {
		this.buffer = buffer;
	}
	public String getOrderCode() {
		return orderCode;
	}
	public void setOrderCode(String orderCode) {
		this.orderCode = orderCode;
	}
	public Double getEndOfRollLength() {
		return endOfRollLength;
	}
	public void setEndOfRollLength(Double endOfRollLength) {
		this.endOfRollLength = endOfRollLength;
	}
	public String getWorkOrderCode() {
		return workOrderCode;
	}
	public void setWorkOrderCode(String workOrderCode) {
		this.workOrderCode = workOrderCode;
	}
	public String getFramingMarker() {
		return framingMarker;
	}
	public void setFramingMarker(String framingMarker) {
		this.framingMarker = framingMarker;
	}
	public Integer getFabricTypeGoodPartLength() {
		return fabricTypeGoodPartLength;
	}
	public void setFabricTypeGoodPartLength(Integer fabricTypeGoodPartLength) {
		this.fabricTypeGoodPartLength = fabricTypeGoodPartLength;
	}
	public Integer getNumberOfSets() {
		return numberOfSets;
	}
	public void setNumberOfSets(Integer numberOfSets) {
		this.numberOfSets = numberOfSets;
	}
	public Integer getNumberOfInvertSplice() {
		return numberOfInvertSplice;
	}
	public void setNumberOfInvertSplice(Integer numberOfInvertSplice) {
		this.numberOfInvertSplice = numberOfInvertSplice;
	}
	public Integer getMaterialRollWidth() {
		return materialRollWidth;
	}
	public void setMaterialRollWidth(Integer materialRollWidth) {
		this.materialRollWidth = materialRollWidth;
	}
	public Integer getMarkerWidthBrut() {
		return markerWidthBrut;
	}
	public void setMarkerWidthBrut(Integer markerWidthBrut) {
		this.markerWidthBrut = markerWidthBrut;
	}
	public Double getFabricTypeSpliceLengthInversed() {
		return fabricTypeSpliceLengthInversed;
	}
	public void setFabricTypeSpliceLengthInversed(Double fabricTypeSpliceLengthInversed) {
		this.fabricTypeSpliceLengthInversed = fabricTypeSpliceLengthInversed;
	}
	public Integer getLongestPieceLength() {
		return longestPieceLength;
	}
	public void setLongestPieceLength(Integer longestPieceLength) {
		this.longestPieceLength = longestPieceLength;
	}
	public String getScannedMarker() {
		return scannedMarker;
	}
	public void setScannedMarker(String scannedMarker) {
		this.scannedMarker = scannedMarker;
	}
	public Integer getMrkDrill1() {
		return mrkDrill1;
	}
	public void setMrkDrill1(Integer mrkDrill1) {
		this.mrkDrill1 = mrkDrill1;
	}
	public Integer getMrkDrill2() {
		return mrkDrill2;
	}
	public void setMrkDrill2(Integer mrkDrill2) {
		this.mrkDrill2 = mrkDrill2;
	}
	public Integer getStartMarginLength() {
		return startMarginLength;
	}
	public void setStartMarginLength(Integer startMarginLength) {
		this.startMarginLength = startMarginLength;
	}
	public Integer getStopMarginLength() {
		return stopMarginLength;
	}
	public void setStopMarginLength(Integer stopMarginLength) {
		this.stopMarginLength = stopMarginLength;
	}
	public Integer getInitialStartMarginLength() {
		return initialStartMarginLength;
	}
	public void setInitialStartMarginLength(Integer initialStartMarginLength) {
		this.initialStartMarginLength = initialStartMarginLength;
	}
	public Integer getInitialStopMarginLength() {
		return initialStopMarginLength;
	}
	public void setInitialStopMarginLength(Integer initialStopMarginLength) {
		this.initialStopMarginLength = initialStopMarginLength;
	}
	public String getFabricTypeDescription() {
		return fabricTypeDescription;
	}
	public void setFabricTypeDescription(String fabricTypeDescription) {
		this.fabricTypeDescription = fabricTypeDescription;
	}
	public String getFabricTypeCategory() {
		return fabricTypeCategory;
	}
	public void setFabricTypeCategory(String fabricTypeCategory) {
		this.fabricTypeCategory = fabricTypeCategory;
	}
	public String getConfirmDenyCuttingBadge() {
		return confirmDenyCuttingBadge;
	}
	public void setConfirmDenyCuttingBadge(String confirmDenyCuttingBadge) {
		this.confirmDenyCuttingBadge = confirmDenyCuttingBadge;
	}
	public String getConfirmDenyCuttingName() {
		return confirmDenyCuttingName;
	}
	public void setConfirmDenyCuttingName(String confirmDenyCuttingName) {
		this.confirmDenyCuttingName = confirmDenyCuttingName;
	}
	public Integer getManualMode() {
		return manualMode;
	}
	public void setManualMode(Integer manualMode) {
		this.manualMode = manualMode;
	}
	public Integer getStandardTimePerLayer() {
		return standardTimePerLayer;
	}
	public void setStandardTimePerLayer(Integer standardTimePerLayer) {
		this.standardTimePerLayer = standardTimePerLayer;
	}
	public Integer getStandardTimePerRoll() {
		return standardTimePerRoll;
	}
	public void setStandardTimePerRoll(Integer standardTimePerRoll) {
		this.standardTimePerRoll = standardTimePerRoll;
	}
	public Integer getBreakTime() {
		return breakTime;
	}
	public void setBreakTime(Integer breakTime) {
		this.breakTime = breakTime;
	}
	public String getCriticalItemApprovedByBadge() {
		return criticalItemApprovedByBadge;
	}
	public void setCriticalItemApprovedByBadge(String criticalItemApprovedByBadge) {
		this.criticalItemApprovedByBadge = criticalItemApprovedByBadge;
	}
	public String getCriticalItemApprovedByName() {
		return criticalItemApprovedByName;
	}
	public void setCriticalItemApprovedByName(String criticalItemApprovedByName) {
		this.criticalItemApprovedByName = criticalItemApprovedByName;
	}
	
	
	
	
}
