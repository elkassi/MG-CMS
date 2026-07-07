package com.lear.pls.repositories;

import com.lear.pls.domain.CauseScrap;
import com.lear.pls.domain.CauseScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface CauseScrapPlsRepository extends JpaRepository<CauseScrap, Long>, JpaSpecificationExecutor<CauseScrap> {

	CauseScrap getCauseScrapById(Long id);
}
