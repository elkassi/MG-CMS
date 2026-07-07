package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.MachineDxfRapport;
import com.lear.MGCMS.domain.MachineDxfRapportId;
import com.lear.MGCMS.domain.QualityReftissuBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QualityReftissuBlockRepository extends JpaRepository<QualityReftissuBlock, String>, JpaSpecificationExecutor<QualityReftissuBlock> {
    QualityReftissuBlock findByReftissu(String reftissu);
}
