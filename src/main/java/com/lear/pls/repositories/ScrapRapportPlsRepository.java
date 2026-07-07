package com.lear.pls.repositories;

import com.lear.pls.domain.ScrapRapport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScrapRapportPlsRepository extends JpaRepository<ScrapRapport, Long>, JpaSpecificationExecutor<ScrapRapport> {
}
