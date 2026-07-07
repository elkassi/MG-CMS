package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.LaminationPls;
import org.springframework.data.repository.CrudRepository;

public interface LaminationPlsRepository extends CrudRepository<LaminationPls, String> {
    LaminationPls findByReftissu(String reftissu);
}
