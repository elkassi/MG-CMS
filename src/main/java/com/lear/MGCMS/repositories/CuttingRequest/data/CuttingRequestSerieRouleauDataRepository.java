package com.lear.MGCMS.repositories.CuttingRequest.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieRouleauData;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CuttingRequestSerieRouleauDataRepository extends JpaRepository<CuttingRequestSerieRouleauData, Long>, JpaSpecificationExecutor<CuttingRequestSerieRouleauData>  {

    @Query("from CuttingRequestSerieRouleauData where serie is null and confirmRetour = true")
    List<CuttingRequestSerieRouleauData> findEmptySerie();
    @Query("from CuttingRequestSerieRouleauData where defautCode is not null and deblockedDate is null order by createdAt desc")
    List<CuttingRequestSerieRouleauData> findDefaut();

    @Query("from CuttingRequestSerieRouleauData where excess < :max and createdAt between :date1 and :date2 order by createdAt desc")
    List<CuttingRequestSerieRouleauData> findExcess(Double max , LocalDateTime date1, LocalDateTime date2);

    @Query("from CuttingRequestSerieRouleauData where serie in (:listSeries)")
    List<CuttingRequestSerieRouleauData> findBySeries(List<String> listSeries);

    @Query("from CuttingRequestSerieRouleauData where idRouleau in (:listRouleaux)")
    List<CuttingRequestSerieRouleauData> findByIdRouleaux(List<String> listRouleaux);
}
