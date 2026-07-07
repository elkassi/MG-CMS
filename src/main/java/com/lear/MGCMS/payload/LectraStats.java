package com.lear.MGCMS.payload;

public class LectraStats {
	
	private String machine;
	private Long countPlacement;
	private Double sumLongueur;
	
	public LectraStats(String machine, Long countPlacement, Double sumLongueur) {
		super();
		this.machine = machine;
		this.countPlacement = countPlacement;
		this.sumLongueur = sumLongueur;
	}
	public String getMachine() {
		return machine;
	}
	public void setMachine(String machine) {
		this.machine = machine;
	}
	public Long getCountPlacement() {
		return countPlacement;
	}
	public void setCountPlacement(Long countPlacement) {
		this.countPlacement = countPlacement;
	}
	public Double getSumLongueur() {
		return sumLongueur;
	}
	public void setSumLongueur(Double sumLongueur) {
		this.sumLongueur = sumLongueur;
	}
	

}
