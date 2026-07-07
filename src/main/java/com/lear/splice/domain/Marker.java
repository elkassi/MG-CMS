package com.lear.splice.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "marker")
public class Marker {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	@Column(name = "marker")
	private String marker;
	@Column(name = "fabric_type")
	private String fabricType;
	@Column(name = "marker_length_net")
	private Integer markerLengthNet;
	@Column(name = "marker_length_brut")
	private Integer markerLengthBrut;
	@Column(name = "number_of_layers")
	private Integer numberOfLayers;
	@Column(name = "created_at")
	private LocalDateTime createdAt;
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	@Column(name = "old_splice_length")
	private Integer oldSpliceLength;
	@Column(name = "drill1")
	private String drill1;
	@Column(name = "drill2")
	private String drill2;
	@Column(name = "number_of_sets")
	private Integer numberOfSets;
	@Column(name = "marker_width_brut")
	private Integer markerWidthBrut;
	@Column(name = "fabric_type_description")
	private String fabricTypeDescription;
	@Column(name = "number_of_piece_per_layer")
	private String numberOfPiecePerLayer;
	@Column(name = "check_material")
	private Integer checkMaterial;
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
	public Integer getMarkerLengthNet() {
		return markerLengthNet;
	}
	public void setMarkerLengthNet(Integer markerLengthNet) {
		this.markerLengthNet = markerLengthNet;
	}
	public Integer getMarkerLengthBrut() {
		return markerLengthBrut;
	}
	public void setMarkerLengthBrut(Integer markerLengthBrut) {
		this.markerLengthBrut = markerLengthBrut;
	}
	public Integer getNumberOfLayers() {
		return numberOfLayers;
	}
	public void setNumberOfLayers(Integer numberOfLayers) {
		this.numberOfLayers = numberOfLayers;
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
	public Integer getOldSpliceLength() {
		return oldSpliceLength;
	}
	public void setOldSpliceLength(Integer oldSpliceLength) {
		this.oldSpliceLength = oldSpliceLength;
	}
	public String getDrill1() {
		return drill1;
	}
	public void setDrill1(String drill1) {
		this.drill1 = drill1;
	}
	public String getDrill2() {
		return drill2;
	}
	public void setDrill2(String drill2) {
		this.drill2 = drill2;
	}
	public Integer getNumberOfSets() {
		return numberOfSets;
	}
	public void setNumberOfSets(Integer numberOfSets) {
		this.numberOfSets = numberOfSets;
	}
	public Integer getMarkerWidthBrut() {
		return markerWidthBrut;
	}
	public void setMarkerWidthBrut(Integer markerWidthBrut) {
		this.markerWidthBrut = markerWidthBrut;
	}
	public String getFabricTypeDescription() {
		return fabricTypeDescription;
	}
	public void setFabricTypeDescription(String fabricTypeDescription) {
		this.fabricTypeDescription = fabricTypeDescription;
	}
	public String getNumberOfPiecePerLayer() {
		return numberOfPiecePerLayer;
	}
	public void setNumberOfPiecePerLayer(String numberOfPiecePerLayer) {
		this.numberOfPiecePerLayer = numberOfPiecePerLayer;
	}
	public Integer getCheckMaterial() {
		return checkMaterial;
	}
	public void setCheckMaterial(Integer checkMaterial) {
		this.checkMaterial = checkMaterial;
	}
	
	
		
}
