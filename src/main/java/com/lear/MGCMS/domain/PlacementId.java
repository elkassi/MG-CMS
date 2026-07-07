package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class PlacementId implements Serializable {

	private String folder;

	private String placement;

	public PlacementId(String folder, String placement) {
		super();
		this.folder = folder;
		this.placement = placement;
	}

	public PlacementId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlacementId accountId = (PlacementId) o;
        return folder.equals(accountId.folder) &&
        		placement.equals(accountId.placement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(folder, placement);
    }

	
}

