package com.lear.MGCMS.domain.CuttingRequest;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lear.MGCMS.utils.BoxIdGenerator;

@Entity
@Table(name = "CuttingRequestBox")
public class CuttingRequestBoxV2 {
	
	@Id
    private String id; 
	
	@ManyToOne
	@JsonIgnore
	private CuttingRequestV2 cuttingRequest;
	
	private String partNumber;
	private String description;
	private String item;
	private String wo;
	private String woid;
	private Integer qtyBox = 10;
	private Integer gammePrinted = 0;
	
	private String nbrImpression = "1";
	
	public CuttingRequestBoxV2() {
		super();
	}
	
	public String getNbrImpression() {
		return nbrImpression;
	}

	public void setNbrImpression(String nbrImpression) {
		this.nbrImpression = nbrImpression;
	}



	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getItem() {
		return item;
	}


	public void setItem(String item) {
		this.item = item;
	}


	public String getWo() {
		return wo;
	}


	public void setWo(String wo) {
		this.wo = wo;
	}


	public String getWoid() {
		return woid;
	}


	public void setWoid(String woid) {
		this.woid = woid;
	}


	public String getPartNumber() {
		return partNumber;
	}


	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}


	public Integer getQtyBox() {
		return qtyBox;
	}

	public void setQtyBox(Integer qtyBox) {
		this.qtyBox = qtyBox;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public CuttingRequestV2 getCuttingRequest() {
		return cuttingRequest;
	}

	public void setCuttingRequest(CuttingRequestV2 cuttingRequest) {
		this.cuttingRequest = cuttingRequest;
	}

	public Integer getGammePrinted() {
		return gammePrinted;
	}

	public void setGammePrinted(Integer gammePrinted) {
		this.gammePrinted = gammePrinted;
	}

}
