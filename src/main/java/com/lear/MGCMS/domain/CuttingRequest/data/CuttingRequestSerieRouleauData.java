package com.lear.MGCMS.domain.CuttingRequest.data;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMax;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "CuttingRequestSerieRouleau")
public class CuttingRequestSerieRouleauData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "cuttingRequestSerie_serie")
	private String serie;
	
	private String confirmReftissu;
	
	private String idRouleau;
	
	private String lotFrs;
		
	private Double metrage;
	private Double laize;
	private Integer nbrCouche;
	private Double longueurPremierCouche;
	private Double longueurCoucheOverlap;
	private Double defaut;
	private Double nonUtitlse;
	private String nuance;

	private Double retour;
	private Double excess;
	private Double overlap1;
	private Double overlap2;
	private Double overlap3;
	private Double overlap4;
	private Double overlap5;
	private Double overlap6;
	private Double overlap7;
	private Double overlap8;
	@DecimalMax(value = "9999999.999", inclusive = true)
	private Double totalUsage;
	private Boolean confirmRetour = false;
	
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime updatedAt;

	private LocalDateTime deblockedDate;
	private String deblockedBy;
	private String defautCode;
	private Double deblockedMetrage;

	private String machine;
	private String location;
	private String sequence;
	private String createdBy;
	private String updatedBy;

	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	@Override
	public String toString() {
		return "CuttingRequestSerieRouleauData{" +
				"id=" + id +
				", serie='" + serie + '\'' +
				", confirmReftissu='" + confirmReftissu + '\'' +
				", idRouleau='" + idRouleau + '\'' +
				", lotFrs='" + lotFrs + '\'' +
				", metrage=" + metrage +
				", laize=" + laize +
				", nbrCouche=" + nbrCouche +
				", longueurPremierCouche=" + longueurPremierCouche +
				", longueurCoucheOverlap=" + longueurCoucheOverlap +
				", defaut=" + defaut +
				", nonUtitlse=" + nonUtitlse +
				", retour=" + retour +
				", excess=" + excess +
				", overlap1=" + overlap1 +
				", overlap2=" + overlap2 +
				", overlap3=" + overlap3 +
				", overlap4=" + overlap4 +
				", overlap5=" + overlap5 +
				", overlap6=" + overlap6 +
				", overlap7=" + overlap7 +
				", overlap8=" + overlap8 +
				", totalUsage=" + totalUsage +
				", confirmRetour=" + confirmRetour +
				", createdAt=" + createdAt +
				", updatedAt=" + updatedAt +
				", deblockedDate=" + deblockedDate +
				", deblockedBy='" + deblockedBy + '\'' +
				", defautCode='" + defautCode + '\'' +
				", deblockedMetrage=" + deblockedMetrage +
				'}';
	}

	public String getNuance() {
		return nuance;
	}

	public void setNuance(String nuance) {
		this.nuance = nuance;
	}

	public Double getDeblockedMetrage() {
		return deblockedMetrage;
	}

	public void setDeblockedMetrage(Double deblockedMetrage) {
		this.deblockedMetrage = deblockedMetrage;
	}

	public LocalDateTime getDeblockedDate() {
		return deblockedDate;
	}

	public void setDeblockedDate(LocalDateTime deblockedDate) {
		this.deblockedDate = deblockedDate;
	}

	public String getDeblockedBy() {
		return deblockedBy;
	}

	public void setDeblockedBy(String deblockedBy) {
		this.deblockedBy = deblockedBy;
	}

	public String getDefautCode() {
		return defautCode;
	}

	public void setDefautCode(String defautCode) {
		this.defautCode = defautCode;
	}

	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSerie() {
		return serie;
	}

	public void setSerie(String serie) {
		this.serie = serie;
	}

	public String getConfirmReftissu() {
		return confirmReftissu;
	}

	public void setConfirmReftissu(String confirmReftissu) {
		this.confirmReftissu = confirmReftissu;
	}

	public String getIdRouleau() {
		return idRouleau;
	}

	public void setIdRouleau(String idRouleau) {
		this.idRouleau = idRouleau;
	}

	public String getLotFrs() {
		return lotFrs;
	}

	public void setLotFrs(String lotFrs) {
		this.lotFrs = lotFrs;
	}

	public Double getMetrage() {
		return metrage;
	}

	public void setMetrage(Double metrage) {
		this.metrage = metrage;
	}

	public Double getLaize() {
		return laize;
	}

	public void setLaize(Double laize) {
		this.laize = laize;
	}

	public Integer getNbrCouche() {
		return nbrCouche;
	}

	public void setNbrCouche(Integer nbrCouche) {
		this.nbrCouche = nbrCouche;
	}

	public Double getLongueurPremierCouche() {
		return longueurPremierCouche;
	}

	public void setLongueurPremierCouche(Double longueurPremierCouche) {
		this.longueurPremierCouche = longueurPremierCouche;
	}

	public Double getLongueurCoucheOverlap() {
		return longueurCoucheOverlap;
	}

	public void setLongueurCoucheOverlap(Double longueurCoucheOverlap) {
		this.longueurCoucheOverlap = longueurCoucheOverlap;
	}

	public Double getDefaut() {
		return defaut;
	}

	public void setDefaut(Double defaut) {
		this.defaut = defaut;
	}

	public Double getNonUtitlse() {
		return nonUtitlse;
	}

	public void setNonUtitlse(Double nonUtitlse) {
		this.nonUtitlse = nonUtitlse;
	}

	public Double getRetour() {
		return retour;
	}

	public void setRetour(Double retour) {
		this.retour = retour;
	}

	public Double getExcess() {
		return excess;
	}

	public void setExcess(Double excess) {
		this.excess = excess;
	}

	public Double getOverlap1() {
		return overlap1;
	}

	public void setOverlap1(Double overlap1) {
		this.overlap1 = overlap1;
	}

	public Double getOverlap2() {
		return overlap2;
	}

	public void setOverlap2(Double overlap2) {
		this.overlap2 = overlap2;
	}

	public Double getOverlap3() {
		return overlap3;
	}

	public void setOverlap3(Double overlap3) {
		this.overlap3 = overlap3;
	}

	public Double getOverlap4() {
		return overlap4;
	}

	public void setOverlap4(Double overlap4) {
		this.overlap4 = overlap4;
	}

	public Double getOverlap5() {
		return overlap5;
	}

	public void setOverlap5(Double overlap5) {
		this.overlap5 = overlap5;
	}

	public Double getOverlap6() {
		return overlap6;
	}

	public void setOverlap6(Double overlap6) {
		this.overlap6 = overlap6;
	}

	public Double getOverlap7() {
		return overlap7;
	}

	public void setOverlap7(Double overlap7) {
		this.overlap7 = overlap7;
	}

	public Double getOverlap8() {
		return overlap8;
	}

	public void setOverlap8(Double overlap8) {
		this.overlap8 = overlap8;
	}

	public Double getTotalUsage() {
		return totalUsage;
	}

	public void setTotalUsage(Double totalUsage) {
		this.totalUsage = totalUsage;
	}

	public Boolean getConfirmRetour() {
		return confirmRetour;
	}

	public void setConfirmRetour(Boolean confirmRetour) {
		this.confirmRetour = confirmRetour;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
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
    
    
	
}
