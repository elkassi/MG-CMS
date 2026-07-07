package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.PlacementFolder;

public interface PlacementFolderRepository extends JpaRepository<PlacementFolder, Long>, JpaSpecificationExecutor<PlacementFolder> {

}
