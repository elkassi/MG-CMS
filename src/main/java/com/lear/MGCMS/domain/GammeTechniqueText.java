package com.lear.MGCMS.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(
	name = "GammeTechniqueText",
	indexes = {
		@Index(name = "ix_GammeTechniqueText_partNumber", columnList = "partNumber, applyToPattern"),
		@Index(name = "ix_GammeTechniqueText_pattern", columnList = "pattern, applyToPattern"),
		@Index(name = "ix_GammeTechniqueText_panelNumber", columnList = "panelNumber")
	}
)
public class GammeTechniqueText {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 128)
	private String partNumber;

	@Column(length = 128)
	private String panelNumber;

	@Column(length = 128)
	private String partNumberMaterial;

	@Column(length = 128)
	private String pattern;

	@Column(columnDefinition = "NVARCHAR(512)", nullable = false)
	private String content;

	private Double labelX;
	private Double labelY;
	private Double labelSize;

	@Column(length = 64)
	private String fontFamily;

	@Column(length = 32)
	private String fontWeight;

	@Column(length = 32)
	private String fontStyle;

	@Column(length = 32)
	private String fillColor;

	private Integer rotation;

	@Column(nullable = false)
	private Boolean applyToPattern = false;

	@Column(length = 64)
	private String createdBy;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;

	@Column(length = 64)
	private String updatedBy;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		applyDefaults();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
		applyDefaults();
	}

	private void applyDefaults() {
		if (applyToPattern == null) applyToPattern = false;
		if (labelX == null) labelX = 0D;
		if (labelY == null) labelY = 0D;
		if (labelSize == null) labelSize = 24D;
		if (fontFamily == null || fontFamily.trim().isEmpty()) fontFamily = "Arial";
		if (fontWeight == null || fontWeight.trim().isEmpty()) fontWeight = "900";
		if (fontStyle == null || fontStyle.trim().isEmpty()) fontStyle = "normal";
		if (fillColor == null || fillColor.trim().isEmpty()) fillColor = "#ff0000";
		if (rotation == null) rotation = 0;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

	public String getPanelNumber() {
		return panelNumber;
	}

	public void setPanelNumber(String panelNumber) {
		this.panelNumber = panelNumber;
	}

	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}

	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Double getLabelX() {
		return labelX;
	}

	public void setLabelX(Double labelX) {
		this.labelX = labelX;
	}

	public Double getLabelY() {
		return labelY;
	}

	public void setLabelY(Double labelY) {
		this.labelY = labelY;
	}

	public Double getLabelSize() {
		return labelSize;
	}

	public void setLabelSize(Double labelSize) {
		this.labelSize = labelSize;
	}

	public String getFontFamily() {
		return fontFamily;
	}

	public void setFontFamily(String fontFamily) {
		this.fontFamily = fontFamily;
	}

	public String getFontWeight() {
		return fontWeight;
	}

	public void setFontWeight(String fontWeight) {
		this.fontWeight = fontWeight;
	}

	public String getFontStyle() {
		return fontStyle;
	}

	public void setFontStyle(String fontStyle) {
		this.fontStyle = fontStyle;
	}

	public String getFillColor() {
		return fillColor;
	}

	public void setFillColor(String fillColor) {
		this.fillColor = fillColor;
	}

	public Integer getRotation() {
		return rotation;
	}

	public void setRotation(Integer rotation) {
		this.rotation = rotation;
	}

	public Boolean getApplyToPattern() {
		return applyToPattern;
	}

	public void setApplyToPattern(Boolean applyToPattern) {
		this.applyToPattern = applyToPattern;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
