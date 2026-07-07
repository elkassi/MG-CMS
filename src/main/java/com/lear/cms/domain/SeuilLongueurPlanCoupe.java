package com.lear.cms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "SeuilLongueur_PlanCoupe")
public class SeuilLongueurPlanCoupe {

	@Id
	@Column(name = "ID_Seuil_Plan")
	private int idSeuil_Plan;
	
	@Column(name = "ID_ItemForeign1_Plan")
	private Integer idItemForeign1Plan;
	
	@Column(name = "SeuilMin_Plan")
	private Double seuilMinPlan;
	
	@Column(name = "SeuilMax_Plan")
	private Double seuilMaxPlan;
	
	@Column(name = "LongueurPlus_Plan")
	private Double longueurPlusPlan;
	
	@Column(name = "CommentSeuil_Plan")
	private String commentSeuilPlan;

	public SeuilLongueurPlanCoupe() {
		super();
	}

	public int getIdSeuil_Plan() {
		return idSeuil_Plan;
	}

	public void setIdSeuil_Plan(int idSeuil_Plan) {
		this.idSeuil_Plan = idSeuil_Plan;
	}

	public Integer getIdItemForeign1Plan() {
		return idItemForeign1Plan;
	}

	public void setIdItemForeign1Plan(Integer idItemForeign1Plan) {
		this.idItemForeign1Plan = idItemForeign1Plan;
	}

	public Double getSeuilMinPlan() {
		return seuilMinPlan;
	}

	public void setSeuilMinPlan(Double seuilMinPlan) {
		this.seuilMinPlan = seuilMinPlan;
	}

	public Double getSeuilMaxPlan() {
		return seuilMaxPlan;
	}

	public void setSeuilMaxPlan(Double seuilMaxPlan) {
		this.seuilMaxPlan = seuilMaxPlan;
	}

	public Double getLongueurPlusPlan() {
		return longueurPlusPlan;
	}

	public void setLongueurPlusPlan(Double longueurPlusPlan) {
		this.longueurPlusPlan = longueurPlusPlan;
	}

	public String getCommentSeuilPlan() {
		return commentSeuilPlan;
	}

	public void setCommentSeuilPlan(String commentSeuilPlan) {
		this.commentSeuilPlan = commentSeuilPlan;
	}
	
		    	
	
}
