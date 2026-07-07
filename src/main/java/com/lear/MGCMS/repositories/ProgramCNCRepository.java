package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ProgramCNC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramCNCRepository extends JpaRepository<ProgramCNC, Long>, JpaSpecificationExecutor<ProgramCNC> {

    List<ProgramCNC> findByPartNumber(String partNumber);
}
