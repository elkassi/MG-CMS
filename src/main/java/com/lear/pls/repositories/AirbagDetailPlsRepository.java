package com.lear.pls.repositories;

import com.lear.pls.domain.AirbagDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AirbagDetailPlsRepository extends JpaRepository<AirbagDetail, Long>, JpaSpecificationExecutor<AirbagDetail> {
}
