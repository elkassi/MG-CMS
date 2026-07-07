package com.lear.MGCMS.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lear.MGCMS.domain.PlacementFolder;
import com.lear.MGCMS.domain.ReftissuPrix;

public interface ReftissuPrixRepository extends JpaRepository<ReftissuPrix, Long>, JpaSpecificationExecutor<ReftissuPrix> {

	@Query("from ReftissuPrix where reftissu in (:arr)")
	List<ReftissuPrix> findList(List<String> arr);

}
