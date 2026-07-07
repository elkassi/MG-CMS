package com.lear.MGCMS.repositories.CuttingRequest;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CuttingRequestSerieRouleauInfoRepository extends CrudRepository<CuttingRequestSerieRouleauInfo, Long> {

	@Query("select rl from CuttingRequestSerieRouleauInfo rl where rl.idRouleau = :idRouleau order by metrage")
	public List<CuttingRequestSerieRouleauInfo> findByIdRouleau(String idRouleau);
	@Query("select rl.idRouleau from CuttingRequestSerieRouleauInfo rl where rl.idRouleau in (:arrStr) and rl.confirmRetour = 1")
    List<String> getArrLabelIdWithRetour(List<String> arrStr);
}
