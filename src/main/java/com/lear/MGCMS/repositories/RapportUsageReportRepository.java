package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.RapportUsageReport;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface RapportUsageReportRepository extends CrudRepository<RapportUsageReport, String>, JpaSpecificationExecutor<RapportUsageReport> {
}
