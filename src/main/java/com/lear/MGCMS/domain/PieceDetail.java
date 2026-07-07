package com.lear.MGCMS.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "PieceDetail")
public class PieceDetail {

	@Id
	private String pieceName;

	private String descrip;
	private String category;
	private String comment;
	private String ruleTable;
	private Integer byteSize;
	private Double area;
	private Double totalArea;
	private Double perimeter;
	private Double baseSize;
	private Double smallestSize;
	private Integer numInt;
	private Integer numNch;
	private Integer numGp;
	private Integer numCrn;
	private Double pieceX;
	private Double pieceY;
	private String shrinkStretchX;
	private String shrinkStretchY;
	private String fabricCode;
	private String date;
	private String userLastMod;
	private String createdTime;
	private String userCreated;
	private String prevModTime;
	private String userPrevMod;

	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime importedAt;

	private String importedBy;

	public PieceDetail() {
		super();
	}

	public String getPieceName() {
		return pieceName;
	}

	public void setPieceName(String pieceName) {
		this.pieceName = pieceName;
	}

	public String getDescrip() {
		return descrip;
	}

	public void setDescrip(String descrip) {
		this.descrip = descrip;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getRuleTable() {
		return ruleTable;
	}

	public void setRuleTable(String ruleTable) {
		this.ruleTable = ruleTable;
	}

	public Integer getByteSize() {
		return byteSize;
	}

	public void setByteSize(Integer byteSize) {
		this.byteSize = byteSize;
	}

	public Double getArea() {
		return area;
	}

	public void setArea(Double area) {
		this.area = area;
	}

	public Double getTotalArea() {
		return totalArea;
	}

	public void setTotalArea(Double totalArea) {
		this.totalArea = totalArea;
	}

	public Double getPerimeter() {
		return perimeter;
	}

	public void setPerimeter(Double perimeter) {
		this.perimeter = perimeter;
	}

	public Double getBaseSize() {
		return baseSize;
	}

	public void setBaseSize(Double baseSize) {
		this.baseSize = baseSize;
	}

	public Double getSmallestSize() {
		return smallestSize;
	}

	public void setSmallestSize(Double smallestSize) {
		this.smallestSize = smallestSize;
	}

	public Integer getNumInt() {
		return numInt;
	}

	public void setNumInt(Integer numInt) {
		this.numInt = numInt;
	}

	public Integer getNumNch() {
		return numNch;
	}

	public void setNumNch(Integer numNch) {
		this.numNch = numNch;
	}

	public Integer getNumGp() {
		return numGp;
	}

	public void setNumGp(Integer numGp) {
		this.numGp = numGp;
	}

	public Integer getNumCrn() {
		return numCrn;
	}

	public void setNumCrn(Integer numCrn) {
		this.numCrn = numCrn;
	}

	public Double getPieceX() {
		return pieceX;
	}

	public void setPieceX(Double pieceX) {
		this.pieceX = pieceX;
	}

	public Double getPieceY() {
		return pieceY;
	}

	public void setPieceY(Double pieceY) {
		this.pieceY = pieceY;
	}

	public String getShrinkStretchX() {
		return shrinkStretchX;
	}

	public void setShrinkStretchX(String shrinkStretchX) {
		this.shrinkStretchX = shrinkStretchX;
	}

	public String getShrinkStretchY() {
		return shrinkStretchY;
	}

	public void setShrinkStretchY(String shrinkStretchY) {
		this.shrinkStretchY = shrinkStretchY;
	}

	public String getFabricCode() {
		return fabricCode;
	}

	public void setFabricCode(String fabricCode) {
		this.fabricCode = fabricCode;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getUserLastMod() {
		return userLastMod;
	}

	public void setUserLastMod(String userLastMod) {
		this.userLastMod = userLastMod;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUserCreated() {
		return userCreated;
	}

	public void setUserCreated(String userCreated) {
		this.userCreated = userCreated;
	}

	public String getPrevModTime() {
		return prevModTime;
	}

	public void setPrevModTime(String prevModTime) {
		this.prevModTime = prevModTime;
	}

	public String getUserPrevMod() {
		return userPrevMod;
	}

	public void setUserPrevMod(String userPrevMod) {
		this.userPrevMod = userPrevMod;
	}

	public LocalDateTime getImportedAt() {
		return importedAt;
	}

	public void setImportedAt(LocalDateTime importedAt) {
		this.importedAt = importedAt;
	}

	public String getImportedBy() {
		return importedBy;
	}

	public void setImportedBy(String importedBy) {
		this.importedBy = importedBy;
	}
}
