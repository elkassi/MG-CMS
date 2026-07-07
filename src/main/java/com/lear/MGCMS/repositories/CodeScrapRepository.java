package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CodeScrap;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CodeScrapRepository extends JpaRepository<CodeScrap, String>, JpaSpecificationExecutor<CodeScrap>  {

    @Query("from CodeScrap where code like 'CNC%'")
    List<CodeScrap> findAllCNC();

}
