package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.ReftissuProperty;
import com.lear.MGCMS.domain.ReftissuPropertyId;
import com.lear.MGCMS.domain.StockStatusReport;
import com.lear.MGCMS.domain.StockStatusReportId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface StockStatusReportRepository extends PagingAndSortingRepository<StockStatusReport, StockStatusReportId>, JpaSpecificationExecutor<StockStatusReport> {

    @org.springframework.data.jpa.repository.Query("from StockStatusReport where ref in (:refs)")
    java.util.List<StockStatusReport> findByRefs(java.util.List<String> refs);

    @org.springframework.data.jpa.repository.Query("from StockStatusReport where ref = :ref")
    java.util.List<StockStatusReport> findByRef(String ref);

    Page<StockStatusReport> findByRefContainingIgnoreCaseAndItemNumberContainingIgnoreCaseAndIsDeletedFalse(String ref, String itemNumber, Pageable pageable);
    Page<StockStatusReport> findByRefContainingIgnoreCaseAndIsDeletedFalse(String ref, Pageable pageable);
    Page<StockStatusReport> findByItemNumberContainingIgnoreCaseAndIsDeletedFalse(String itemNumber, Pageable pageable);
    Page<StockStatusReport> findByIsDeletedFalse(Pageable pageable);
}
