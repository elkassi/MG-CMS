package com.lear.MGCMS.repositories.CuttingRequest.data;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestPartNumberData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestBoxData;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CuttingRequestBoxDataRepository extends JpaRepository<CuttingRequestBoxData, String>, JpaSpecificationExecutor<CuttingRequestBoxData> {

    @Query("select sequence from CuttingRequestBoxData where wo = :idDemande group by sequence")
    List<String> findByWo(String idDemande);

    CuttingRequestBoxData findFirstByWo(String wo);

    @Query("from CuttingRequestBoxData where sequence = :sequence")
    List<CuttingRequestBoxData> findBySequence(String sequence);

    @Query("from CuttingRequestBoxData where sequence in :sequences")
    List<CuttingRequestBoxData> findBySequences(List<String> sequences);
}
