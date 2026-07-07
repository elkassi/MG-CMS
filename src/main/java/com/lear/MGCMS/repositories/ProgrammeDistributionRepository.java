package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ProgrammeDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgrammeDistributionRepository extends JpaRepository<ProgrammeDistribution, Long>, JpaSpecificationExecutor<ProgrammeDistribution> {

    ProgrammeDistribution findFirstByMachineIdAndProgrammeNumber(Long machineId, Integer programmeNumber);

    List<ProgrammeDistribution> findByProgrammeNumber(Integer programmeNumber);
}
