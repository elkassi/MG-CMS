package com.lear.MGCMS.repositories.scheduling;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lear.MGCMS.domain.scheduling.SchedulingConfig;

@Repository
public interface SchedulingConfigRepository extends JpaRepository<SchedulingConfig, Long> {

    /** Find config for a specific zone */
    Optional<SchedulingConfig> findByZoneCode(String zoneCode);

    /** Find global default config (zoneCode is null) */
    Optional<SchedulingConfig> findByZoneCodeIsNull();
}
