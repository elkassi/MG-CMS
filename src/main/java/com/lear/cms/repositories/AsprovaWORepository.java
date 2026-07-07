package com.lear.cms.repositories;

import com.lear.cms.domain.AsprovaWO;
import com.lear.cms.domain.CategoryLaizePlanCoupe;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AsprovaWORepository extends CrudRepository<AsprovaWO, Integer> {
    @Query("SELECT MAX(idTableAsprovaWO) FROM AsprovaWO")
    Integer getMaxId();
}
