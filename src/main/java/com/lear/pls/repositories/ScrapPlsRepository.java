package com.lear.pls.repositories;

import com.lear.pls.domain.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScrapPlsRepository extends JpaRepository<Scrap, String>, JpaSpecificationExecutor<Scrap> {

	Scrap findScrapById(String id);
}
