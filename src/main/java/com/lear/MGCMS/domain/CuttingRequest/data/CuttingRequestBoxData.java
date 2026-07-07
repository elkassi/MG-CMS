package com.lear.MGCMS.domain.CuttingRequest.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequest;

@Entity
@Table(name = "CuttingRequestBox")
public class CuttingRequestBoxData {

	@Id
	private String id; 
	
	
	@Column(name = "cuttingRequest_sequence")
	private String sequence;
	
	private String partNumber;
	private String description;
	private String item;
	private String wo;
	private String woid;
	
	private Integer qtyBox = 10;
	
	private Integer gammePrinted = 0;
	private String nbrImpression = "1";


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
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

	public Integer getQtyBox() {
		return qtyBox;
	}

	public void setQtyBox(Integer qtyBox) {
		this.qtyBox = qtyBox;
	}

	public Integer getGammePrinted() {
		return gammePrinted;
	}

	public void setGammePrinted(Integer gammePrinted) {
		this.gammePrinted = gammePrinted;
	}

	public String getNbrImpression() {
		return nbrImpression;
	}

	public void setNbrImpression(String nbrImpression) {
		this.nbrImpression = nbrImpression;
	}
	
	
	
}
