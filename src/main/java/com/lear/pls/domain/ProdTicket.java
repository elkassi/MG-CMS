package com.lear.pls.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="ProdTicket")
public class ProdTicket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name= "pls_id")
	private String plsId;
	
	private String reftissu;
	
	private String description;
	@Column(name= "labelId")
	private String labelId;
	
	private Double quantity;
	@Column(name= "lotNr")
	private String lotNr;
	@Column(name= "tableName")
	private String tableName;
	@Column(name= "initQuantity")
	private Double initQuantity;
	@Column(name= "prixUnit")
	private Double prixUnit;
	@Column(name= "prixTotal")
	private Double prixTotal;
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    @Column(updatable = false)
	private LocalDateTime createdAt;
	@Column(name= "quantitePLS")
	private Double quantitePLS;

	@Override
	public String toString() {
		return "ProdTicket{" +
				"id=" + id +
				", plsId='" + plsId + '\'' +
				", reftissu='" + reftissu + '\'' +
				", description='" + description + '\'' +
				", labelId='" + labelId + '\'' +
				", quantity=" + quantity +
				", lotNr='" + lotNr + '\'' +
				", tableName='" + tableName + '\'' +
				", initQuantity=" + initQuantity +
				", prixUnit=" + prixUnit +
				", prixTotal=" + prixTotal +
				", createdAt=" + createdAt +
				", quantitePLS=" + quantitePLS +
				'}';
	}

	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
	
	public ProdTicket() {
		super();
	}

	public String getPlsId() {
		return plsId;
	}

	public void setPlsId(String plsId) {
		this.plsId = plsId;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public void setId(Long id) {
		this.id = id;
	}


	public Double getPrixUnit() {
		return prixUnit;
	}

	public void setPrixUnit(Double prixUnit) {
		this.prixUnit = prixUnit;
	}

	public Double getPrixTotal() {
		return prixTotal;
	}

	public void setPrixTotal(Double prixTotal) {
		this.prixTotal = prixTotal;
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

	public Double getQuantity() {
		return quantity;
	}

	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}

	public Double getInitQuantity() {
		return initQuantity;
	}

	public void setInitQuantity(Double initQuantity) {
		this.initQuantity = initQuantity;
	}

	public Double getQuantitePLS() {
		return quantitePLS;
	}

	public void setQuantitePLS(Double quantitePLS) {
		this.quantitePLS = quantitePLS;
	}

	

	
	
}
