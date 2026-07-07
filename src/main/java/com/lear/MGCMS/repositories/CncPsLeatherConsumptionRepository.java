package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CncPsLeatherConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CncPsLeatherConsumptionRepository extends JpaRepository<CncPsLeatherConsumption, Long> {

    List<CncPsLeatherConsumption> findBySessionId(Long sessionId);

    List<CncPsLeatherConsumption> findByLeatherPartNumber(String leatherPartNumber);

    List<CncPsLeatherConsumption> findBySerialOrderByCreatedAtDesc(String serial);
}
