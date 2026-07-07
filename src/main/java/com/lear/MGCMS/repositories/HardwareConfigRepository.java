package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.HardwareConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HardwareConfigRepository extends JpaRepository<HardwareConfig, Long>, JpaSpecificationExecutor<HardwareConfig> {

    @Query("SELECT h FROM HardwareConfig h WHERE h.machine = :machine")
    List<HardwareConfig> findByMachine(@Param("machine") String machine);

    @Query("SELECT h FROM HardwareConfig h WHERE h.type = :type")
    List<HardwareConfig> findByType(@Param("type") String type);

    @Query("SELECT h FROM HardwareConfig h WHERE h.machine = :machine AND h.type = :type")
    List<HardwareConfig> findByMachineAndType(@Param("machine") String machine, @Param("type") String type);
}
