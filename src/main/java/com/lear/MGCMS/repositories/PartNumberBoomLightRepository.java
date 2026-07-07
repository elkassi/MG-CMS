package com.lear.MGCMS.repositories;

import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.domain.PartNumberBoomId;
import com.lear.MGCMS.domain.PartNumberBoomLight;

public interface PartNumberBoomLightRepository extends CrudRepository<PartNumberBoomLight, PartNumberBoomId> {

}
