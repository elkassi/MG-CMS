package com.lear.pls.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ScrapRapport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "scrap_id")
	private String scrap;
	
	private String pn;
	
	private String description;
		
	private Double quantiteScrap;

	private Double prixUnit;
	
	private Double prixTotal;	
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    @Column(updatable = false)
	private LocalDateTime createdAt;
	
	@Transient
	private String defaut;
	
	@Transient
	private String projet;
	
	@Transient
	private String typeDefaut;
	
	@Transient
	private String site;
	
	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
	
	
	
	

	public ScrapRapport(String pn, Double quantiteScrap) {
		super();
		this.pn = pn;
		this.quantiteScrap = quantiteScrap;
	}





	public String getDescription() {
		return description;
	}





	public void setDescription(String description) {
		this.description = description;
	}





	public ScrapRapport() {
		super();
	}

	


	public String getPn() {
		return pn;
	}





	public void setPn(String pn) {
		this.pn = pn;
	}





	public String getDefaut() {
		return defaut;
	}



	public void setDefaut(String defaut) {
		this.defaut = defaut;
	}



	public String getProjet() {
		return projet;
	}



	public void setProjet(String projet) {
		this.projet = projet;
	}



	public String getTypeDefaut() {
		return typeDefaut;
	}



	public void setTypeDefaut(String typeDefaut) {
		this.typeDefaut = typeDefaut;
	}



	public String getSite() {
		return site;
	}



	public void setSite(String site) {
		this.site = site;
	}



	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getScrap() {
		return scrap;
	}

	public void setScrap(String scrap) {
		this.scrap = scrap;
	}

	public Double getQuantiteScrap() {
		return quantiteScrap;
	}



	public void setQuantiteScrap(Double quantiteScrap) {
		this.quantiteScrap = quantiteScrap;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
	
	
	
}
