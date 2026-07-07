package com.lear.MGCMS.repositories.CuttingRequest.data;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestPartNumberId;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestPartNumberData;
import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestSerieData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CuttingRequestPartNumberDataRepository extends JpaRepository<CuttingRequestPartNumberData, CuttingRequestPartNumberId>, JpaSpecificationExecutor<CuttingRequestPartNumberData> {
    @Query("SELECT c FROM CuttingRequestPartNumberData c WHERE c.cuttingRequest = :sequence")
    List<CuttingRequestPartNumberData> findBySequence(String sequence);

    @Query("SELECT c FROM CuttingRequestPartNumberData c WHERE c.cuttingRequest IN (:sequences)")
    List<CuttingRequestPartNumberData> findBySequences(List<String> sequences);

    @Query("SELECT c.cuttingRequest FROM CuttingRequestPartNumberData c WHERE c.wo IN (:wos) group by c.cuttingRequest")
    List<String> getSequencesByWos(List<String> wos);
    @Query("SELECT c.wo FROM CuttingRequestPartNumberData c WHERE c.cuttingRequest = :sequence")
    List<String> findWoBySequence(String sequence);
}
