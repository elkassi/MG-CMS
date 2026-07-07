package com.lear.MGCMS.domain.CuttingPlan;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class CuttingPlanRapportDrill {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JsonIgnore
	private CuttingPlan cuttingPlan;
	
	private String nomPlacement;
	private String pointsAtributs;
	private String pointAttrbQt;
	private String typeDeCrans;
	private String qtCrans;
	private String labelInterne;
	private String qtInterne;
	
	public CuttingPlanRapportDrill() {
		super();
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public CuttingPlan getCuttingPlan() {
		return cuttingPlan;
	}
	public void setCuttingPlan(CuttingPlan cuttingPlan) {
		this.cuttingPlan = cuttingPlan;
	}
	public String getNomPlacement() {
		return nomPlacement;
	}
	public void setNomPlacement(String nomPlacement) {
		this.nomPlacement = nomPlacement;
	}
	public String getPointsAtributs() {
		return pointsAtributs;
	}
	public void setPointsAtributs(String pointsAtributs) {
		this.pointsAtributs = pointsAtributs;
	}
	public String getPointAttrbQt() {
		return pointAttrbQt;
	}
	public void setPointAttrbQt(String pointAttrbQt) {
		this.pointAttrbQt = pointAttrbQt;
	}
	public String getTypeDeCrans() {
		return typeDeCrans;
	}
	public void setTypeDeCrans(String typeDeCrans) {
		this.typeDeCrans = typeDeCrans;
	}
	public String getQtCrans() {
		return qtCrans;
	}
	public void setQtCrans(String qtCrans) {
		this.qtCrans = qtCrans;
	}
	public String getLabelInterne() {
		return labelInterne;
	}
	public void setLabelInterne(String labelInterne) {
		this.labelInterne = labelInterne;
	}
	public String getQtInterne() {
		return qtInterne;
	}
	public void setQtInterne(String qtInterne) {
		this.qtInterne = qtInterne;
	}
	
	
	
}
