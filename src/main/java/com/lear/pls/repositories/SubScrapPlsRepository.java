package com.lear.pls.repositories;

import com.lear.pls.domain.SubScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubScrapPlsRepository extends JpaRepository<SubScrap, Long>, JpaSpecificationExecutor<SubScrap> {

	SubScrap getSubScrapById(Long id);
}
