package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.DefautRouleau;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DefautRouleauRepository extends CrudRepository<DefautRouleau, String> {

    @Query("from DefautRouleau where active = 1")
    List<DefautRouleau> findAllActive();
}
