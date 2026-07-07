package com.lear.pls.domain;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
public class DemandeHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String updatedBy;
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime updatedAt;
	
	private String typeOperation;
	
	private String tableName;
	
	@Lob
	private String changement;

	public DemandeHistory(String updatedBy, LocalDateTime updatedAt, String typeOperation, String tableName,
			String changement) {
		super();
		this.updatedBy = updatedBy;
		this.updatedAt = updatedAt;
		this.typeOperation = typeOperation;
		this.tableName = tableName;
		this.changement = changement;
	}

	public DemandeHistory() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getTypeOperation() {
		return typeOperation;
	}

	public void setTypeOperation(String typeOperation) {
		this.typeOperation = typeOperation;
	}


	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getChangement() {
		return changement;
	}

	public void setChangement(String changement) {
		this.changement = changement;
	}
	
	
	
	
}
