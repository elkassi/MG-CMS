package com.lear.pls.domain;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
public class Reftissu {

	@Id
	private String id;

	@NotNull(message = "ref2 can't be null")
	private String oldReftissu;

	public Reftissu() {
		super();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOldReftissu() {
		return oldReftissu;
	}

	public void setOldReftissu(String oldReftissu) {
		this.oldReftissu = oldReftissu;
	}
}
