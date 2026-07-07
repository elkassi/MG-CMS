package com.lear.ctc.domain;

import java.time.LocalDateTime;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "sequences")
@XmlRootElement
public class Sequences {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "sequence")
	private String sequence;
	
	@Column(name = "cover_part_number")
	private String coverPartNumber;

	@Column(name= "created_at")
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime createdAt;
	
	@Column(name= "updated_at")
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime updatedAt ;
	
	

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public String getCoverPartNumber() {
		return coverPartNumber;
	}

	public void setCoverPartNumber(String coverPartNumber) {
		this.coverPartNumber = coverPartNumber;
	}

	public Sequences() {
		super();
	}
	
	
	
}
