package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class PlacementDetailId implements Serializable {

	private Integer ind;

	private String placement;
	
	private String folder;

	public PlacementDetailId(Integer ind, String placement, String folder) {
		super();
		this.ind = ind;
		this.placement = placement;
		this.folder = folder;
	}

	public PlacementDetailId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlacementDetailId accountId = (PlacementDetailId) o;
        return ind.equals(accountId.ind) &&
        		placement.equals(accountId.placement)&&
        		folder.equals(accountId.folder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ind, placement, folder);
    }

	
}

