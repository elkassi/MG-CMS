package com.lear.MGCMS.payload;

import java.time.LocalDate;

public class WorkOrderElem {
	
	private String item;
	private String wo;
	private String woid;
	private Double qtyOpen;
	private Double qtyRejeter;
	private Double qtyCompleted;
	private LocalDate dueDate;
	private String shift;
	private String st;

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


	public Double getQtyOpen() {
		return qtyOpen;
	}

	public void setQtyOpen(Double qtyOpen) {
		this.qtyOpen = qtyOpen;
	}

	public Double getQtyRejeter() {
		return qtyRejeter;
	}

	public void setQtyRejeter(Double qtyRejeter) {
		this.qtyRejeter = qtyRejeter;
	}

	public Double getQtyCompleted() {
		return qtyCompleted;
	}

	public void setQtyCompleted(Double qtyCompleted) {
		this.qtyCompleted = qtyCompleted;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getShift() {
		return shift;
	}

	public void setShift(String shift) {
		this.shift = shift;
	}

	public String getSt() {
		return st;
	}

	public void setSt(String st) {
		this.st = st;
	}

}
