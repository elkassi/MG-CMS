package com.lear.MGCMS.domain.CuttingRequest;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lear.MGCMS.utils.BoxIdGenerator;

@Entity
@Table(name = "CuttingRequestBox")
public class CuttingRequestBoxInfo {

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "box_id")
    @GenericGenerator(
        name = "box_id", 
        strategy = "com.lear.MGCMS.utils.BoxIdGenerator", 
        parameters = {
        		@Parameter(name = BoxIdGenerator.INCREMENT_PARAM, value = "1"),
        		@Parameter(name = "initial_value", value = "60000001")
        		})
    private String id; 
	
	@ManyToOne
	@JoinColumn(name = "cuttingRequest_sequence")
	private CuttingRequest cuttingRequest;
	
	private String partNumber;
	private String description;
	private String item;
	private String wo;
	private String woid;
	
	private Integer qtyBox = 10;
	
	private Integer gammePrinted = 0;
	
	public CuttingRequestBoxInfo() {
		super();
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


	public CuttingRequest getCuttingRequest() {
		return cuttingRequest;
	}

	public void setCuttingRequest(CuttingRequest cuttingRequest) {
		this.cuttingRequest = cuttingRequest;
	}

	public Integer getGammePrinted() {
		return gammePrinted;
	}

	public void setGammePrinted(Integer gammePrinted) {
		this.gammePrinted = gammePrinted;
	}
	
}
