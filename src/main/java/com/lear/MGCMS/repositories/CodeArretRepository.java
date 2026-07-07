package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CodeArret;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestBoxData;

public interface CodeArretRepository extends JpaRepository<CodeArret, String>, JpaSpecificationExecutor<CodeArret>  {

}
