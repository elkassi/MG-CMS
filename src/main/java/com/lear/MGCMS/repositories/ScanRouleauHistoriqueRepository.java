package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ScanRouleauHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScanRouleauHistoriqueRepository extends JpaRepository<ScanRouleauHistorique, Integer>, JpaSpecificationExecutor<ScanRouleauHistorique> {

    @Query("select h from ScanRouleauHistorique h where h.serialId = :serialId order by h.date desc")
    List<ScanRouleauHistorique> findBySerialIdOrderByDateDesc(@Param("serialId") String serialId);

    @Query("select h from ScanRouleauHistorique h where h.date >= :startDate and h.date <= :endDate order by h.date desc")
    List<ScanRouleauHistorique> findByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("select h from ScanRouleauHistorique h where h.serialId = :serialId and h.date >= :startDate and h.date <= :endDate order by h.date desc")
    List<ScanRouleauHistorique> findBySerialIdAndDateBetween(@Param("serialId") String serialId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
