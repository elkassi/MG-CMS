package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.PartNumberMaterialConfigData;
import com.lear.MGCMS.domain.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PointageRepository extends JpaRepository<Pointage, Long>, JpaSpecificationExecutor<Pointage> {


    Pointage findFirstByDateBetweenAndPosteAndTypeOrderByDateDesc(LocalDateTime startDate, LocalDateTime endDate, String poste, String type);

    @Query("from Pointage p where p.date >= :startDate and p.date <= :endDate and p.poste = :poste and p.type = :type and (:departement is null or p.departement = :departement) order by p.date desc")
    List<Pointage> findByFilter(LocalDateTime startDate, LocalDateTime endDate, String poste, String type, String departement);
}
