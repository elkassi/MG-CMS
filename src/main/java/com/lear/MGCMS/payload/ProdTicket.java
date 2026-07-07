package com.lear.MGCMS.payload;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ProdTicket {

	private Long id;

	private String plsId;

	private String reftissu;
	
	private String description;
	
	private String labelId;
	
	private Double quantity;
	
	private String lotNr;
	
	private String tableName;
	
	private Double initQuantity;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;
	
	private Double quantitePLS;

	public ProdTicket() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPlsId() {
		return plsId;
	}

	public void setPlsId(String plsId) {
		this.plsId = plsId;
	}

	public String getReftissu() {
		return reftissu;
	}

	public void setReftissu(String reftissu) {
		this.reftissu = reftissu;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLabelId() {
		return labelId;
	}

	public void setLabelId(String labelId) {
		this.labelId = labelId;
	}

	public Double getQuantity() {
		return quantity;
	}

	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}

	public String getLotNr() {
		return lotNr;
	}

	public void setLotNr(String lotNr) {
		this.lotNr = lotNr;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public Double getInitQuantity() {
		return initQuantity;
	}

	public void setInitQuantity(Double initQuantity) {
		this.initQuantity = initQuantity;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Double getQuantitePLS() {
		return quantitePLS;
	}

	public void setQuantitePLS(Double quantitePLS) {
		this.quantitePLS = quantitePLS;
	}
}
