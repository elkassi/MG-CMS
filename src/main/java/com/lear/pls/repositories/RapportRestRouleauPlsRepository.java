package com.lear.pls.repositories;

import com.lear.pls.domain.RapportRestRouleau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RapportRestRouleauPlsRepository extends JpaRepository<RapportRestRouleau, Long>, JpaSpecificationExecutor<RapportRestRouleau> {
}
