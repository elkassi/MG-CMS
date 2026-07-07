package com.lear.MGCMS.domain.CuttingRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.domain.Zone;

@Entity
@Table(name="CuttingRequest")
public class CuttingRequestV2 {
	
		@Id
		private String sequence;
		private Long cuttingPlanId;
	    private String projet;
	    private String version;
	    
		private String modele;
		private String definition;		
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
		private LocalDateTime createdAt;
		@ManyToOne
		private User createdBy;
		private LocalDate planningDate; 
		private String shift;

		@OneToMany(mappedBy="cuttingRequest", cascade = CascadeType.ALL)
		@NotEmpty(message = "ce champ ne peut pas être vide")
		@LazyCollection(LazyCollectionOption.FALSE)
		private List<CuttingRequestPartNumberV2> cuttingRequestPartNumbers = new ArrayList<CuttingRequestPartNumberV2>();

		@OneToMany(mappedBy="cuttingRequest", cascade = CascadeType.ALL)
		@NotEmpty(message = "ce champ ne peut pas être vide")
		@LazyCollection(LazyCollectionOption.FALSE)
		private List<CuttingRequestSerieV2> cuttingRequestSeries = new ArrayList<CuttingRequestSerieV2>();
		
		@OneToMany(mappedBy="cuttingRequest", cascade = CascadeType.ALL)
		@NotEmpty(message = "ce champ ne peut pas être vide")
		@LazyCollection(LazyCollectionOption.FALSE)
		List<CuttingRequestBoxV2> cuttingRequestBoxs = new ArrayList<CuttingRequestBoxV2>();
		
		@ManyToOne
		private Zone zone;

	private Long cmsId;
	private LocalDate dueDate;
	private String dueShift;

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getDueShift() {
		return dueShift;
	}

	public void setDueShift(String dueShift) {
		this.dueShift = dueShift;
	}

	public Long getCmsId() {
			return cmsId;
		}


		public void setCmsId(Long cmsId) {
			this.cmsId = cmsId;
		}


		public User getCreatedBy() {
			return createdBy;
		}


		public void setCreatedBy(User createdBy) {
			this.createdBy = createdBy;
		}


		public Zone getZone() {
			return zone;
		}


		public void setZone(Zone zone) {
			this.zone = zone;
		}


		public CuttingRequestV2() {
			super();
		}
		

		public List<CuttingRequestBoxV2> getCuttingRequestBoxs() {
			return cuttingRequestBoxs;
		}


		public void setCuttingRequestBoxs(List<CuttingRequestBoxV2> cuttingRequestBoxs) {
			this.cuttingRequestBoxs = cuttingRequestBoxs;
		}


		public Long getCuttingPlanId() {
			return cuttingPlanId;
		}

		public void setCuttingPlanId(Long cuttingPlanId) {
			this.cuttingPlanId = cuttingPlanId;
		}

		public LocalDate getPlanningDate() {
			return planningDate;
		}

		public void setPlanningDate(LocalDate planningDate) {
			this.planningDate = planningDate;
		}

		public String getShift() {
			return shift;
		}

		public void setShift(String shift) {
			this.shift = shift;
		}

		public String getProjet() {
			return projet;
		}

		public void setProjet(String projet) {
			this.projet = projet;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public List<CuttingRequestSerieV2> getCuttingRequestSeries() {
			return cuttingRequestSeries;
		}

		public void setCuttingRequestSeries(List<CuttingRequestSerieV2> cuttingRequestSeries) {
			this.cuttingRequestSeries = cuttingRequestSeries;
		}

		public String getSequence() {
			return sequence;
		}

		public void setSequence(String sequence) {
			this.sequence = sequence;
		}

		public String getModele() {
			return modele;
		}

		public void setModele(String modele) {
			this.modele = modele;
		}

		public String getDefinition() {
			return definition;
		}

		public void setDefinition(String definition) {
			this.definition = definition;
		}

		public LocalDateTime getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(LocalDateTime createdAt) {
			this.createdAt = createdAt;
		}

		public List<CuttingRequestPartNumberV2> getCuttingRequestPartNumbers() {
			return cuttingRequestPartNumbers;
		}

		public void setCuttingRequestPartNumbers(List<CuttingRequestPartNumberV2> cuttingRequestPartNumbers) {
			this.cuttingRequestPartNumbers = cuttingRequestPartNumbers;
		}

}
