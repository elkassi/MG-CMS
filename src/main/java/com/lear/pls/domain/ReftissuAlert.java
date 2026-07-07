package com.lear.pls.domain;

import javax.persistence.*;

@Entity
public class ReftissuAlert {

	@Id
	private String id;

	@Lob
	private String alertContent;

	public ReftissuAlert() {
		super();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAlertContent() {
		return alertContent;
	}

	public void setAlertContent(String alertContent) {
		this.alertContent = alertContent;
	}
}
