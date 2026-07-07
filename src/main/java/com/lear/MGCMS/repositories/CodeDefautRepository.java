package com.lear.MGCMS.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CodeDefaut;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CodeDefautRepository extends JpaRepository<CodeDefaut, String>, JpaSpecificationExecutor<CodeDefaut>  {

    @Query("from CodeDefaut where code like 'C%' and active = 1")
    List<CodeDefaut> findAllC();

    @Query("from CodeDefaut where code like 'CNC%'")
    List<CodeDefaut> findAllCNC();

}
