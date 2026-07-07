package com.lear.MGCMS.repositories;

import org.springframework.data.repository.CrudRepository;

import com.lear.MGCMS.domain.Site;
import com.lear.MGCMS.domain.User;

public interface SiteRepository extends CrudRepository<Site, String> {

}
