package com.lear.MGCMS.payload;

import javax.persistence.Id;

public class EmpStat {
	
	private String placement;
	private String folder;
	private String pattern;
	private String nomModele;
	private String idPaquet;
	private Long total;
	
	public EmpStat(String placement, String folder, String pattern, String nomModele, String idPaquet, Long total) {
		super();
		this.placement = placement;
		this.folder = folder;
		this.pattern = pattern;
		this.nomModele = nomModele;
		this.idPaquet = idPaquet;
		this.total = total;
	}
	public String getPlacement() {
		return placement;
	}
	public void setPlacement(String placement) {
		this.placement = placement;
	}
	public String getFolder() {
		return folder;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}
	public String getPattern() {
		return pattern;
	}
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	public String getNomModele() {
		return nomModele;
	}
	public void setNomModele(String nomModele) {
		this.nomModele = nomModele;
	}
	public String getIdPaquet() {
		return idPaquet;
	}
	public void setIdPaquet(String idPaquet) {
		this.idPaquet = idPaquet;
	}
	public Long getTotal() {
		return total;
	}
	public void setTotal(Long total) {
		this.total = total;
	}
	
	

}
