package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CuttingRequest.data.CuttingRequestData;
import com.lear.MGCMS.domain.StockStatusReport;
import com.lear.MGCMS.services.StockStatusReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stockStatusReport")
public class StockStatusReportController {

    @Autowired
    private StockStatusReportService service;

    @GetMapping("/all")
    public Page<StockStatusReport> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page,size,sortBy);
    }


    @GetMapping("/currentStock")
    public List<StockStatusReport> getCurrentStock(
            //list of reftissus
            @RequestParam(value = "refTissus", required = true) List<String> refTissus
    ) {
        return service.getCurrentStock(refTissus);
    }

    @GetMapping("/stockQLaize")
    public List<StockStatusReport> getStockQLaize() {
        return service.getStockQLaize();
    }



}
