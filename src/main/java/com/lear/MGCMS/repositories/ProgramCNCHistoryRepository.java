package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ProgramCNCHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramCNCHistoryRepository extends JpaRepository<ProgramCNCHistory, Long> {
}
