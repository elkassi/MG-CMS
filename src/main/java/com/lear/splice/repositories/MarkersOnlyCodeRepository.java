package com.lear.splice.repositories;

import com.lear.splice.domain.MarkersOnlyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface MarkersOnlyCodeRepository extends JpaRepository<MarkersOnlyCode, Long>, JpaSpecificationExecutor<MarkersOnlyCode> {
    MarkersOnlyCode findFirstByOrderCode(String orderCode);

    @Modifying
    @Transactional
    @Query("delete from MarkersOnlyCode where orderCode = :serie")
    void deleteByCode(String serie);
}
