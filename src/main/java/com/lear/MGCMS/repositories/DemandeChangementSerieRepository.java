package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CoupePerformanceId;
import com.lear.MGCMS.domain.DemandeChangementSerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DemandeChangementSerieRepository extends JpaRepository<DemandeChangementSerie, CoupePerformanceId>, JpaSpecificationExecutor<DemandeChangementSerie> {

    @Query("SELECT t from DemandeChangementSerie t where t.id = :id")
    DemandeChangementSerie findByObjId(String id);


    @Query("SELECT max(t.ind) from DemandeChangementSerie t where FUNCTION('YEAR', t.dateCreation) = :year")
    Integer getMaxInd(int year);

    @Query("from DemandeChangementSerie t where t.serie in (:series) and t.active = true")
    List<DemandeChangementSerie> findBySerieIn(List<String> series);
}
