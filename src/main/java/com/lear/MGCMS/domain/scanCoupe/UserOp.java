package com.lear.MGCMS.domain.scanCoupe;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Scan_UserOp")
public class UserOp {

	@Id
	@Column(name = "matricule",unique=true, nullable=false)
	private Integer matricule;
	
	@Column(name = "password")
	private String password;
	
	@Column(name = "block")
	private Boolean block = false;
	
	@Column(name = "reason")
	private String reason;
	
	@Column(name = "blockDate")
	@JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
	private LocalDateTime blockDate;
	@Transient
	private String code;
	
	

	public UserOp(Integer matricule, String password) {
		super();
		this.matricule = matricule;
		this.password = password;
	}
	
	

	@Override
	public String toString() {
		return "UserOp [matricule=" + matricule + ", password=" + password + ", block=" + block + ", reason=" + reason
				+ ", code=" + code + "]";
	}


	public UserOp(Integer matricule, String password, Boolean block, String reason) {
		super();
		this.matricule = matricule;
		this.password = password;
		this.block = block;
		this.reason = reason;
	}



	public UserOp() {
		super();
	}

	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public LocalDateTime getBlockDate() {
		return blockDate;
	}



	public void setBlockDate(LocalDateTime blockDate) {
		this.blockDate = blockDate;
	}



	public Integer getMatricule() {
		return matricule;
	}

	public void setMatricule(Integer matricule) {
		this.matricule = matricule;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	

	


	public String getReason() {
		return reason;
	}


	public void setReason(String reason) {
		this.reason = reason;
	}



	public Boolean getBlock() {
		return block;
	}

	public void setBlock(Boolean block) {
		this.block = block;
	}
	
}
