package com.lear.MGCMS.domain;

import java.io.Serializable;
import java.util.Objects;

public class ReftissuCategoryId implements Serializable{

	private String category;

	private String partNumberMaterialConfig;

	public ReftissuCategoryId(String category, String partNumberMaterialConfig) {
		super();
		this.category = category;
		this.partNumberMaterialConfig = partNumberMaterialConfig;
	}

	public ReftissuCategoryId() {
		super();
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReftissuCategoryId accountId = (ReftissuCategoryId) o;
        return category.equals(accountId.category) &&
        		partNumberMaterialConfig.equals(accountId.partNumberMaterialConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, partNumberMaterialConfig);
    }
	
}
