package com.lear.MGCMS.payload;

import java.time.LocalDateTime;

public class SequenceStatus {
	
	private String sequence;
	private String statusMatelassage;
	private LocalDateTime dateDebutCoupe;
	private LocalDateTime dateFinCoupe;
	private Integer total;
	private Integer notNullCount;
	private Integer totalBoxes;
	
	public Integer getTotalBoxes() {
		return totalBoxes;
	}
	public void setTotalBoxes(Integer totalBoxes) {
		this.totalBoxes = totalBoxes;
	}
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public String getStatusMatelassage() {
		return statusMatelassage;
	}
	public void setStatusMatelassage(String statusMatelassage) {
		this.statusMatelassage = statusMatelassage;
	}
	public LocalDateTime getDateDebutCoupe() {
		return dateDebutCoupe;
	}
	public void setDateDebutCoupe(LocalDateTime dateDebutCoupe) {
		this.dateDebutCoupe = dateDebutCoupe;
	}
	public LocalDateTime getDateFinCoupe() {
		return dateFinCoupe;
	}
	public void setDateFinCoupe(LocalDateTime dateFinCoupe) {
		this.dateFinCoupe = dateFinCoupe;
	}
	public Integer getTotal() {
		return total;
	}
	public void setTotal(Integer total) {
		this.total = total;
	}
	public Integer getNotNullCount() {
		return notNullCount;
	}
	public void setNotNullCount(Integer notNullCount) {
		this.notNullCount = notNullCount;
	}
	
	

}
