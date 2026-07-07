package com.lear.MGCMS.payload;

import java.util.ArrayList;
import java.util.List;

import com.lear.MGCMS.domain.GammeTechniqueText;

public class GammeTechniqueTextBulkRequest {

	private String partNumber;
	private List<GammeTechniqueText> texts = new ArrayList<GammeTechniqueText>();
	private List<Long> deletedIds = new ArrayList<Long>();

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

	public List<GammeTechniqueText> getTexts() {
		return texts;
	}

	public void setTexts(List<GammeTechniqueText> texts) {
		this.texts = texts;
	}

	public List<Long> getDeletedIds() {
		return deletedIds;
	}

	public void setDeletedIds(List<Long> deletedIds) {
		this.deletedIds = deletedIds;
	}
}
