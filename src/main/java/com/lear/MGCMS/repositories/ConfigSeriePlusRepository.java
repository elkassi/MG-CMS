package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ConfigSeriePlus;
import com.lear.MGCMS.domain.ProductionTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConfigSeriePlusRepository extends JpaRepository<ConfigSeriePlus, Long>, JpaSpecificationExecutor<ConfigSeriePlus> {

    @Query("SELECT t from ConfigSeriePlus t where t.id = :id")
    ConfigSeriePlus findConfigSeriePlusById(Long id);


    @Query("SELECT t from ConfigSeriePlus t where t.partNumberMaterial in (:arr)")
    List<ConfigSeriePlus> findByPartNumberMaterialIn(List<String> arr);
}
