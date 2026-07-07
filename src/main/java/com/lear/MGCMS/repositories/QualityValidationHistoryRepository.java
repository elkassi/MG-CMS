package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.MachineDxfRapport;
import com.lear.MGCMS.domain.MachineDxfRapportId;
import com.lear.MGCMS.domain.QualityValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QualityValidationHistoryRepository extends JpaRepository<QualityValidationHistory, String>, JpaSpecificationExecutor<QualityValidationHistory> {
}
