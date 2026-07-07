package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.Placement;
import com.lear.MGCMS.domain.PlacementId;

public interface PlacementRepository extends JpaRepository<Placement, PlacementId>, JpaSpecificationExecutor<Placement> {

	@Query("select p from Placement p where p.placement = :placement and p.folder = :folder")
	Placement findByfilter(String placement, String folder);


}
