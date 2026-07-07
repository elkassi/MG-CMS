package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingSpeed;

public interface CuttingSpeedRepository extends JpaRepository<CuttingSpeed, String>, JpaSpecificationExecutor<CuttingSpeed> {

	CuttingSpeed findByConfig(String config);

}
