package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CodeDefaut;
import com.lear.MGCMS.domain.ScanRouleau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScanRouleauRepository   extends JpaRepository<ScanRouleau, String>, JpaSpecificationExecutor<ScanRouleau> {

    @Query("select r from ScanRouleau r where r.emplacement in :locations")
    List<ScanRouleau> findByLocations(List<String> locations);

    /**
     * Lightweight projection for workbench cache hot path.
     * Avoids full entity instantiation overhead.
     * Columns: 0=serialId, 1=reftissu, 2=quantite, 3=emplacement, 4=lot, 5=metrage
     */
    @Query("SELECT r.serialId, r.reftissu, r.quantite, r.emplacement, r.lot, r.metrage FROM ScanRouleau r")
    List<Object[]> findAllLight();

    /**
     * Same lightweight projection as {@link #findAllLight()}, but only rolls that are
     * actually racked ({@code emplacement} not null) — the floor-state poll discards
     * everything else anyway, so filtering in SQL avoids a full-table scan every 60s.
     * Columns: 0=serialId, 1=reftissu, 2=quantite, 3=emplacement, 4=lot, 5=metrage
     */
    @Query("SELECT r.serialId, r.reftissu, r.quantite, r.emplacement, r.lot, r.metrage FROM ScanRouleau r WHERE r.emplacement IS NOT NULL")
    List<Object[]> findRackLight();

//    @Query("select metrage from Rouleau where reftissu = :reftissu and emplacement like :prefix and date >= :date")
//    List<Double> getMetrage(String reftissu, String prefix, LocalDateTime date);


}
