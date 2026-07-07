package com.lear.MGCMS.repositories.CuttingRequest;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauHistory;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface CuttingRequestSerieRouleauHistoryRepository extends CrudRepository<CuttingRequestSerieRouleauHistory, Long> , JpaSpecificationExecutor<CuttingRequestSerieRouleauHistory> {
}
