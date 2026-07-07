package com.lear.ctc.domain;

import java.time.LocalDateTime;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "files")
@XmlRootElement
public class Files {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "part_number_cover")
	@NotBlank(message = "partNumberCover feild is required")
	private String partNumberCover;
	@Column(name = "part_number_cover_description")
	@NotBlank(message = "partNumberCoverDesciption feild is required")
	@Size(max = 60, message = "Part Number Cover Desciption must not exceed 40 characters")
	private String partNumberCoverDesciption;
	@Column(name = "panel_number")
	@NotBlank(message = "panelNumber feild is required")
	private String panelNumber; 
	@Column(name= "semi_finished_good_part_number")
	private String semiFinishedGoodPartNumber;
	@Column(name = "pattern")
	@NotBlank(message = "pattern feild is required")
	private String pattern;
	@Column(name = "part_number_material")
	@NotBlank(message = "partNumberMaterial feild is required")
	private String partNumberMaterial;
	
	@Column(name = "part_number_material_description")
	@NotBlank(message = "Part Number Material Description feild is required")
	@Size(max = 80, message = "Part Number Material Description must not exceed 40 characters")
	private String partNumberMaterialDescription;
	
	@Column(name = "type")
	@NotBlank(message = "type feild is required")
	private String type;
	
	@Column(name= "ecn_number")
	private String ecnNumber;
	
	@Column(name= "quantity")
	private Integer quantity;
	
	@Column(name= "projet")
	@NotBlank(message = "projet feild is required")
	private String projet;
	
	@Column(name = "added_by")
	private String addedBy;
	
	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name= "created_at")
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime createdAt;
	
	@Column(name= "updated_at")
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime updatedAt ;
	@Column(name= "plt_found")
	private Boolean pltFound;

	@Column(name= "tol_min1")
	private Double min1;//-1
	@Column(name= "tol_max1")
	private Double max1;//2.5
	@Column(name= "tol_min2")
	private Double min2;//-1
	@Column(name= "tol_max2")
	private Double max2;//2.5
	@Column(name= "toleranceDrill")
	private Integer toleranceDrill = 1;

	public Integer getToleranceDrill() {
		return toleranceDrill;
	}

	public void setToleranceDrill(Integer toleranceDrill) {
		this.toleranceDrill = toleranceDrill;
	}

	public Files() {
		super();
	}

	public Double getMin1() {
		return min1;
	}

	public void setMin1(Double min1) {
		this.min1 = min1;
	}

	public Double getMax1() {
		return max1;
	}

	public void setMax1(Double max1) {
		this.max1 = max1;
	}

	public Double getMin2() {
		return min2;
	}

	public void setMin2(Double min2) {
		this.min2 = min2;
	}

	public Double getMax2() {
		return max2;
	}

	public void setMax2(Double max2) {
		this.max2 = max2;
	}

	public Boolean getPltFound() {
		return pltFound;
	}



	public void setPltFound(Boolean pltFound) {
		this.pltFound = pltFound;
	}



	public String getProjet() {
		return projet;
	}


	public void setProjet(String projet) {
		this.projet = projet;
	}


	@Override
	public String toString() {
		return "Files [id=" + id + ", partNumberCover=" + partNumberCover + ", partNumberCoverDesciption="
				+ partNumberCoverDesciption + ", panelNumber=" + panelNumber + ", pattern=" + pattern
				+ ", partNumberMaterial=" + partNumberMaterial + ", partNumberMaterialDescription="
				+ partNumberMaterialDescription + ", type=" + type + ", addedBy=" + addedBy + ", updatedBy=" + updatedBy
				+ ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", ecnNumber=" + ecnNumber + ", quantity="
				+ quantity + ", semiFinishedGoodPartNumber=" + semiFinishedGoodPartNumber + "]";
	}


	public String getEcnNumber() {
		return ecnNumber;
	}


	public void setEcnNumber(String ecnNumber) {
		this.ecnNumber = ecnNumber;
	}


	public Integer getQuantity() {
		return quantity;
	}


	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}


	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public String getSemiFinishedGoodPartNumber() {
		return semiFinishedGoodPartNumber;
	}

	public void setSemiFinishedGoodPartNumber(String semiFinishedGoodPartNumber) {
		this.semiFinishedGoodPartNumber = semiFinishedGoodPartNumber;
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



	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPartNumberCover() {
		return partNumberCover;
	}

	public void setPartNumberCover(String partNumberCover) {
		this.partNumberCover = partNumberCover;
	}

	public String getPartNumberCoverDesciption() {
		return partNumberCoverDesciption;
	}

	public void setPartNumberCoverDesciption(String partNumberCoverDesciption) {
		this.partNumberCoverDesciption = partNumberCoverDesciption;
	}

	public String getPanelNumber() {
		return panelNumber;
	}

	public void setPanelNumber(String panelNumber) {
		this.panelNumber = panelNumber;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getPartNumberMaterial() {
		return partNumberMaterial;
	}

	public void setPartNumberMaterial(String partNumberMaterial) {
		this.partNumberMaterial = partNumberMaterial;
	}

	public String getPartNumberMaterialDescription() {
		return partNumberMaterialDescription;
	}

	public void setPartNumberMaterialDescription(String partNumberMaterialDescription) {
		this.partNumberMaterialDescription = partNumberMaterialDescription;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(String addedBy) {
		this.addedBy = addedBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	
	
	
}
