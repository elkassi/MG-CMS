package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.Intervention;
import com.lear.MGCMS.domain.ProductionTable;
import com.lear.MGCMS.domain.QualityNotice;
import com.lear.MGCMS.payload.StatsInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QualityNoticeRepository extends JpaRepository<QualityNotice, Long>, JpaSpecificationExecutor<QualityNotice> {

    @Query("SELECT t from QualityNotice t where t.id = :id")
    QualityNotice findByObjId(String id);


    @Query("SELECT max(t.ind) from QualityNotice t where t.site = :site")
    Integer getMaxIndBySite(String site);

    @Query("select new com.lear.MGCMS.payload.StatsInfo(d.site, count(d)) from QualityNotice d "
            + "where d.active = 1 and d.dateTraitement is null "
            + "GROUP BY d.site")
    List<StatsInfo> getStatsBySites();

    @Query("select new com.lear.MGCMS.payload.StatsInfo(d.typeDefaut, count(d)) from QualityNotice d "
            + "where d.active = 1 and d.dateTraitement is null "
            + "GROUP BY d.typeDefaut")
    List<StatsInfo> getStatsByTypeDefaut();
}
