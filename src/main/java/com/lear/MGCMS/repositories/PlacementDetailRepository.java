package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.PlacementDetail;
import com.lear.MGCMS.domain.PlacementDetailId;
import com.lear.MGCMS.payload.EmpStat;

public interface PlacementDetailRepository extends JpaRepository<PlacementDetail, PlacementDetailId>, JpaSpecificationExecutor<PlacementDetail> {
	
	@Query(value = "SELECT new com.lear.MGCMS.payload.EmpStat(placement,folder ,  pattern, nomMedele, idPaquet, count(*))  " + 
			"  FROM PlacementDetail " + 
			"  Group by folder , placement, pattern, nomMedele, idPaquet " + 
			"  HAVING COUNT(*) >= 2 " + 
			"  order by pattern , idPaquet")
	List<EmpStat> findSats();
	


}
