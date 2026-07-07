package com.lear.MGCMS.repositories.scanCoupe;

import com.lear.MGCMS.domain.scanCoupe.Config;
import org.springframework.data.repository.CrudRepository;


public interface ConfigRepository extends CrudRepository<Config, String> {

	Config findByParam(String param);

}
