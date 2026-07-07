package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.BoxTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoxTypeConfigRepository extends JpaRepository<BoxTypeConfig, Long> {

    Optional<BoxTypeConfig> findByBoxType(String boxType);
}
